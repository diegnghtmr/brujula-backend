package co.edu.uniquindio.brujula.administration.infrastructure.adapter.out.persistence;

import co.edu.uniquindio.brujula.administration.application.port.out.LoadCreditLimitPort;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Default driven adapter for {@link LoadCreditLimitPort}.
 *
 * <p>The {@code administration.credit_limit} table does not exist yet (ADR-F4-006 — deferred). This
 * adapter returns {@code Optional.empty()} for every request, signalling to
 * {@link co.edu.uniquindio.brujula.administration.application.service.GetCreditLimitService} that
 * the system default of 23 should be used.
 *
 * <p>When the credit_limit table and its Flyway migration are added in a future fast-follow, this
 * adapter is replaced by a real JPA implementation without changing the application service.
 */
@Component
class DefaultCreditLimitAdapter implements LoadCreditLimitPort {

  @Override
  public Optional<Integer> load(UUID programId, UUID periodId) {
    // No credit_limit table yet — service applies default 23 (ADR-F4-006).
    return Optional.empty();
  }
}
