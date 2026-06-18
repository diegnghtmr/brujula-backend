package co.edu.uniquindio.brujula.simulation.infrastructure.adapter.out.administration;

import co.edu.uniquindio.brujula.administration.application.port.in.GetCreditLimitQuery;
import co.edu.uniquindio.brujula.simulation.application.port.out.CreditLimitPort;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Anti-corruption adapter: bridges simulation's {@link CreditLimitPort} to administration's
 * published {@code administration::api.GetCreditLimitQuery}.
 *
 * <p>Default credit limit is 23 when no explicit override exists.
 */
@Component
class CreditLimitAdapter implements CreditLimitPort {

  private final GetCreditLimitQuery getCreditLimitQuery;

  CreditLimitAdapter(GetCreditLimitQuery getCreditLimitQuery) {
    this.getCreditLimitQuery = getCreditLimitQuery;
  }

  @Override
  public int creditLimit(UUID programId, UUID periodId) {
    return getCreditLimitQuery.getCreditLimit(programId, periodId);
  }
}
