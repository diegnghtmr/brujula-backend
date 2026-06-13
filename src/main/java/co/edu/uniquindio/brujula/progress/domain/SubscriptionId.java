package co.edu.uniquindio.brujula.progress.domain;

import java.util.Objects;
import java.util.UUID;

/**
 * Value object: identity of a {@link Subscription} aggregate.
 *
 * <p>No Spring/JPA imports.
 */
public record SubscriptionId(UUID value) {

  public SubscriptionId {
    Objects.requireNonNull(value, "SubscriptionId value must not be null");
  }

  public static SubscriptionId generate() {
    return new SubscriptionId(UUID.randomUUID());
  }

  public static SubscriptionId of(UUID value) {
    return new SubscriptionId(value);
  }
}
