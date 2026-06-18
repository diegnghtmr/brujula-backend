package co.edu.uniquindio.brujula.administration.application.port.in;

import java.util.UUID;

/**
 * Published input port — returns the configured credit limit for a (programId, periodId) pair.
 *
 * <p>Default value when no explicit limit is configured: 23. Range: minimum 1, maximum 60 per
 * contract CreditLimit schema.
 *
 * <p>Published via {@code @NamedInterface("api")} in this package's {@code package-info.java}.
 * Cross-module consumers (e.g. simulation) must access this port ONLY through {@code
 * administration::api}.
 *
 * <p>Method signature uses only UUID primitives — no domain entities or JPA types.
 */
public interface GetCreditLimitQuery {

  /**
   * Get the configured maximum credit load for the given program and period.
   *
   * @param programId the program UUID
   * @param periodId the academic period UUID
   * @return the configured credit limit, or the system default of 23 if no override exists
   */
  int getCreditLimit(UUID programId, UUID periodId);
}
