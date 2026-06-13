package co.edu.uniquindio.brujula.administration.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import co.edu.uniquindio.brujula.administration.application.port.in.GetCreditLimitQuery;
import co.edu.uniquindio.brujula.administration.application.port.out.LoadCreditLimitPort;
import co.edu.uniquindio.brujula.administration.application.service.GetCreditLimitService;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link GetCreditLimitService} — TDD RED→GREEN.
 *
 * <p>Scenarios:
 *
 * <ul>
 *   <li>(1) no override configured → returns default 23
 *   <li>(2) explicit override configured → returns that override
 *   <li>(3) override of 0 (below minimum 1) → throws IllegalStateException (bounds guard)
 *   <li>(4) override of 999 (above maximum 60) → throws IllegalStateException (bounds guard)
 *   <li>(5) override of 1 (minimum boundary) → returned as-is
 *   <li>(6) override of 60 (maximum boundary) → returned as-is
 * </ul>
 */
class GetCreditLimitServiceTest {

  private LoadCreditLimitPort loadCreditLimitPort;
  private GetCreditLimitQuery service;

  private final UUID programId = UUID.randomUUID();
  private final UUID periodId = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    loadCreditLimitPort = mock(LoadCreditLimitPort.class);
    service = new GetCreditLimitService(loadCreditLimitPort);
  }

  @Test
  void returnsDefaultOf23WhenNoOverrideIsConfigured() {
    when(loadCreditLimitPort.load(programId, periodId)).thenReturn(Optional.empty());

    int limit = service.getCreditLimit(programId, periodId);

    assertThat(limit).isEqualTo(23);
  }

  @Test
  void returnsOverrideValueWhenConfigured() {
    when(loadCreditLimitPort.load(programId, periodId)).thenReturn(Optional.of(18));

    int limit = service.getCreditLimit(programId, periodId);

    assertThat(limit).isEqualTo(18);
  }

  @Test
  void throwsIllegalStateExceptionWhenOverrideIsBelowMinimum() {
    when(loadCreditLimitPort.load(programId, periodId)).thenReturn(Optional.of(0));

    org.assertj.core.api.Assertions.assertThatThrownBy(
            () -> service.getCreditLimit(programId, periodId))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("0")
        .hasMessageContaining("[1,60]");
  }

  @Test
  void throwsIllegalStateExceptionWhenOverrideIsAboveMaximum() {
    when(loadCreditLimitPort.load(programId, periodId)).thenReturn(Optional.of(999));

    org.assertj.core.api.Assertions.assertThatThrownBy(
            () -> service.getCreditLimit(programId, periodId))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("999")
        .hasMessageContaining("[1,60]");
  }

  @Test
  void returnsMinimumBoundaryValue1AsIs() {
    when(loadCreditLimitPort.load(programId, periodId)).thenReturn(Optional.of(1));

    int limit = service.getCreditLimit(programId, periodId);

    assertThat(limit).isEqualTo(1);
  }

  @Test
  void returnsMaximumBoundaryValue60AsIs() {
    when(loadCreditLimitPort.load(programId, periodId)).thenReturn(Optional.of(60));

    int limit = service.getCreditLimit(programId, periodId);

    assertThat(limit).isEqualTo(60);
  }
}
