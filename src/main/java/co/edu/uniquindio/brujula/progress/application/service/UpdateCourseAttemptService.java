package co.edu.uniquindio.brujula.progress.application.service;

import co.edu.uniquindio.brujula.audit.application.dto.RecordAuditEventCommand;
import co.edu.uniquindio.brujula.audit.application.port.in.AuditRecorder;
import co.edu.uniquindio.brujula.progress.application.dto.CourseAttemptView;
import co.edu.uniquindio.brujula.progress.application.dto.UpdateCourseAttemptCommand;
import co.edu.uniquindio.brujula.progress.application.port.in.UpdateCourseAttemptUseCase;
import co.edu.uniquindio.brujula.progress.application.port.out.LoadCourseAttemptPort;
import co.edu.uniquindio.brujula.progress.application.port.out.LoadSubscriptionPort;
import co.edu.uniquindio.brujula.progress.application.port.out.SaveCourseAttemptPort;
import co.edu.uniquindio.brujula.progress.domain.AttemptNotFoundException;
import co.edu.uniquindio.brujula.progress.domain.AttemptStatus;
import co.edu.uniquindio.brujula.progress.domain.CourseAttempt;
import co.edu.uniquindio.brujula.progress.domain.Grade;
import co.edu.uniquindio.brujula.progress.domain.Subscription;
import java.math.BigDecimal;

/**
 * Use case: update an existing course attempt.
 *
 * <p>Enforces BOLA ownership, grade-status coherence (delegated to {@link CourseAttempt}), and
 * optimistic locking (JPA {@code @Version} → {@code OptimisticLockingFailureException} → 409).
 *
 * <p>Framework-free — wired by {@code ProgressConfiguration}.
 */
public class UpdateCourseAttemptService implements UpdateCourseAttemptUseCase {

  private final LoadCourseAttemptPort loadCourseAttemptPort;
  private final LoadSubscriptionPort loadSubscriptionPort;
  private final SaveCourseAttemptPort saveCourseAttemptPort;
  private final AuditRecorder auditRecorder;

  public UpdateCourseAttemptService(
      LoadCourseAttemptPort loadCourseAttemptPort,
      LoadSubscriptionPort loadSubscriptionPort,
      SaveCourseAttemptPort saveCourseAttemptPort,
      AuditRecorder auditRecorder) {
    this.loadCourseAttemptPort = loadCourseAttemptPort;
    this.loadSubscriptionPort = loadSubscriptionPort;
    this.saveCourseAttemptPort = saveCourseAttemptPort;
    this.auditRecorder = auditRecorder;
  }

  @Override
  public CourseAttemptView updateCourseAttempt(UpdateCourseAttemptCommand command) {
    // Load attempt
    CourseAttempt attempt =
        loadCourseAttemptPort
            .loadById(command.attemptId())
            .orElseThrow(() -> new AttemptNotFoundException(command.attemptId()));

    // BOLA ownership guard
    Subscription sub =
        loadSubscriptionPort.loadById(attempt.getSubscriptionId().value()).orElse(null);
    if (sub == null || !sub.getUserId().equals(command.actorId())) {
      throw new AttemptNotFoundException(command.attemptId());
    }

    // Apply update (domain validates grade-status coherence)
    AttemptStatus newStatus = AttemptStatus.valueOf(command.statusName());
    Grade newGrade = command.finalGrade() != null ? new Grade(command.finalGrade()) : null;
    attempt.update(newStatus, newGrade, command.periodId(), command.journeyCode());

    // Persist (JPA @Version handles optimistic locking)
    CourseAttempt saved = saveCourseAttemptPort.save(attempt);

    // Audit: COURSE_ATTEMPT_UPDATED
    auditRecorder.record(
        new RecordAuditEventCommand(
            command.actorId(),
            "STUDENT",
            "COURSE_ATTEMPT_UPDATED",
            "courseAttempt",
            command.attemptId(),
            null,
            null,
            null));

    return toView(saved);
  }

  private CourseAttemptView toView(CourseAttempt attempt) {
    BigDecimal gradeValue = attempt.getGrade() != null ? attempt.getGrade().value() : null;
    return new CourseAttemptView(
        attempt.getId().value(),
        attempt.getSubscriptionId().value(),
        attempt.getCourseId(),
        "N/A", // courseName not yet resolved
        attempt.getPeriodId(),
        null, // periodName not yet resolved
        attempt.getJourneyCode(),
        attempt.getStatus().name(), // plain String — no domain type in application DTO
        gradeValue,
        attempt.getRegisteredAt(),
        attempt.getVersion());
  }
}
