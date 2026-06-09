package co.edu.uniquindio.brujula.offering.application.port.in;

import co.edu.uniquindio.brujula.offering.application.dto.AcademicPeriodView;
import co.edu.uniquindio.brujula.offering.application.dto.CreatePeriodCommand;

/**
 * Input port: create an academic period (idempotent, ADMIN-only).
 *
 * <p>Part of {@code offering::api} named interface (package-info annotates the package). Satisfies:
 * OFFW-001.
 */
public interface CreatePeriodUseCase {

  /**
   * Create an academic period, respecting idempotency semantics.
   *
   * @param command the create command (must not be null)
   * @return view of the created or replayed period
   */
  AcademicPeriodView createPeriod(CreatePeriodCommand command);
}
