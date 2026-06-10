package co.edu.uniquindio.brujula.offering.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.edu.uniquindio.brujula.audit.application.port.in.AuditRecorder;
import co.edu.uniquindio.brujula.curriculum.application.dto.CourseView;
import co.edu.uniquindio.brujula.curriculum.application.port.in.GetCourseQuery;
import co.edu.uniquindio.brujula.offering.application.dto.OfferingDetailView;
import co.edu.uniquindio.brujula.offering.application.dto.PublishOfferingCommand;
import co.edu.uniquindio.brujula.offering.application.port.out.SaveOfferingPort;
import co.edu.uniquindio.brujula.offering.application.service.PublishOfferingService;
import co.edu.uniquindio.brujula.offering.domain.CourseOffering;
import co.edu.uniquindio.brujula.offering.domain.CourseOfferingId;
import co.edu.uniquindio.brujula.offering.domain.IllegalOfferingTransitionException;
import co.edu.uniquindio.brujula.offering.domain.OfferingNotFoundException;
import co.edu.uniquindio.brujula.offering.domain.OfferingNotPublishableException;
import co.edu.uniquindio.brujula.offering.domain.OfferingStatus;
import co.edu.uniquindio.brujula.offering.domain.ScheduleOption;
import co.edu.uniquindio.brujula.offering.domain.ScheduleOptionId;
import co.edu.uniquindio.brujula.offering.domain.ScheduleSession;
import co.edu.uniquindio.brujula.offering.domain.Weekday;
import co.edu.uniquindio.brujula.sharedkernel.application.exception.IdempotencyKeyConflictException;
import co.edu.uniquindio.brujula.sharedkernel.application.port.out.IdempotencyStorePort;
import co.edu.uniquindio.brujula.sharedkernel.application.port.out.JsonCodecPort;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.OptimisticLockingFailureException;

/**
 * TDD unit tests for {@link PublishOfferingService}.
 *
 * <p>Covers:
 *
 * <ul>
 *   <li>Idempotency replay: same key + same hash returns stored view
 *   <li>Idempotency conflict: same key + different hash throws IdempotencyKeyConflictException
 *   <li>Offering not found throws OfferingNotFoundException
 *   <li>Illegal transition propagates IllegalOfferingTransitionException (→ 409)
 *   <li>No-options precondition propagates OfferingNotPublishableException (→ 422)
 *   <li>Stale version propagates OptimisticLockingFailureException (→ 409)
 *   <li>Intensity mismatch advisory: successful transition (200) + intensityWarning=true
 *   <li>No intensity mismatch: successful transition (200) + intensityWarning=false
 *   <li>Successful publish: returned view version > expectedVersion (staleness keystone)
 *   <li>Audit event recorded with OFFERING_PUBLISHED action
 * </ul>
 *
 * <p>Zero Spring — pure unit test. Satisfies: OFFW-005, OFFW-006, WU3 Phase 4.
 */
class PublishOfferingServiceTest {

  private SaveOfferingPort saveOfferingPort;
  private IdempotencyStorePort idempotencyStorePort;
  private JsonCodecPort jsonCodecPort;
  private AuditRecorder auditRecorder;
  private GetCourseQuery getCourseQuery;
  private PublishOfferingService service;

  private final UUID offeringIdValue = UUID.randomUUID();
  private final CourseOfferingId offeringId = CourseOfferingId.of(offeringIdValue);
  private final UUID courseId = UUID.randomUUID();
  private final UUID programId = UUID.randomUUID();
  private final UUID periodId = UUID.randomUUID();
  private final UUID actorId = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    saveOfferingPort = mock(SaveOfferingPort.class);
    idempotencyStorePort = mock(IdempotencyStorePort.class);
    jsonCodecPort = mock(JsonCodecPort.class);
    auditRecorder = mock(AuditRecorder.class);
    getCourseQuery = mock(GetCourseQuery.class);
    service =
        new PublishOfferingService(
            saveOfferingPort, idempotencyStorePort, jsonCodecPort, auditRecorder, getCourseQuery);
  }

  // -------------------------------------------------------------------------
  // Idempotency
  // -------------------------------------------------------------------------

  @Test
  void idempotency_replay_same_hash_returns_stored_view() {
    UUID iKey = UUID.randomUUID();
    String hash = "abc123";
    OfferingDetailView stored = sampleView("PRELIMINARY", 2L, null);

    when(idempotencyStorePort.find(iKey, "PUBLISH_OFFERING"))
        .thenReturn(Optional.of(new IdempotencyStorePort.StoredRecord(hash, 200, "{}")));
    when(jsonCodecPort.fromJson("{}", OfferingDetailView.class)).thenReturn(stored);

    PublishOfferingCommand command = publishCommand(iKey, "PRELIMINARY", 1L, hash);
    OfferingDetailView result = service.publishOffering(command);

    assertThat(result).isEqualTo(stored);
    verify(saveOfferingPort, never()).save(any());
  }

  @Test
  void idempotency_conflict_different_hash_throws() {
    UUID iKey = UUID.randomUUID();
    String storedHash = "hash1";
    String requestHash = "hash2";

    when(idempotencyStorePort.find(iKey, "PUBLISH_OFFERING"))
        .thenReturn(Optional.of(new IdempotencyStorePort.StoredRecord(storedHash, 200, "{}")));

    PublishOfferingCommand command = publishCommand(iKey, "PRELIMINARY", 1L, requestHash);

    assertThatThrownBy(() -> service.publishOffering(command))
        .isInstanceOf(IdempotencyKeyConflictException.class);
  }

  // -------------------------------------------------------------------------
  // Offering not found
  // -------------------------------------------------------------------------

  @Test
  void offering_not_found_throws_OfferingNotFoundException() {
    when(idempotencyStorePort.find(any(), any())).thenReturn(Optional.empty());
    when(saveOfferingPort.findCurrentById(offeringId)).thenReturn(Optional.empty());

    PublishOfferingCommand command = publishCommand(UUID.randomUUID(), "PRELIMINARY", 1L, "h");

    assertThatThrownBy(() -> service.publishOffering(command))
        .isInstanceOf(OfferingNotFoundException.class);
  }

  // -------------------------------------------------------------------------
  // Domain guard propagation
  // -------------------------------------------------------------------------

  @Test
  void illegal_transition_propagates() {
    CourseOffering archived = offeringWith(OfferingStatus.ARCHIVED, Collections.emptyList(), 1L);
    when(idempotencyStorePort.find(any(), any())).thenReturn(Optional.empty());
    when(saveOfferingPort.findCurrentById(offeringId)).thenReturn(Optional.of(archived));

    PublishOfferingCommand command = publishCommand(UUID.randomUUID(), "PRELIMINARY", 1L, "h");

    assertThatThrownBy(() -> service.publishOffering(command))
        .isInstanceOf(IllegalOfferingTransitionException.class);
  }

  @Test
  void no_options_precondition_propagates() {
    CourseOffering draft = offeringWith(OfferingStatus.DRAFT, Collections.emptyList(), 0L);
    when(idempotencyStorePort.find(any(), any())).thenReturn(Optional.empty());
    when(saveOfferingPort.findCurrentById(offeringId)).thenReturn(Optional.of(draft));

    PublishOfferingCommand command = publishCommand(UUID.randomUUID(), "PRELIMINARY", 0L, "h");

    assertThatThrownBy(() -> service.publishOffering(command))
        .isInstanceOf(OfferingNotPublishableException.class);
  }

  // -------------------------------------------------------------------------
  // Optimistic lock
  // -------------------------------------------------------------------------

  @Test
  void stale_version_propagates() {
    CourseOffering draft = offeringWith(OfferingStatus.DRAFT, List.of(oneOption()), 3L);
    when(idempotencyStorePort.find(any(), any())).thenReturn(Optional.empty());
    when(saveOfferingPort.findCurrentById(offeringId)).thenReturn(Optional.of(draft));
    when(saveOfferingPort.save(any())).thenThrow(new OptimisticLockingFailureException("stale"));
    when(getCourseQuery.getCourse(courseId)).thenReturn(courseViewWithHours(4));

    PublishOfferingCommand command = publishCommand(UUID.randomUUID(), "PRELIMINARY", 1L, "h");

    assertThatThrownBy(() -> service.publishOffering(command))
        .isInstanceOf(OptimisticLockingFailureException.class);
  }

  // -------------------------------------------------------------------------
  // Intensity advisory (MUST be REAL — not a no-op)
  // -------------------------------------------------------------------------

  @Test
  void intensity_mismatch_returns_200_with_warning_true_never_throws() {
    // Schedule option has 0 weeklyHours (empty sessions) vs curriculum 4 hrs — MISMATCH
    CourseOffering draft = offeringWith(OfferingStatus.DRAFT, List.of(oneOption()), 0L);
    long bumpedVersion = 1L;
    CourseOffering saved =
        offeringWith(OfferingStatus.PRELIMINARY, List.of(oneOption()), bumpedVersion);

    when(idempotencyStorePort.find(any(), any())).thenReturn(Optional.empty());
    when(saveOfferingPort.findCurrentById(offeringId)).thenReturn(Optional.of(draft));
    when(saveOfferingPort.save(any())).thenReturn(saved);
    // 4 curricular hours but option has 0 weeklyHours → mismatch
    when(getCourseQuery.getCourse(courseId)).thenReturn(courseViewWithHours(4));
    when(jsonCodecPort.toJson(any())).thenReturn("{}");

    PublishOfferingCommand command = publishCommand(UUID.randomUUID(), "PRELIMINARY", 0L, "h");
    OfferingDetailView result = service.publishOffering(command);

    // Must succeed (200), must set warning flag, must NOT throw
    assertThat(result.intensityWarning()).isTrue();
    assertThat(result.status()).isEqualTo("PRELIMINARY");
  }

  @Test
  void intensity_match_returns_200_with_warning_false() {
    // Option with 4 hrs/week (2 sessions × 2 hrs = 4 hrs), curriculum = 4 → NO mismatch
    ScheduleOption matchingOption =
        optionWithSessions(
            new ScheduleSession(
                UUID.randomUUID(), Weekday.MONDAY, LocalTime.of(8, 0), LocalTime.of(10, 0)),
            new ScheduleSession(
                UUID.randomUUID(), Weekday.WEDNESDAY, LocalTime.of(8, 0), LocalTime.of(10, 0)));

    CourseOffering draft = offeringWith(OfferingStatus.DRAFT, List.of(matchingOption), 0L);
    CourseOffering saved = offeringWith(OfferingStatus.PRELIMINARY, List.of(matchingOption), 1L);

    when(idempotencyStorePort.find(any(), any())).thenReturn(Optional.empty());
    when(saveOfferingPort.findCurrentById(offeringId)).thenReturn(Optional.of(draft));
    when(saveOfferingPort.save(any())).thenReturn(saved);
    // 4 curricular hours; option weeklyHours = 4.0 → match
    when(getCourseQuery.getCourse(courseId)).thenReturn(courseViewWithHours(4));
    when(jsonCodecPort.toJson(any())).thenReturn("{}");

    PublishOfferingCommand command = publishCommand(UUID.randomUUID(), "PRELIMINARY", 0L, "h");
    OfferingDetailView result = service.publishOffering(command);

    assertThat(result.intensityWarning()).isFalse();
  }

  // -------------------------------------------------------------------------
  // Staleness keystone: version bump
  // -------------------------------------------------------------------------

  @Test
  void success_returns_view_with_bumped_version() {
    long expectedVersion = 2L;
    long bumpedVersion = 3L;

    CourseOffering draft =
        offeringWith(OfferingStatus.DRAFT, List.of(oneOption()), expectedVersion);
    CourseOffering saved =
        offeringWith(OfferingStatus.PRELIMINARY, List.of(oneOption()), bumpedVersion);

    when(idempotencyStorePort.find(any(), any())).thenReturn(Optional.empty());
    when(saveOfferingPort.findCurrentById(offeringId)).thenReturn(Optional.of(draft));
    when(saveOfferingPort.save(any())).thenReturn(saved);
    when(getCourseQuery.getCourse(courseId)).thenReturn(courseViewWithHours(4));
    when(jsonCodecPort.toJson(any())).thenReturn("{}");

    PublishOfferingCommand command =
        publishCommand(UUID.randomUUID(), "PRELIMINARY", expectedVersion, "h");
    OfferingDetailView result = service.publishOffering(command);

    assertThat(result.version()).isGreaterThan(expectedVersion);
    assertThat(result.version()).isEqualTo(bumpedVersion);
  }

  // -------------------------------------------------------------------------
  // Audit
  // -------------------------------------------------------------------------

  @Test
  void success_records_audit_event_with_offering_published() {
    CourseOffering draft = offeringWith(OfferingStatus.DRAFT, List.of(oneOption()), 0L);
    CourseOffering saved = offeringWith(OfferingStatus.PRELIMINARY, List.of(oneOption()), 1L);

    when(idempotencyStorePort.find(any(), any())).thenReturn(Optional.empty());
    when(saveOfferingPort.findCurrentById(offeringId)).thenReturn(Optional.of(draft));
    when(saveOfferingPort.save(any())).thenReturn(saved);
    when(getCourseQuery.getCourse(courseId)).thenReturn(courseViewWithHours(4));
    when(jsonCodecPort.toJson(any())).thenReturn("{}");

    PublishOfferingCommand command = publishCommand(UUID.randomUUID(), "PRELIMINARY", 0L, "h");
    service.publishOffering(command);

    verify(auditRecorder)
        .record(
            argThat(
                cmd -> "OFFERING_PUBLISHED".equals(cmd.action()) && actorId.equals(cmd.actorId())));
  }

  // -------------------------------------------------------------------------
  // Helpers
  // -------------------------------------------------------------------------

  private PublishOfferingCommand publishCommand(
      UUID iKey, String targetStatus, long version, String hash) {
    return new PublishOfferingCommand(
        iKey, offeringIdValue, actorId, targetStatus, version, false, hash);
  }

  private CourseOffering offeringWith(
      OfferingStatus status, List<ScheduleOption> options, long version) {
    return CourseOffering.reconstitute(
        offeringId, courseId, "Course", programId, periodId, status, null, options, version);
  }

  private ScheduleOption oneOption() {
    return ScheduleOption.reconstitute(
        ScheduleOptionId.generate(), offeringIdValue, "A", "DIURNA", Collections.emptyList(), 0L);
  }

  private ScheduleOption optionWithSessions(ScheduleSession... sessions) {
    return ScheduleOption.reconstitute(
        ScheduleOptionId.generate(), offeringIdValue, "A", "DIURNA", List.of(sessions), 0L);
  }

  private CourseView courseViewWithHours(int curricularWeeklyHours) {
    return new CourseView(
        courseId,
        UUID.randomUUID(),
        programId,
        "PROG-001",
        "Course",
        "REQUIRED",
        3,
        curricularWeeklyHours,
        Collections.emptyList(),
        "ACTIVE",
        0L);
  }

  private OfferingDetailView sampleView(String status, long version, Boolean intensityWarning) {
    return new OfferingDetailView(
        offeringIdValue,
        courseId,
        "Course",
        programId,
        periodId,
        status,
        null,
        version,
        Collections.emptyList(),
        intensityWarning);
  }
}
