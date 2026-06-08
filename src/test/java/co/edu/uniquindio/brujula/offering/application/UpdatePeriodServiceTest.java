package co.edu.uniquindio.brujula.offering.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.edu.uniquindio.brujula.audit.application.port.in.AuditRecorder;
import co.edu.uniquindio.brujula.offering.application.dto.AcademicPeriodView;
import co.edu.uniquindio.brujula.offering.application.dto.UpdatePeriodCommand;
import co.edu.uniquindio.brujula.offering.application.port.out.SavePeriodPort;
import co.edu.uniquindio.brujula.offering.application.service.UpdatePeriodService;
import co.edu.uniquindio.brujula.offering.domain.AcademicPeriod;
import co.edu.uniquindio.brujula.offering.domain.AcademicPeriodId;
import co.edu.uniquindio.brujula.offering.domain.PeriodNotFoundException;
import co.edu.uniquindio.brujula.offering.domain.PeriodStatus;
import org.springframework.dao.OptimisticLockingFailureException;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * TDD unit tests for {@link UpdatePeriodService}.
 *
 * <p>Covers: period not found → PeriodNotFoundException; optimistic lock failure propagates;
 * success returns view with bumped version. Zero Spring.
 */
class UpdatePeriodServiceTest {

  private SavePeriodPort savePeriodPort;
  private AuditRecorder auditRecorder;
  private UpdatePeriodService service;

  private final AcademicPeriodId periodId = AcademicPeriodId.of(UUID.randomUUID());
  private final LocalDate start = LocalDate.of(2026, 7, 1);
  private final LocalDate end = LocalDate.of(2026, 12, 15);

  @BeforeEach
  void setUp() {
    savePeriodPort = mock(SavePeriodPort.class);
    auditRecorder = mock(AuditRecorder.class);
    service = new UpdatePeriodService(savePeriodPort, auditRecorder);
  }

  @Test
  void period_not_found_throws_PeriodNotFoundException() {
    when(savePeriodPort.findCurrentById(periodId)).thenReturn(Optional.empty());

    UpdatePeriodCommand command = new UpdatePeriodCommand(
        periodId.value(), UUID.randomUUID(), null, null, "ACTIVE", 0L);

    assertThatThrownBy(() -> service.updatePeriod(command))
        .isInstanceOf(PeriodNotFoundException.class);
  }

  @Test
  void optimistic_lock_conflict_propagates() {
    AcademicPeriod existing = AcademicPeriod.reconstitute(
        periodId, "2026-2", start, end, PeriodStatus.UPCOMING, 1L);
    when(savePeriodPort.findCurrentById(periodId)).thenReturn(Optional.of(existing));
    when(savePeriodPort.save(any())).thenThrow(new OptimisticLockingFailureException("stale version"));

    UpdatePeriodCommand command = new UpdatePeriodCommand(
        periodId.value(), UUID.randomUUID(), null, null, "ACTIVE", 0L);

    assertThatThrownBy(() -> service.updatePeriod(command))
        .isInstanceOf(OptimisticLockingFailureException.class);
  }

  @Test
  void success_returns_view_with_bumped_version() {
    AcademicPeriod existing = AcademicPeriod.reconstitute(
        periodId, "2026-2", start, end, PeriodStatus.UPCOMING, 2L);
    // Fake the @Version bump: save returns version+1
    AcademicPeriod saved = AcademicPeriod.reconstitute(
        periodId, "2026-2", start, end, PeriodStatus.ACTIVE, 3L);

    when(savePeriodPort.findCurrentById(periodId)).thenReturn(Optional.of(existing));
    when(savePeriodPort.save(any())).thenReturn(saved);

    UpdatePeriodCommand command = new UpdatePeriodCommand(
        periodId.value(), UUID.randomUUID(), null, null, "ACTIVE", 2L);

    AcademicPeriodView result = service.updatePeriod(command);

    assertThat(result.status()).isEqualTo("ACTIVE");
    assertThat(result.version()).isGreaterThan(command.expectedVersion());
    verify(auditRecorder).record(argThat(cmd -> "PERIOD_UPDATED".equals(cmd.action())));
  }
}
