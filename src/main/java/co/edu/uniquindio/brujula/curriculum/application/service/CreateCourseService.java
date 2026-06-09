package co.edu.uniquindio.brujula.curriculum.application.service;

import co.edu.uniquindio.brujula.audit.application.dto.RecordAuditEventCommand;
import co.edu.uniquindio.brujula.audit.application.port.in.AuditRecorder;
import co.edu.uniquindio.brujula.curriculum.application.dto.CourseView;
import co.edu.uniquindio.brujula.curriculum.application.dto.CreateCourseCommand;
import co.edu.uniquindio.brujula.curriculum.application.port.in.CreateCourseUseCase;
import co.edu.uniquindio.brujula.curriculum.application.port.out.SaveCoursePort;
import co.edu.uniquindio.brujula.curriculum.domain.Course;
import co.edu.uniquindio.brujula.curriculum.domain.CourseId;
import co.edu.uniquindio.brujula.curriculum.domain.CourseType;
import co.edu.uniquindio.brujula.curriculum.domain.RecommendedSemester;
import co.edu.uniquindio.brujula.sharedkernel.application.exception.IdempotencyKeyConflictException;
import co.edu.uniquindio.brujula.sharedkernel.application.port.out.IdempotencyStorePort;
import co.edu.uniquindio.brujula.sharedkernel.application.port.out.JsonCodecPort;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Use case: create a new Course (idempotent via Idempotency-Key, STAFF/ADMIN).
 *
 * <p>Idempotency rules (mirrors CreateOfferingService):
 * <ul>
 *   <li>Same key + same hash → return stored result (replay)
 *   <li>Same key + different hash → throw {@link IdempotencyKeyConflictException} (409)
 *   <li>New key → execute, persist idempotency record, record audit event
 * </ul>
 *
 * <p>Framework-free — wired by {@code CurriculumConfiguration}.
 * Satisfies: CUR-001, IMP-009, SC-009-A.
 */
public class CreateCourseService implements CreateCourseUseCase {

  public static final String OPERATION = "CREATE_COURSE";

  private final SaveCoursePort saveCoursePort;
  private final IdempotencyStorePort idempotencyStorePort;
  private final JsonCodecPort jsonCodecPort;
  private final AuditRecorder auditRecorder;

  public CreateCourseService(
      SaveCoursePort saveCoursePort,
      IdempotencyStorePort idempotencyStorePort,
      JsonCodecPort jsonCodecPort,
      AuditRecorder auditRecorder) {
    this.saveCoursePort = saveCoursePort;
    this.idempotencyStorePort = idempotencyStorePort;
    this.jsonCodecPort = jsonCodecPort;
    this.auditRecorder = auditRecorder;
  }

  @Override
  public CourseView createCourse(CreateCourseCommand command) {
    // 1. Check idempotency store first
    Optional<IdempotencyStorePort.StoredRecord> existing =
        idempotencyStorePort.find(command.idempotencyKey(), OPERATION);
    if (existing.isPresent()) {
      IdempotencyStorePort.StoredRecord stored = existing.get();
      if (!stored.requestHash().equals(command.requestHash())) {
        throw new IdempotencyKeyConflictException(command.idempotencyKey(), OPERATION);
      }
      return jsonCodecPort.fromJson(stored.responseBodyJson(), CourseView.class);
    }

    // 2. Map recommended semesters from command (empty list if null)
    List<RecommendedSemester> semesters = command.recommendedSemesters() != null
        ? command.recommendedSemesters().stream()
            .map(rs -> new RecommendedSemester(rs.journeyCode(), rs.semester()))
            .toList()
        : Collections.emptyList();

    // 3. Create domain aggregate (starts ACTIVE, version 0)
    Course course = Course.create(
        CourseId.generate(),
        command.planId(),
        command.programId(),
        command.name(),
        CourseType.valueOf(command.type()),
        command.credits(),
        command.curricularWeeklyHours(),
        semesters);

    // 4. Persist
    Course saved = saveCoursePort.save(course);

    // 5. Build view and serialize for idempotency storage
    CourseView view = toView(saved);
    String responseBodyJson = jsonCodecPort.toJson(view);

    // 6. Record idempotency entry
    idempotencyStorePort.save(
        command.idempotencyKey(), OPERATION, command.requestHash(), 201, responseBodyJson);

    // 7. Record audit event
    auditRecorder.record(new RecordAuditEventCommand(
        command.actorId(),
        "STAFF",
        "COURSE_CREATED",
        "course",
        saved.id().value(),
        null,
        null,
        null));

    return view;
  }

  static CourseView toView(Course course) {
    List<CourseView.RecommendedSemesterView> semesterViews = course.recommendedSemesters().stream()
        .map(rs -> new CourseView.RecommendedSemesterView(rs.journeyCode(), rs.semester()))
        .toList();
    return new CourseView(
        course.id().value(),
        course.planId(),
        course.programId(),
        course.officialCode(),
        course.name(),
        course.type().name(),
        course.credits(),
        course.curricularWeeklyHours(),
        semesterViews,
        course.status().name(),
        course.version());
  }
}
