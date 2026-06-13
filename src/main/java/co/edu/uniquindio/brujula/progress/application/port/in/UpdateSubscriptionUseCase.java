package co.edu.uniquindio.brujula.progress.application.port.in;

import co.edu.uniquindio.brujula.progress.application.dto.SubscriptionView;
import co.edu.uniquindio.brujula.progress.application.dto.UpdateSubscriptionCommand;

/**
 * Input port: update a subscription (plan and/or journey change).
 *
 * <p>Enforces acknowledgeHistoryImpact gate and optimistic locking.
 */
public interface UpdateSubscriptionUseCase {

  /**
   * Apply the update command.
   *
   * @param command the update command including version for optimistic locking
   * @return the updated subscription view
   */
  SubscriptionView updateSubscription(UpdateSubscriptionCommand command);
}
