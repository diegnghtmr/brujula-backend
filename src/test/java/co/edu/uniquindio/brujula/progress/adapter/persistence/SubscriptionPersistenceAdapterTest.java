package co.edu.uniquindio.brujula.progress.adapter.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import co.edu.uniquindio.brujula.AbstractPostgresIntegrationTest;
import co.edu.uniquindio.brujula.progress.application.port.out.LoadSubscriptionPort;
import co.edu.uniquindio.brujula.progress.application.port.out.SaveSubscriptionPort;
import co.edu.uniquindio.brujula.progress.domain.Subscription;
import co.edu.uniquindio.brujula.progress.domain.SubscriptionId;
import co.edu.uniquindio.brujula.progress.domain.SubscriptionStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * WU2-T07 RED: persistence adapter integration tests for subscriptions.
 *
 * <p>Uses {@link AbstractPostgresIntegrationTest} ({@code @SpringBootTest}). Validates save, load,
 * soft-delete, and active-count queries against the real V015 schema.
 */
@SpringBootTest
class SubscriptionPersistenceAdapterTest extends AbstractPostgresIntegrationTest {

  @Autowired private SaveSubscriptionPort saveSubscriptionPort;
  @Autowired private LoadSubscriptionPort loadSubscriptionPort;

  private final UUID userId = UUID.randomUUID();
  private final UUID programId = UUID.randomUUID();
  private final UUID planId = UUID.randomUUID();

  @Test
  void save_and_load_subscription_round_trip() {
    SubscriptionId id = SubscriptionId.generate();
    Subscription sub = Subscription.create(id, userId, programId, planId, "DIURNA");

    saveSubscriptionPort.save(sub);
    Optional<Subscription> loaded = loadSubscriptionPort.loadById(id.value());

    assertThat(loaded).isPresent();
    assertThat(loaded.get().getId()).isEqualTo(id);
    assertThat(loaded.get().getUserId()).isEqualTo(userId);
    assertThat(loaded.get().getPlanId()).isEqualTo(planId);
    assertThat(loaded.get().getReferenceJourneyCode()).isEqualTo("DIURNA");
    assertThat(loaded.get().getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
  }

  @Test
  void soft_delete_is_persisted() {
    SubscriptionId id = SubscriptionId.generate();
    Subscription sub = Subscription.create(id, userId, programId, planId, "NOCTURNA");
    saveSubscriptionPort.save(sub);

    sub.softDelete();
    saveSubscriptionPort.save(sub);

    Optional<Subscription> loaded = loadSubscriptionPort.loadById(id.value());
    assertThat(loaded).isPresent();
    assertThat(loaded.get().getStatus()).isEqualTo(SubscriptionStatus.DELETED);
  }

  @Test
  void load_active_by_user_returns_only_active_subscriptions() {
    UUID uniqueUserId = UUID.randomUUID();

    SubscriptionId id1 = SubscriptionId.generate();
    SubscriptionId id2 = SubscriptionId.generate();

    Subscription active =
        Subscription.create(id1, uniqueUserId, UUID.randomUUID(), UUID.randomUUID(), "DIURNA");
    Subscription deleted =
        Subscription.create(id2, uniqueUserId, UUID.randomUUID(), UUID.randomUUID(), "NOCTURNA");
    deleted.softDelete();

    saveSubscriptionPort.save(active);
    saveSubscriptionPort.save(deleted);

    List<Subscription> activeList = loadSubscriptionPort.loadActiveByUserId(uniqueUserId);

    assertThat(activeList).hasSize(1);
    assertThat(activeList.get(0).getId()).isEqualTo(id1);
  }

  @Test
  void count_active_by_user_returns_correct_count() {
    UUID uniqueUserId = UUID.randomUUID();

    SubscriptionId id1 = SubscriptionId.generate();
    SubscriptionId id2 = SubscriptionId.generate();

    saveSubscriptionPort.save(
        Subscription.create(id1, uniqueUserId, UUID.randomUUID(), UUID.randomUUID(), "DIURNA"));
    saveSubscriptionPort.save(
        Subscription.create(id2, uniqueUserId, UUID.randomUUID(), UUID.randomUUID(), "NOCTURNA"));

    int count = loadSubscriptionPort.countActiveByUserId(uniqueUserId);

    assertThat(count).isEqualTo(2);
  }

  @Test
  void load_by_id_returns_empty_for_unknown_id() {
    Optional<Subscription> result = loadSubscriptionPort.loadById(UUID.randomUUID());
    assertThat(result).isEmpty();
  }

  // -------------------------------------------------------------------------
  // FIX-W4 RED (S3): save → modify → save → reload asserts version increments to 1
  // -------------------------------------------------------------------------

  @Test
  void save_returns_incremented_version_after_update() {
    SubscriptionId id = SubscriptionId.generate();
    Subscription sub = Subscription.create(id, userId, programId, planId, "DIURNA");
    // Initial save; version should be 0
    Subscription saved = saveSubscriptionPort.save(sub);
    assertThat(saved.getVersion()).isEqualTo(0L);

    // Modify and re-save — JPA @Version must increment to 1
    saved.updateReferenceJourney("NOCTURNA");
    Subscription updated = saveSubscriptionPort.save(saved);
    assertThat(updated.getVersion()).isEqualTo(1L);

    // Reload confirms version is 1 in the database
    Subscription reloaded = loadSubscriptionPort.loadById(id.value()).orElseThrow();
    assertThat(reloaded.getVersion()).isEqualTo(1L);
    assertThat(reloaded.getReferenceJourneyCode()).isEqualTo("NOCTURNA");
  }
}
