package co.edu.uniquindio.brujula.simulation.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Domain unit tests for {@link Simulation} with scope-based invariants (INV-002-v2).
 *
 * <p>Covers: 5 valid scopes, violations (3 distinct programs, duplicate pair, empty, >4), and the
 * derived {@code programIds()} accessor.
 */
class SimulationScopeTest {

  private final UUID ownerId = UUID.randomUUID();
  private final UUID periodId = UUID.randomUUID();
  private final UUID p1 = UUID.randomUUID();
  private final UUID p2 = UUID.randomUUID();

  // -------------------------------------------------------------------------
  // Valid scopes
  // -------------------------------------------------------------------------

  @Test
  void scope_1_program_1_journey_is_valid() {
    var entries = List.of(new SimulationScopeEntry(p1, "DIURNA"));
    Simulation sim = Simulation.create(SimulationId.generate(), ownerId, "Sim", periodId, entries);
    assertThat(sim.getScopeEntries()).hasSize(1);
    assertThat(sim.programIds()).containsExactly(p1);
  }

  @Test
  void scope_1_program_2_journeys_is_valid() {
    var entries =
        List.of(new SimulationScopeEntry(p1, "DIURNA"), new SimulationScopeEntry(p1, "NOCTURNA"));
    Simulation sim = Simulation.create(SimulationId.generate(), ownerId, "Sim", periodId, entries);
    assertThat(sim.getScopeEntries()).hasSize(2);
    assertThat(sim.programIds()).containsExactly(p1);
  }

  @Test
  void scope_2_programs_1_journey_each_is_valid() {
    var entries =
        List.of(new SimulationScopeEntry(p1, "DIURNA"), new SimulationScopeEntry(p2, "DIURNA"));
    Simulation sim = Simulation.create(SimulationId.generate(), ownerId, "Sim", periodId, entries);
    assertThat(sim.getScopeEntries()).hasSize(2);
    assertThat(sim.programIds()).containsExactlyInAnyOrder(p1, p2);
  }

  @Test
  void scope_2_programs_one_with_2_journeys_is_valid() {
    var entries =
        List.of(
            new SimulationScopeEntry(p1, "DIURNA"),
            new SimulationScopeEntry(p1, "NOCTURNA"),
            new SimulationScopeEntry(p2, "DIURNA"));
    Simulation sim = Simulation.create(SimulationId.generate(), ownerId, "Sim", periodId, entries);
    assertThat(sim.getScopeEntries()).hasSize(3);
    assertThat(sim.programIds()).containsExactlyInAnyOrder(p1, p2);
  }

  @Test
  void scope_2_programs_2_journeys_each_is_valid_max_4() {
    var entries =
        List.of(
            new SimulationScopeEntry(p1, "DIURNA"),
            new SimulationScopeEntry(p1, "NOCTURNA"),
            new SimulationScopeEntry(p2, "DIURNA"),
            new SimulationScopeEntry(p2, "NOCTURNA"));
    Simulation sim = Simulation.create(SimulationId.generate(), ownerId, "Sim", periodId, entries);
    assertThat(sim.getScopeEntries()).hasSize(4);
    assertThat(sim.programIds()).containsExactlyInAnyOrder(p1, p2);
  }

  // -------------------------------------------------------------------------
  // Violations
  // -------------------------------------------------------------------------

  @Test
  void three_distinct_programs_throws() {
    UUID p3 = UUID.randomUUID();
    var entries =
        List.of(
            new SimulationScopeEntry(p1, "DIURNA"),
            new SimulationScopeEntry(p2, "DIURNA"),
            new SimulationScopeEntry(p3, "DIURNA"));
    assertThatThrownBy(
            () -> Simulation.create(SimulationId.generate(), ownerId, "Bad", periodId, entries))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("2 distinct programs");
  }

  @Test
  void duplicate_program_journey_pair_throws() {
    var entries =
        List.of(new SimulationScopeEntry(p1, "DIURNA"), new SimulationScopeEntry(p1, "DIURNA"));
    assertThatThrownBy(
            () -> Simulation.create(SimulationId.generate(), ownerId, "Dup", periodId, entries))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("unique");
  }

  @Test
  void empty_scope_entries_throws() {
    assertThatThrownBy(
            () -> Simulation.create(SimulationId.generate(), ownerId, "Empty", periodId, List.of()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("at least 1");
  }

  @Test
  void more_than_4_entries_throws() {
    UUID p3 = UUID.randomUUID();
    var entries =
        List.of(
            new SimulationScopeEntry(p1, "DIURNA"),
            new SimulationScopeEntry(p1, "NOCTURNA"),
            new SimulationScopeEntry(p2, "DIURNA"),
            new SimulationScopeEntry(p2, "NOCTURNA"),
            new SimulationScopeEntry(p3, "DIURNA"));
    assertThatThrownBy(
            () -> Simulation.create(SimulationId.generate(), ownerId, "TooMany", periodId, entries))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("4");
  }

  // -------------------------------------------------------------------------
  // Derived programIds()
  // -------------------------------------------------------------------------

  @Test
  void programIds_returns_distinct_program_ids() {
    var entries =
        List.of(
            new SimulationScopeEntry(p1, "DIURNA"),
            new SimulationScopeEntry(p1, "NOCTURNA"),
            new SimulationScopeEntry(p2, "DIURNA"));
    Simulation sim = Simulation.create(SimulationId.generate(), ownerId, "Sim", periodId, entries);
    List<UUID> programIds = sim.programIds();
    assertThat(programIds).containsExactlyInAnyOrder(p1, p2);
    assertThat(programIds).doesNotHaveDuplicates();
  }

  // -------------------------------------------------------------------------
  // SimulationScopeEntry VO validation
  // -------------------------------------------------------------------------

  @Test
  void scope_entry_with_blank_journey_throws() {
    assertThatThrownBy(() -> new SimulationScopeEntry(p1, "  "))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("blank");
  }

  @Test
  void scope_entry_with_null_program_id_throws() {
    assertThatThrownBy(() -> new SimulationScopeEntry(null, "DIURNA"))
        .isInstanceOf(NullPointerException.class);
  }
}
