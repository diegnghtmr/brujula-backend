package co.edu.uniquindio.brujula.progress.application.service;

import co.edu.uniquindio.brujula.audit.application.dto.RecordAuditEventCommand;
import co.edu.uniquindio.brujula.audit.application.port.in.AuditRecorder;
import co.edu.uniquindio.brujula.progress.application.port.in.DeleteCourseAttemptUseCase;
import co.edu.uniquindio.brujula.progress.application.port.out.LoadCourseAttemptPort;
import co.edu.uniquindio.brujula.progress.application.port.out.LoadSubscriptionPort;
import co.edu.uniquindio.brujula.progress.application.port.out.SaveCourseAttemptPort;
import co.edu.uniquindio.brujula.progress.domain.AttemptNotFoundException;
import co.edu.uniquindio.brujula.progress.domain.CourseAttempt;
import co.edu.uniquindio.brujula.progress.domain.Subscription;
import java.util.UUID;

/**
 * Use case: delete a course attempt (PRG-4).
 *
 * <p>Enforces BOLA ownership and records an audit event (COURSE_ATTEMPT_DELETED).
 *
 * <p>Framework-free — wired by {@code ProgressConfiguration}. Satisfies: PRG-4, CROSS-4.
 */
public class DeleteCourseAttemptService implements DeleteCourseAttemptUseCase {

  private final LoadCourseAttemptPort loadCourseAttemptPort;
  private final LoadSubscriptionPort loadSubscriptionPort;
  private final SaveCourseAttemptPort saveCourseAttemptPort;
  private final AuditRecorder auditRecorder;

  public DeleteCourseAttemptService(
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
  public void deleteCourseAttempt(UUID attemptId, UUID actorId) {
    // Load attempt
    CourseAttempt attempt =
        loadCourseAttemptPort
            .loadById(attemptId)
            .orElseThrow(() -> new AttemptNotFoundException(attemptId));

    // BOLA ownership guard
    Subscription sub = loadSubscriptionPort.loadById(attempt.getSubscriptionId().value()).orElse(null);
    if (sub == null || !sub.getUserId().equals(actorId)) {
      throw new AttemptNotFoundException(attemptId);
    }

    // Audit BEFORE delete (CROSS-4: COURSE_ATTEMPT_DELETED)
    auditRecorder.record(
        new RecordAuditEventCommand(
            actorId,
            "STUDENT",
            "COURSE_ATTEMPT_DELETED",
            "courseAttempt",
            attemptId,
            null,
            null,
            null));

    // Delete
    saveCourseAttemptPort.delete(attempt);
  }
}
