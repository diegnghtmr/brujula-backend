package co.edu.uniquindio.brujula.offering.application.port.in;

import co.edu.uniquindio.brujula.offering.application.dto.CreateOfferingCommand;
import co.edu.uniquindio.brujula.offering.application.dto.OfferingDetailView;

/**
 * Input port: create a course offering (idempotent, STAFF/ADMIN).
 *
 * <p>Satisfies: OFFW-003.
 */
public interface CreateOfferingUseCase {

  /**
   * Create a new course offering and return the detail view.
   *
   * <p>Idempotency: same key + same body hash → replay stored result; same key + different hash →
   * {@link
   * co.edu.uniquindio.brujula.sharedkernel.application.exception.IdempotencyKeyConflictException}.
   *
   * @param command the create command
   * @return the offering detail view (new or replayed)
   */
  OfferingDetailView createOffering(CreateOfferingCommand command);
}
