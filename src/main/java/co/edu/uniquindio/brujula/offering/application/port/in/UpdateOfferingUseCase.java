package co.edu.uniquindio.brujula.offering.application.port.in;

import co.edu.uniquindio.brujula.offering.application.dto.OfferingDetailView;
import co.edu.uniquindio.brujula.offering.application.dto.UpdateOfferingCommand;

/**
 * Input port: update offering notes with optimistic locking (STAFF/ADMIN, NOT idempotent).
 *
 * <p>Status transitions are NOT permitted — use {@code publishOffering} for that. A successful
 * update MUST increment {@code course_offering.version} (staleness activation invariant, OFFW-006).
 *
 * <p>Satisfies: OFFW-004.
 */
public interface UpdateOfferingUseCase {

  /**
   * Update the offering's notes and return the detail view with the bumped version.
   *
   * @param command the update command carrying {@code notes} and {@code expectedVersion}
   * @return the updated offering detail view
   * @throws co.edu.uniquindio.brujula.offering.domain.OfferingNotFoundException if not found
   * @throws org.springframework.dao.OptimisticLockingFailureException on version conflict
   */
  OfferingDetailView updateOffering(UpdateOfferingCommand command);
}
