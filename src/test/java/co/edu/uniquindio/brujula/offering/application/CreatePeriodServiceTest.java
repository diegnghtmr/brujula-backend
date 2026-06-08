package co.edu.uniquindio.brujula.offering.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.edu.uniquindio.brujula.audit.application.port.in.AuditRecorder;
import co.edu.uniquindio.brujula.offering.application.dto.AcademicPeriodView;
import co.edu.uniquindio.brujula.offering.application.dto.CreatePeriodCommand;
import co.edu.uniquindio.brujula.offering.application.port.out.SavePeriodPort;
import co.edu.uniquindio.brujula.offering.application.service.CreatePeriodService;
import co.edu.uniquindio.brujula.offering.domain.AcademicPeriod;
import co.edu.uniquindio.brujula.offering.domain.AcademicPeriodId;
import co.edu.uniquindio.brujula.offering.domain.PeriodStatus;
import co.edu.uniquindio.brujula.sharedkernel.application.exception.IdempotencyKeyConflictException;
import co.edu.uniquindio.brujula.sharedkernel.application.port.out.IdempotencyStorePort;
import co.edu.uniquindio.brujula.sharedkernel.application.port.out.JsonCodecPort;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * TDD unit tests for {@link CreatePeriodService}.
 *
 * <p>Covers: new key creates+saves+records audit; idempotency replay returns stored view;
 * same-key+different-hash throws {@link IdempotencyKeyConflictException}. Zero Spring.
 */
class CreatePeriodServiceTest {

  private SavePeriodPort savePeriodPort;
  private IdempotencyStorePort idempotencyStorePort;
  private JsonCodecPort jsonCodecPort;
  private AuditRecorder auditRecorder;
  private CreatePeriodService service;

  @BeforeEach
  void setUp() {
    savePeriodPort = mock(SavePeriodPort.class);
    idempotencyStorePort = mock(IdempotencyStorePort.class);
    jsonCodecPort = mock(JsonCodecPort.class);
    auditRecorder = mock(AuditRecorder.class);
    service = new CreatePeriodService(savePeriodPort, idempotencyStorePort, jsonCodecPort, auditRecorder);
  }

  @Test
  void new_key_creates_period_saves_and_records_audit() {
    UUID idempotencyKey = UUID.randomUUID();
    UUID actorId = UUID.randomUUID();
    String requestHash = "hash-abc";
    LocalDate start = LocalDate.of(2026, 7, 1);
    LocalDate end = LocalDate.of(2026, 12, 15);

    when(idempotencyStorePort.find(idempotencyKey, "CREATE_PERIOD")).thenReturn(Optional.empty());

    AcademicPeriod savedPeriod = AcademicPeriod.reconstitute(
        AcademicPeriodId.of(UUID.randomUUID()), "2026-2", start, end, PeriodStatus.UPCOMING, 0L);
    when(savePeriodPort.save(any())).thenReturn(savedPeriod);
    when(jsonCodecPort.toJson(any())).thenReturn("{\"id\":\"...\"}");

    CreatePeriodCommand command = new CreatePeriodCommand(
        idempotencyKey, actorId, "2026-2", start, end, "UPCOMING", requestHash);

    AcademicPeriodView result = service.createPeriod(command);

    assertThat(result).isNotNull();
    assertThat(result.name()).isEqualTo("2026-2");
    assertThat(result.version()).isEqualTo(0L);
    verify(savePeriodPort).save(any());
    verify(auditRecorder).record(argThat(cmd -> "PERIOD_CREATED".equals(cmd.action())));
    verify(jsonCodecPort).toJson(any(AcademicPeriodView.class));
    verify(idempotencyStorePort).save(
        eq(idempotencyKey), eq("CREATE_PERIOD"), eq(requestHash), eq(201), any(String.class));
  }

  @Test
  void same_key_same_hash_replays_stored_view() {
    UUID idempotencyKey = UUID.randomUUID();
    String requestHash = "hash-abc";
    UUID periodId = UUID.randomUUID();
    String storedJson = "{\"id\":\"" + periodId + "\",\"name\":\"2026-2\",\"startDate\":null,"
        + "\"endDate\":null,\"status\":\"UPCOMING\",\"version\":0}";
    AcademicPeriodView replayView = new AcademicPeriodView(periodId, "2026-2", null, null, "UPCOMING", 0L);

    when(idempotencyStorePort.find(idempotencyKey, "CREATE_PERIOD"))
        .thenReturn(Optional.of(new IdempotencyStorePort.StoredRecord(requestHash, 201, storedJson)));
    when(jsonCodecPort.fromJson(storedJson, AcademicPeriodView.class)).thenReturn(replayView);

    CreatePeriodCommand command = new CreatePeriodCommand(
        idempotencyKey, UUID.randomUUID(), "2026-2", null, null, "UPCOMING", requestHash);

    AcademicPeriodView result = service.createPeriod(command);

    assertThat(result.id()).isEqualTo(periodId);
    assertThat(result.name()).isEqualTo("2026-2");
    verify(savePeriodPort, never()).save(any());
    verify(auditRecorder, never()).record(any());
    verify(jsonCodecPort).fromJson(storedJson, AcademicPeriodView.class);
  }

  @Test
  void same_key_different_hash_throws_conflict() {
    UUID idempotencyKey = UUID.randomUUID();
    String storedJson = "{\"id\":\"00000000-0000-0000-0000-000000000001\"}";

    when(idempotencyStorePort.find(idempotencyKey, "CREATE_PERIOD"))
        .thenReturn(Optional.of(new IdempotencyStorePort.StoredRecord("stored-hash", 201, storedJson)));

    CreatePeriodCommand command = new CreatePeriodCommand(
        idempotencyKey, UUID.randomUUID(), "2026-2", null, null, "UPCOMING", "different-hash");

    assertThatThrownBy(() -> service.createPeriod(command))
        .isInstanceOf(IdempotencyKeyConflictException.class);
    verify(savePeriodPort, never()).save(any());
  }
}
