package co.edu.uniquindio.brujula.administration.application.port.out;

import java.util.Optional;
import java.util.UUID;

/**
 * Internal output port: reads a configured credit limit for a (programId, periodId) pair.
 *
 * <p>This is an SPI — not published to other modules. The implementation either queries a
 * persistence store or returns empty when no explicit override has been configured. The application
 * service applies the default of 23 when this port returns empty.
 *
 * <p>Method uses only UUID and boxed Integer — no domain entities or JPA types.
 */
public interface LoadCreditLimitPort {

  /**
   * Load the configured credit limit for the given program and period.
   *
   * @param programId the program UUID
   * @param periodId the academic period UUID
   * @return the configured limit, or empty if no explicit override exists
   */
  Optional<Integer> load(UUID programId, UUID periodId);
}
