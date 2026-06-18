package co.edu.uniquindio.brujula.administration.application.port.in;

import co.edu.uniquindio.brujula.administration.application.dto.StaffAccountView;
import java.util.UUID;

/**
 * Input port: update a staff account status (activate / deactivate).
 *
 * <p>Takes a status name string and returns {@link StaffAccountView} (application-layer DTO) so
 * REST adapters do not import domain types.
 */
public interface UpdateStaffUseCase {

  /**
   * Update the status of a staff account.
   *
   * @param staffId the staff account identifier
   * @param newStatusName the desired status name (ACTIVE | INACTIVE)
   * @param version the client's known version (optimistic locking)
   * @param actorId the internal ID of the admin performing the action (for audit)
   * @return the updated staff account view
   */
  StaffAccountView updateStatus(UUID staffId, String newStatusName, long version, UUID actorId);
}
