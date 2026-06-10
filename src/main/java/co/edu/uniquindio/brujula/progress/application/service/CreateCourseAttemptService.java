package co.edu.uniquindio.brujula.progress.application.service;

import co.edu.uniquindio.brujula.audit.application.dto.RecordAuditEventCommand;
import co.edu.uniquindio.brujula.audit.application.port.in.AuditRecorder;
import co.edu.uniquindio.brujula.progress.application.dto.CourseAttemptView;
import co.edu.uniquindio.brujula.progress.application.dto.CreateCourseAttemptCommand;
import co.edu.uniquindio.brujula.progress.application.port.in.CreateCourseAttemptUseCase;
import co.edu.uniquindio.brujula.progress.application.port.out.LoadCourseAttemptPort;
import co.edu.uniquindio.brujula.progress.application.port.out.LoadSubscriptionPort;
import co.edu.uniquindio.brujula.progress.application.port.out.SaveCourseAttemptPort;
import co.edu.uniquindio.brujula.progress.domain.AttemptId;
import co.edu.uniquindio.brujula.progress.domain.AttemptStatus;
import co.edu.uniquindio.brujula.progress.domain.CourseAttempt;
import co.edu.uniquindio.brujula.progress.domain.Grade;
import co.edu.uniquindio.brujula.progress.domain.Subscription;
import co.edu.uniquindio.brujula.progress.domain.SubscriptionId;
import co.edu.uniquindio.brujula.progress.domain.SubscriptionNotFoundException;
import co.edu.uniquindio.brujula.sharedkernel.application.exception.IdempotencyKeyConflictException;
import co.edu.uniquindio.brujula.sharedkernel.application.port.out.IdempotencyStorePort;
import co.edu.uniquindio.brujula.sharedkernel.application.port.out.JsonCodecPort;
import java.math.BigDecimal;
import java.util.Optional;

/**
 * Use case: register a new course attempt for the authenticated student.
 *
 * <p>Enforces: BOLA ownership guard (subscription must belong to actorId), grade-status coherence
 * (delegated to {@link CourseAttempt} domain), idempotency via sharedkernel {@link
 * IdempotencyStorePort}, audit via {@link AuditRecorder}.
 *
 * <p>Framework-free — wired by {@code ProgressConfiguration}. Satisfies: PRG-1, CROSS-1, CROSS-4.
 */
public class CreateCourseAttemptService implements CreateCourseAttemptUseCase {

  public static final String OPERATION = "CREATE_COURSE_ATTEMPT";

  private final LoadSubscriptionPort loadSubscriptionPort;
  private final SaveCourseAttemptPort saveCourseAttemptPort;
  private final LoadCourseAttemptPort loadCourseAttemptPort;
  private final IdempotencyStorePort idempotencyStorePort;
  private final JsonCodecPort jsonCodecPort;
  private final AuditRecorder auditRecorder;

  public CreateCourseAttemptService(
      LoadSubscriptionPort loadSubscriptionPort,
      SaveCourseAttemptPort saveCourseAttemptPort,
      LoadCourseAttemptPort loadCourseAttemptPort,
      IdempotencyStorePort idempotencyStorePort,
      JsonCodecPort jsonCodecPort,
      AuditRecorder auditRecorder) {
    this.loadSubscriptionPort = loadSubscriptionPort;
    this.saveCourseAttemptPort = saveCourseAttemptPort;
    this.loadCourseAttemptPort = loadCourseAttemptPort;
    this.idempotencyStorePort = idempotencyStorePort;
    this.jsonCodecPort = jsonCodecPort;
    this.auditRecorder = auditRecorder;
  }

  @Override
  public CourseAttemptView createCourseAttempt(CreateCourseAttemptCommand command) {
    // 1. Idempotency check
    Optional<IdempotencyStorePort.StoredRecord> existing =
        idempotencyStorePort.find(command.idempotencyKey(), OPERATION);
    if (existing.isPresent()) {
      IdempotencyStorePort.StoredRecord stored = existing.get();
      if (!stored.requestHash().equals(command.requestHash())) {
        throw new IdempotencyKeyConflictException(command.idempotencyKey(), OPERATION);
      }
      return jsonCodecPort.fromJson(stored.responseBodyJson(), CourseAttemptView.class);
    }

    // 2. BOLA ownership guard — subscription must belong to actorId
    Subscription subscription =
        loadSubscriptionPort
            .loadById(command.subscriptionId())
            .orElseThrow(() -> new SubscriptionNotFoundException(command.subscriptionId()));

    if (!subscription.getUserId().equals(command.actorId())) {
      // BOLA: return 404 to avoid revealing resource existence (spec CROSS-2, PRG-1-FOREIGN-SUB)
      throw new SubscriptionNotFoundException(command.subscriptionId());
    }

    // 3. Build grade (domain validates coherence)
    Grade grade = command.finalGrade() != null ? new Grade(command.finalGrade()) : null;

    // 4. Register the attempt (domain enforces grade-status coherence)
    AttemptStatus status = AttemptStatus.valueOf(command.statusName());
    AttemptId attemptId = AttemptId.generate();
    CourseAttempt attempt =
        CourseAttempt.register(
            attemptId,
            SubscriptionId.of(command.subscriptionId()),
            command.courseId(),
            status,
            grade,
            command.periodId(),
            command.journeyCode());

    // 5. Persist
    CourseAttempt saved = saveCourseAttemptPort.save(attempt);

    // 6. Build view and store idempotency record
    CourseAttemptView view = toView(saved);
    String responseBodyJson = jsonCodecPort.toJson(view);
    idempotencyStorePort.save(
        command.idempotencyKey(), OPERATION, command.requestHash(), 201, responseBodyJson);

    // 7. Audit (CROSS-4: COURSE_ATTEMPT_REGISTERED)
    auditRecorder.record(
        new RecordAuditEventCommand(
            command.actorId(),
            "STUDENT",
            "COURSE_ATTEMPT_REGISTERED",
            "courseAttempt",
            attemptId.value(),
            null,
            null,
            null));

    return view;
  }

  private CourseAttemptView toView(CourseAttempt attempt) {
    BigDecimal gradeValue = attempt.getGrade() != null ? attempt.getGrade().value() : null;
    return new CourseAttemptView(
        attempt.getId().value(),
        attempt.getSubscriptionId().value(),
        attempt.getCourseId(),
        "N/A", // courseName resolved by query service (WU4)
        attempt.getPeriodId(),
        null, // periodName resolved by query service (WU4)
        attempt.getJourneyCode(),
        attempt.getStatus().name(), // FIX-S1: plain String, no domain type in application DTO
        gradeValue,
        attempt.getRegisteredAt(),
        attempt.getVersion());
  }
}
