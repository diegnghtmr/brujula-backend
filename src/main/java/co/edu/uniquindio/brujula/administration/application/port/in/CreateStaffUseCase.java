package co.edu.uniquindio.brujula.administration.application.port.in;

import co.edu.uniquindio.brujula.administration.application.dto.CreateStaffCommand;
import co.edu.uniquindio.brujula.administration.application.dto.StaffAccountView;

/**
 * Input port: create a new staff account (idempotent via Idempotency-Key).
 *
 * <p>Returns {@link StaffAccountView} (application-layer DTO) so REST adapters do not import domain
 * types.
 */
public interface CreateStaffUseCase {

  /**
   * Create or replay a staff account creation.
   *
   * @param command the creation command including idempotency key and request hash
   * @return the created or replayed staff account view
   */
  StaffAccountView createStaff(CreateStaffCommand command);
}
