package co.edu.uniquindio.brujula.offering.application.port.in;

import co.edu.uniquindio.brujula.offering.application.dto.OfferingDetailView;
import co.edu.uniquindio.brujula.offering.application.dto.PublishOfferingCommand;

/**
 * Input port: publish (state-transition) a course offering.
 *
 * <p>Enforces the legal-transition table, the schedule-option precondition for
 * PRELIMINARY/FINAL, idempotency, and optimistic locking. Returns a detail view whose
 * {@code version} is strictly greater than the submitted {@code expectedVersion} and whose
 * {@code intensityWarning} flag reflects any advisory mismatch.
 *
 * <p>Satisfies: OFFW-005.
 */
public interface PublishOfferingUseCase {

  /**
   * Transition the offering identified by {@code command.offeringId()} to the requested target
   * status.
   *
   * @param command the publish command
   * @return the updated offering detail view (including advisory intensity warning flag)
   * @throws co.edu.uniquindio.brujula.offering.domain.OfferingNotFoundException if the offering
   *         does not exist
   * @throws co.edu.uniquindio.brujula.offering.domain.IllegalOfferingTransitionException if the
   *         transition is illegal per the state machine (→ HTTP 409)
   * @throws co.edu.uniquindio.brujula.offering.domain.OfferingNotPublishableException if the
   *         target is PRELIMINARY or FINAL but the offering has no schedule options (→ HTTP 422)
   * @throws org.springframework.dao.OptimisticLockingFailureException if the version is stale
   *         (→ HTTP 409)
   * @throws co.edu.uniquindio.brujula.sharedkernel.application.exception.IdempotencyKeyConflictException
   *         if the same idempotency key was used with a different body (→ HTTP 409)
   */
  OfferingDetailView publishOffering(PublishOfferingCommand command);
}
