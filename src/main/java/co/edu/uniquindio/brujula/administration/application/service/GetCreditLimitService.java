package co.edu.uniquindio.brujula.administration.application.service;

import co.edu.uniquindio.brujula.administration.application.port.in.GetCreditLimitQuery;
import co.edu.uniquindio.brujula.administration.application.port.out.LoadCreditLimitPort;
import java.util.UUID;

/**
 * Application service: returns the configured credit limit for a (programId, periodId) pair.
 *
 * <p>When no explicit limit has been persisted, falls back to the system default of 23 credits
 * (ADR-013, ADR-F4-006, spec API-002 — "default 23, range minimum 1, maximum 60").
 *
 * <p>If a persisted override is outside the valid range [1, 60], an {@link IllegalStateException}
 * is thrown immediately — a misconfigured value must never silently propagate to WU4's validation
 * engine.
 *
 * <p>Framework-free: no Spring or JPA dependencies in this class (ARQ-002). Wired via
 * {@code @Bean} in {@link co.edu.uniquindio.brujula.administration.infrastructure.configuration.AdministrationConfiguration}.
 */
public class GetCreditLimitService implements GetCreditLimitQuery {

  /** System default credit limit when no explicit override has been configured (ADR-013). */
  private static final int DEFAULT_CREDIT_LIMIT = 23;

  /** Minimum allowed credit limit (inclusive), per spec API-002. */
  private static final int MIN_CREDIT_LIMIT = 1;

  /** Maximum allowed credit limit (inclusive), per spec API-002. */
  private static final int MAX_CREDIT_LIMIT = 60;

  private final LoadCreditLimitPort loadCreditLimitPort;

  public GetCreditLimitService(LoadCreditLimitPort loadCreditLimitPort) {
    this.loadCreditLimitPort = loadCreditLimitPort;
  }

  @Override
  public int getCreditLimit(UUID programId, UUID periodId) {
    return loadCreditLimitPort
        .load(programId, periodId)
        .map(value -> validateRange(value, programId, periodId))
        .orElse(DEFAULT_CREDIT_LIMIT);
  }

  private int validateRange(int value, UUID programId, UUID periodId) {
    if (value < MIN_CREDIT_LIMIT || value > MAX_CREDIT_LIMIT) {
      throw new IllegalStateException(
          String.format(
              "Persisted credit limit %d for program %s / period %s is outside the valid range"
                  + " [1,60]. Correct the configuration before restarting.",
              value, programId, periodId));
    }
    return value;
  }
}
