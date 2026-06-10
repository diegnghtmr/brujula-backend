package co.edu.uniquindio.brujula.offering.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.edu.uniquindio.brujula.audit.application.port.in.AuditRecorder;
import co.edu.uniquindio.brujula.offering.application.dto.OfferingDetailView;
import co.edu.uniquindio.brujula.offering.application.dto.UpdateOfferingCommand;
import co.edu.uniquindio.brujula.offering.application.port.out.SaveOfferingPort;
import co.edu.uniquindio.brujula.offering.application.service.UpdateOfferingService;
import co.edu.uniquindio.brujula.offering.domain.CourseOffering;
import co.edu.uniquindio.brujula.offering.domain.CourseOfferingId;
import co.edu.uniquindio.brujula.offering.domain.OfferingNotFoundException;
import co.edu.uniquindio.brujula.offering.domain.OfferingStatus;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.OptimisticLockingFailureException;

/**
 * TDD unit tests for {@link UpdateOfferingService}.
 *
 * <p>Covers: offering not found → OfferingNotFoundException; optimistic lock failure propagates;
 * returned view version &gt; input expectedVersion (version-bump test — staleness keystone).
 *
 * <p>The version-bump test is the staleness keystone: the simulation StalenessEvaluator reads
 * course_offering.version at query time; the only way OUTDATED fires is if updateOffering genuinely
 * increments @Version. This test proves the service propagates the bumped version from the
 * persistence layer back to the caller. Zero Spring.
 */
class UpdateOfferingServiceTest {

  private SaveOfferingPort saveOfferingPort;
  private AuditRecorder auditRecorder;
  private UpdateOfferingService service;

  private final UUID offeringIdValue = UUID.randomUUID();
  private final CourseOfferingId offeringId = CourseOfferingId.of(offeringIdValue);
  private final UUID courseId = UUID.randomUUID();
  private final UUID programId = UUID.randomUUID();
  private final UUID periodId = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    saveOfferingPort = mock(SaveOfferingPort.class);
    auditRecorder = mock(AuditRecorder.class);
    service = new UpdateOfferingService(saveOfferingPort, auditRecorder);
  }

  @Test
  void offering_not_found_throws_OfferingNotFoundException() {
    when(saveOfferingPort.findCurrentById(offeringId)).thenReturn(Optional.empty());

    UpdateOfferingCommand command =
        new UpdateOfferingCommand(offeringIdValue, UUID.randomUUID(), "notes", 0L);

    assertThatThrownBy(() -> service.updateOffering(command))
        .isInstanceOf(OfferingNotFoundException.class);
  }

  @Test
  void optimistic_lock_conflict_propagates() {
    CourseOffering existing =
        CourseOffering.reconstitute(
            offeringId,
            courseId,
            "Course",
            programId,
            periodId,
            OfferingStatus.DRAFT,
            "Old notes",
            Collections.emptyList(),
            2L);
    when(saveOfferingPort.findCurrentById(offeringId)).thenReturn(Optional.of(existing));
    when(saveOfferingPort.save(any())).thenThrow(new OptimisticLockingFailureException("stale"));

    UpdateOfferingCommand command =
        new UpdateOfferingCommand(offeringIdValue, UUID.randomUUID(), "New notes", 1L);

    assertThatThrownBy(() -> service.updateOffering(command))
        .isInstanceOf(OptimisticLockingFailureException.class);
  }

  /**
   * Staleness keystone test: returned view version MUST be greater than expectedVersion.
   *
   * <p>The fake SaveOfferingPort simulates the @Version increment (JPA bumps version on flush). If
   * UpdateOfferingService fails to use the returned saved object's version, this test catches the
   * bug — same bug that made the prior staleness path dead/fake-green.
   */
  @Test
  void success_returns_view_with_bumped_version_staleness_keystone() {
    long expectedVersion = 3L;
    long bumpedVersion = 4L; // @Version auto-incremented by JPA on flush

    CourseOffering existing =
        CourseOffering.reconstitute(
            offeringId,
            courseId,
            "Course",
            programId,
            periodId,
            OfferingStatus.DRAFT,
            "Old notes",
            Collections.emptyList(),
            expectedVersion);

    // Simulate adapter returning managed entity with bumped version (JPA @Version increment)
    CourseOffering savedWithBumpedVersion =
        CourseOffering.reconstitute(
            offeringId,
            courseId,
            "Course",
            programId,
            periodId,
            OfferingStatus.DRAFT,
            "New notes",
            Collections.emptyList(),
            bumpedVersion);

    when(saveOfferingPort.findCurrentById(offeringId)).thenReturn(Optional.of(existing));
    when(saveOfferingPort.save(any())).thenReturn(savedWithBumpedVersion);

    UpdateOfferingCommand command =
        new UpdateOfferingCommand(offeringIdValue, UUID.randomUUID(), "New notes", expectedVersion);

    OfferingDetailView result = service.updateOffering(command);

    assertThat(result.notes()).isEqualTo("New notes");
    // THE STALENESS KEYSTONE: version returned MUST be strictly greater than the submitted version
    assertThat(result.version()).isGreaterThan(expectedVersion);
    assertThat(result.version()).isEqualTo(bumpedVersion);
    verify(auditRecorder).record(argThat(cmd -> "OFFERING_UPDATED".equals(cmd.action())));
  }

  @Test
  void success_records_audit_event() {
    UUID actorId = UUID.randomUUID();
    CourseOffering existing =
        CourseOffering.reconstitute(
            offeringId,
            courseId,
            "Course",
            programId,
            periodId,
            OfferingStatus.DRAFT,
            "Old notes",
            Collections.emptyList(),
            0L);
    CourseOffering saved =
        CourseOffering.reconstitute(
            offeringId,
            courseId,
            "Course",
            programId,
            periodId,
            OfferingStatus.DRAFT,
            "New notes",
            Collections.emptyList(),
            1L);

    when(saveOfferingPort.findCurrentById(offeringId)).thenReturn(Optional.of(existing));
    when(saveOfferingPort.save(any())).thenReturn(saved);

    UpdateOfferingCommand command =
        new UpdateOfferingCommand(offeringIdValue, actorId, "New notes", 0L);

    service.updateOffering(command);

    verify(auditRecorder)
        .record(
            argThat(
                cmd -> "OFFERING_UPDATED".equals(cmd.action()) && actorId.equals(cmd.actorId())));
  }
}
