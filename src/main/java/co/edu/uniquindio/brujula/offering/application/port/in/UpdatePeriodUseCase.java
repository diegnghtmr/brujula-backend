package co.edu.uniquindio.brujula.offering.application.port.in;

import co.edu.uniquindio.brujula.offering.application.dto.AcademicPeriodView;
import co.edu.uniquindio.brujula.offering.application.dto.UpdatePeriodCommand;

/**
 * Input port: update an academic period (optimistic lock, ADMIN-only).
 *
 * <p>Part of {@code offering::api} named interface (package-info annotates the package). Satisfies:
 * OFFW-002.
 */
public interface UpdatePeriodUseCase {

  /**
   * Update an academic period applying the non-null fields from the command.
   *
   * @param command the update command (must not be null)
   * @return view of the updated period with bumped version
   */
  AcademicPeriodView updatePeriod(UpdatePeriodCommand command);
}
