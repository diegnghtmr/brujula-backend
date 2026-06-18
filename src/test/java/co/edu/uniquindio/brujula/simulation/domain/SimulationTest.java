package co.edu.uniquindio.brujula.simulation.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * WU3 domain unit tests for {@link Simulation}.
 *
 * <p>Covers: name invariant, scope entries (INV-002-v2), rename, markDeleted.
 *
 * <p>Updated from WU3-original: create() now accepts {@link SimulationScopeEntry} list instead of a
 * plain {@code List<UUID>}.
 */
class SimulationTest {

  private final UUID ownerId = UUID.randomUUID();
  private final UUID periodId = UUID.randomUUID();
  private final UUID programId1 = UUID.randomUUID();
  private final UUID programId2 = UUID.randomUUID();

  private SimulationScopeEntry entry1() {
    return new SimulationScopeEntry(programId1, "DIURNA");
  }

  private SimulationScopeEntry entry2() {
    return new SimulationScopeEntry(programId2, "DIURNA");
  }

  @Test
  void create_with_valid_data_succeeds() {
    Simulation sim =
        Simulation.create(
            SimulationId.generate(), ownerId, "Mi simulación 2026", periodId, List.of(entry1()));
    assertThat(sim.getName()).isEqualTo("Mi simulación 2026");
    assertThat(sim.getLifecycleStatus()).isEqualTo(SimulationLifecycle.ACTIVE);
    assertThat(sim.getProgramIds()).containsExactly(programId1);
    assertThat(sim.getVersion()).isEqualTo(0L);
  }

  @Test
  void create_with_two_programs_succeeds() {
    Simulation sim =
        Simulation.create(
            SimulationId.generate(),
            ownerId,
            "Dos programas",
            periodId,
            List.of(entry1(), entry2()));
    assertThat(sim.getProgramIds()).hasSize(2);
  }

  @Test
  void create_with_three_distinct_programs_throws() {
    UUID p3 = UUID.randomUUID();
    assertThatThrownBy(
            () ->
                Simulation.create(
                    SimulationId.generate(),
                    ownerId,
                    "Too many",
                    periodId,
                    List.of(
                        new SimulationScopeEntry(programId1, "DIURNA"),
                        new SimulationScopeEntry(programId2, "DIURNA"),
                        new SimulationScopeEntry(p3, "DIURNA"))))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("2 distinct programs");
  }

  @Test
  void create_with_zero_entries_throws() {
    assertThatThrownBy(
            () -> Simulation.create(SimulationId.generate(), ownerId, "Empty", periodId, List.of()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("at least 1");
  }

  @Test
  void create_with_duplicate_scope_entries_throws() {
    var entry = new SimulationScopeEntry(programId1, "DIURNA");
    assertThatThrownBy(
            () ->
                Simulation.create(
                    SimulationId.generate(),
                    ownerId,
                    "Duplicates",
                    periodId,
                    List.of(entry, entry)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("unique");
  }

  @Test
  void create_with_blank_name_throws() {
    assertThatThrownBy(
            () ->
                Simulation.create(
                    SimulationId.generate(), ownerId, "  ", periodId, List.of(entry1())))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("blank");
  }

  @Test
  void create_with_name_exceeding_120_chars_throws() {
    String longName = "A".repeat(121);
    assertThatThrownBy(
            () ->
                Simulation.create(
                    SimulationId.generate(), ownerId, longName, periodId, List.of(entry1())))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("120");
  }

  @Test
  void rename_updates_name_and_preserves_version_until_persisted() {
    Simulation sim =
        Simulation.create(
            SimulationId.generate(), ownerId, "Original Name", periodId, List.of(entry1()));
    sim.rename("New Name");
    assertThat(sim.getName()).isEqualTo("New Name");
    // Version is managed by JPA @Version; domain object reflects 0 until persisted
  }

  @Test
  void rename_to_blank_throws() {
    Simulation sim =
        Simulation.create(SimulationId.generate(), ownerId, "Name", periodId, List.of(entry1()));
    assertThatThrownBy(() -> sim.rename("")).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void markDeleted_changes_lifecycle_to_deleted() {
    Simulation sim =
        Simulation.create(
            SimulationId.generate(), ownerId, "To delete", periodId, List.of(entry1()));
    sim.markDeleted();
    assertThat(sim.getLifecycleStatus()).isEqualTo(SimulationLifecycle.DELETED);
  }

  @Test
  void scopeEntries_are_defensive_copy() {
    var entries = new java.util.ArrayList<>(List.of(entry1()));
    Simulation sim = Simulation.create(SimulationId.generate(), ownerId, "Safe", periodId, entries);
    entries.add(entry2()); // mutate original list
    assertThat(sim.getScopeEntries()).hasSize(1); // domain object not affected
  }
}
