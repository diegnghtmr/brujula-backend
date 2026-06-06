package co.edu.uniquindio.brujula.audit.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link AuditEvent} domain aggregate (TDD RED phase).
 *
 * <p>Validates immutability, factory invariants, and append-only contract.
 */
class AuditEventTest {

  private final UUID actorId = UUID.randomUUID();

  @Test
  void record_creates_audit_event_with_all_required_fields() {
    AuditEvent event =
        AuditEvent.record(actorId, "ADMIN", "STAFF_CREATED", "app_user", null, null, null, null);

    assertThat(event.getId()).isNotNull();
    assertThat(event.getActorId()).isEqualTo(actorId);
    assertThat(event.getActorRole()).isEqualTo("ADMIN");
    assertThat(event.getAction()).isEqualTo("STAFF_CREATED");
    assertThat(event.getEntityType()).isEqualTo("app_user");
    assertThat(event.getOccurredAt()).isNotNull();
  }

  @Test
  void record_with_optional_fields_sets_them_correctly() {
    UUID entityId = UUID.randomUUID();
    UUID programId = UUID.randomUUID();
    String previousValue = "{\"status\":\"ACTIVE\"}";
    String newValue = "{\"status\":\"INACTIVE\"}";

    AuditEvent event =
        AuditEvent.record(
            actorId,
            "STAFF",
            "STAFF_DEACTIVATED",
            "app_user",
            entityId,
            programId,
            previousValue,
            newValue);

    assertThat(event.getEntityId()).isEqualTo(entityId);
    assertThat(event.getProgramId()).isEqualTo(programId);
    assertThat(event.getPreviousValue()).isEqualTo(previousValue);
    assertThat(event.getNewValue()).isEqualTo(newValue);
  }

  @Test
  void record_rejects_null_actorId() {
    assertThatThrownBy(
            () ->
                AuditEvent.record(
                    null, "ADMIN", "STAFF_CREATED", "app_user", null, null, null, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("actorId");
  }

  @Test
  void record_rejects_null_actorRole() {
    assertThatThrownBy(
            () ->
                AuditEvent.record(
                    actorId, null, "STAFF_CREATED", "app_user", null, null, null, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("actorRole");
  }

  @Test
  void record_rejects_blank_actorRole() {
    assertThatThrownBy(
            () ->
                AuditEvent.record(
                    actorId, "   ", "STAFF_CREATED", "app_user", null, null, null, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("actorRole");
  }

  @Test
  void record_rejects_null_action() {
    assertThatThrownBy(
            () -> AuditEvent.record(actorId, "ADMIN", null, "app_user", null, null, null, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("action");
  }

  @Test
  void record_rejects_null_entityType() {
    assertThatThrownBy(
            () ->
                AuditEvent.record(actorId, "ADMIN", "STAFF_CREATED", null, null, null, null, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("entityType");
  }

  @Test
  void reconstitute_restores_all_fields_from_db() {
    UUID id = UUID.randomUUID();
    UUID entityId = UUID.randomUUID();
    OffsetDateTime occurredAt = OffsetDateTime.now().minusHours(1);

    AuditEvent event =
        AuditEvent.reconstitute(
            id, actorId, "ADMIN", "LOGIN", "session", entityId, null, occurredAt, null, null);

    assertThat(event.getId()).isEqualTo(id);
    assertThat(event.getActorId()).isEqualTo(actorId);
    assertThat(event.getOccurredAt()).isEqualTo(occurredAt);
  }

  @Test
  void audit_event_is_immutable_no_setters_exposed() throws Exception {
    // Verify the class has no public setter methods (append-only invariant)
    long setterCount =
        java.util.Arrays.stream(AuditEvent.class.getMethods())
            .filter(m -> m.getName().startsWith("set"))
            .count();
    assertThat(setterCount)
        .as("AuditEvent must be immutable — no public setters allowed")
        .isEqualTo(0);
  }

  @Test
  void two_events_with_same_data_have_different_ids() {
    AuditEvent e1 = AuditEvent.record(actorId, "ADMIN", "LOGIN", "session", null, null, null, null);
    AuditEvent e2 = AuditEvent.record(actorId, "ADMIN", "LOGIN", "session", null, null, null, null);
    assertThat(e1.getId()).isNotEqualTo(e2.getId());
  }
}
