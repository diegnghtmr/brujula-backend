package co.edu.uniquindio.brujula.curriculum.application.service;

import co.edu.uniquindio.brujula.audit.application.dto.RecordAuditEventCommand;
import co.edu.uniquindio.brujula.audit.application.port.in.AuditRecorder;
import co.edu.uniquindio.brujula.curriculum.application.dto.ArchiveCourseCommand;
import co.edu.uniquindio.brujula.curriculum.application.dto.CourseView;
import co.edu.uniquindio.brujula.curriculum.application.port.in.ArchiveCourseUseCase;
import co.edu.uniquindio.brujula.curriculum.application.port.out.SaveCoursePort;
import co.edu.uniquindio.brujula.curriculum.domain.Course;
import co.edu.uniquindio.brujula.curriculum.domain.CourseId;
import co.edu.uniquindio.brujula.curriculum.domain.CourseNotFoundException;

/**
 * Use case: archive a Course (soft-delete) with optimistic locking (STAFF/ADMIN, NOT idempotent).
 *
 * <p>Follows the UpdateCourseService pattern:
 * <ol>
 *   <li>Load current course (throws {@link CourseNotFoundException} if absent)
 *   <li>Apply domain archive via {@link Course#archive}
 *   <li>Reconstitute with {@code expectedVersion} for optimistic lock enforcement
 *   <li>Persist — may throw {@code OptimisticLockingFailureException} (→ 409)
 *   <li>Record audit event
 * </ol>
 *
 * <p>Framework-free — wired by {@code CurriculumConfiguration}.
 * Satisfies: CUR-002, SC-009-D.
 */
public class ArchiveCourseService implements ArchiveCourseUseCase {

  private final SaveCoursePort saveCoursePort;
  private final AuditRecorder auditRecorder;

  public ArchiveCourseService(SaveCoursePort saveCoursePort, AuditRecorder auditRecorder) {
    this.saveCoursePort = saveCoursePort;
    this.auditRecorder = auditRecorder;
  }

  @Override
  public CourseView archiveCourse(ArchiveCourseCommand command) {
    CourseId id = new CourseId(command.courseId());

    // 1. Load current course
    Course existing = saveCoursePort.findCurrentById(id)
        .orElseThrow(() -> new CourseNotFoundException(id));

    // 2. Apply archive (throws IllegalStateException if already ARCHIVED)
    Course archived = existing.archive();

    // 3. Reconstitute with expectedVersion for optimistic lock enforcement
    Course withExpectedVersion = Course.reconstitute(
        archived.id(),
        archived.planId(),
        archived.programId(),
        archived.officialCode(),
        archived.name(),
        archived.type(),
        archived.credits(),
        archived.curricularWeeklyHours(),
        archived.recommendedSemesters(),
        archived.status(),
        command.expectedVersion());

    // 4. Persist — may throw OptimisticLockingFailureException (→ 409)
    Course saved = saveCoursePort.save(withExpectedVersion);

    // 5. Record audit event (version bump already happened in save + flush)
    auditRecorder.record(new RecordAuditEventCommand(
        command.actorId(),
        "STAFF",
        "COURSE_ARCHIVED",
        "course",
        saved.id().value(),
        null,
        null,
        null));

    return CreateCourseService.toView(saved);
  }
}
