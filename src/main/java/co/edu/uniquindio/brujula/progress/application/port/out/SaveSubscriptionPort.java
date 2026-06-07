package co.edu.uniquindio.brujula.progress.application.port.out;

import co.edu.uniquindio.brujula.progress.domain.Subscription;

/**
 * Output port: write operations for {@link Subscription}.
 *
 * <p>Used for both initial creation and updates (plan change, soft-delete). Optimistic lock
 * enforcement is delegated to JPA {@code @Version} on the entity.
 */
public interface SaveSubscriptionPort {

  /**
   * Persist (insert or update) a subscription.
   *
   * <p>Returns the reconstituted domain object carrying the post-save version (the JPA
   * {@code @Version} field is incremented by the database on every update). Callers MUST use the
   * returned instance instead of the passed-in one to avoid returning a stale version in the
   * response (FIX-W4).
   *
   * @param subscription the domain aggregate to persist
   * @return the reconstituted domain object with the database-assigned (incremented) version
   */
  Subscription save(Subscription subscription);
}
