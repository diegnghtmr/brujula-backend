package co.edu.uniquindio.brujula.progress.application.port.in;

import co.edu.uniquindio.brujula.progress.application.dto.CreateSubscriptionCommand;
import co.edu.uniquindio.brujula.progress.application.dto.SubscriptionView;

/**
 * Input port: create a new subscription.
 *
 * <p>Idempotent via Idempotency-Key. Cap=2 enforced. ARQ-003: interface only.
 */
public interface CreateSubscriptionUseCase {

  /**
   * Execute the create-subscription command.
   *
   * @param command validated create command with idempotency key
   * @return the created (or replayed) subscription view
   */
  SubscriptionView createSubscription(CreateSubscriptionCommand command);
}
