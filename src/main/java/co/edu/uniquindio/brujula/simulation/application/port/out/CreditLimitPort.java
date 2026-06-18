package co.edu.uniquindio.brujula.simulation.application.port.out;

import java.util.UUID;

/**
 * Output port: retrieve the configured credit limit for a program/period pair.
 *
 * <p>Delegates to {@code administration::api.GetCreditLimitQuery}. Default is 23 when no explicit
 * limit is configured (ADR-013, ADR-F4-006).
 *
 * <p>Named by need. Implemented by {@code CreditLimitAdapter}.
 */
public interface CreditLimitPort {

  /**
   * Return the maximum credit load for the given program in the given period.
   *
   * @param programId the program UUID
   * @param periodId the academic period UUID
   * @return configured credit limit, or 23 if no override exists
   */
  int creditLimit(UUID programId, UUID periodId);
}
