package co.edu.uniquindio.brujula.curriculum.application.service;

import co.edu.uniquindio.brujula.audit.application.dto.RecordAuditEventCommand;
import co.edu.uniquindio.brujula.audit.application.port.in.AuditRecorder;
import co.edu.uniquindio.brujula.curriculum.application.dto.CourseView;
import co.edu.uniquindio.brujula.curriculum.application.dto.UpdateCourseCommand;
import co.edu.uniquindio.brujula.curriculum.application.port.in.UpdateCourseUseCase;
import co.edu.uniquindio.brujula.curriculum.application.port.out.SaveCoursePort;
import co.edu.uniquindio.brujula.curriculum.domain.Course;
import co.edu.uniquindio.brujula.curriculum.domain.CourseId;
import co.edu.uniquindio.brujula.curriculum.domain.CourseNotFoundException;
import co.edu.uniquindio.brujula.curriculum.domain.CourseType;
import co.edu.uniquindio.brujula.curriculum.domain.RecommendedSemester;

import java.util.Collections;
import java.util.List;

/**
 * Use case: update a Course's mutable fields with optimistic locking (STAFF/ADMIN, NOT idempotent).
 *
 * <p>Follows the UpdateOfferingService pattern:
 * <ol>
 *   <li>Load current course (throws {@link CourseNotFoundException} if absent)
 *   <li>Apply domain updates via {@link Course#update}
 *   <li>Reconstitute with {@code expectedVersion} so the JPA adapter sets it on the entity;
 *       Hibernate detects mismatch at flush → {@code OptimisticLockingFailureException} (→ 409)
 *   <li>Persist — adapter returns post-flush managed entity with version incremented
 *   <li>Record audit event
 * </ol>
 *
 * <p>Framework-free — wired by {@code CurriculumConfiguration}.
 * Satisfies: CUR-002, SC-009-C.
 */
public class UpdateCourseService implements UpdateCourseUseCase {

  private final SaveCoursePort saveCoursePort;
  private final AuditRecorder auditRecorder;

  public UpdateCourseService(SaveCoursePort saveCoursePort, AuditRecorder auditRecorder) {
    this.saveCoursePort = saveCoursePort;
    this.auditRecorder = auditRecorder;
  }

  @Override
  public CourseView updateCourse(UpdateCourseCommand command) {
    CourseId id = new CourseId(command.courseId());

    // 1. Load current course
    Course existing = saveCoursePort.findCurrentById(id)
        .orElseThrow(() -> new CourseNotFoundException(id));

    // 2. Apply domain update (returns new immutable instance)
    List<RecommendedSemester> semesters = command.recommendedSemesters() != null
        ? command.recommendedSemesters().stream()
            .map(rs -> new RecommendedSemester(rs.journeyCode(), rs.semester()))
            .toList()
        : Collections.emptyList();

    Course updated = existing.update(
        command.name(),
        CourseType.valueOf(command.type()),
        command.credits(),
        command.curricularWeeklyHours(),
        semesters);

    // 3. Reconstitute with expectedVersion for optimistic lock enforcement
    Course withExpectedVersion = Course.reconstitute(
        updated.id(),
        updated.planId(),
        updated.programId(),
        updated.officialCode(),
        updated.name(),
        updated.type(),
        updated.credits(),
        updated.curricularWeeklyHours(),
        updated.recommendedSemesters(),
        updated.status(),
        command.expectedVersion());

    // 4. Persist — may throw OptimisticLockingFailureException (→ 409)
    Course saved = saveCoursePort.save(withExpectedVersion);

    // 5. Record audit event (version bump already happened in save + flush)
    auditRecorder.record(new RecordAuditEventCommand(
        command.actorId(),
        "STAFF",
        "COURSE_UPDATED",
        "course",
        saved.id().value(),
        null,
        null,
        null));

    return CreateCourseService.toView(saved);
  }
}
