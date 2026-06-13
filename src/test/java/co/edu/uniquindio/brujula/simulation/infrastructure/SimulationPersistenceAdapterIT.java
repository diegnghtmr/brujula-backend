package co.edu.uniquindio.brujula.simulation.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import co.edu.uniquindio.brujula.AbstractPostgresIntegrationTest;
import co.edu.uniquindio.brujula.simulation.application.port.out.LoadSimulationPort;
import co.edu.uniquindio.brujula.simulation.application.port.out.SaveSimulationPort;
import co.edu.uniquindio.brujula.simulation.domain.Simulation;
import co.edu.uniquindio.brujula.simulation.domain.SimulationId;
import co.edu.uniquindio.brujula.simulation.domain.SimulationLifecycle;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for SimulationPersistenceAdapter.
 *
 * <p>Verifies against Flyway V016+V017+V018 migrated PostgreSQL (Testcontainers). Tests: save/load,
 * soft-delete, list-by-owner, uuid[] program_ids round-trip.
 */
@SpringBootTest
@Transactional
class SimulationPersistenceAdapterIT extends AbstractPostgresIntegrationTest {

  @Autowired private LoadSimulationPort loadSimulationPort;
  @Autowired private SaveSimulationPort saveSimulationPort;

  private final UUID ownerId = UUID.randomUUID();
  private final UUID periodId = UUID.randomUUID();
  private final UUID programId1 = UUID.randomUUID();
  private final UUID programId2 = UUID.randomUUID();

  @Test
  void save_and_load_by_id() {
    Simulation sim =
        Simulation.create(
            SimulationId.generate(), ownerId, "IT Simulation", periodId, List.of(programId1));
    Simulation saved = saveSimulationPort.save(sim);

    Optional<Simulation> loaded = loadSimulationPort.loadById(saved.getId().value());
    assertThat(loaded).isPresent();
    assertThat(loaded.get().getName()).isEqualTo("IT Simulation");
    assertThat(loaded.get().getOwnerId()).isEqualTo(ownerId);
    assertThat(loaded.get().getPeriodId()).isEqualTo(periodId);
    assertThat(loaded.get().getProgramIds()).containsExactly(programId1);
    assertThat(loaded.get().getLifecycleStatus()).isEqualTo(SimulationLifecycle.ACTIVE);
  }

  @Test
  void save_with_two_program_ids_round_trips() {
    Simulation sim =
        Simulation.create(
            SimulationId.generate(),
            ownerId,
            "Two Programs",
            periodId,
            List.of(programId1, programId2));
    Simulation saved = saveSimulationPort.save(sim);

    Optional<Simulation> loaded = loadSimulationPort.loadById(saved.getId().value());
    assertThat(loaded).isPresent();
    assertThat(loaded.get().getProgramIds()).containsExactlyInAnyOrder(programId1, programId2);
  }

  @Test
  void list_by_owner_returns_only_active() {
    // Create two simulations for owner
    Simulation s1 =
        Simulation.create(SimulationId.generate(), ownerId, "Sim A", periodId, List.of(programId1));
    Simulation s2 =
        Simulation.create(SimulationId.generate(), ownerId, "Sim B", periodId, List.of(programId2));
    Simulation s3 =
        Simulation.create(
            SimulationId.generate(),
            UUID.randomUUID(),
            "Other owner",
            periodId,
            List.of(programId1));
    saveSimulationPort.save(s1);
    saveSimulationPort.save(s2);
    saveSimulationPort.save(s3);

    List<Simulation> found = loadSimulationPort.loadByOwner(ownerId, null, 0, 10);
    assertThat(found).hasSize(2);
    assertThat(found).allSatisfy(s -> assertThat(s.getOwnerId()).isEqualTo(ownerId));
  }

  @Test
  void soft_delete_hides_simulation_from_list() {
    Simulation sim =
        Simulation.create(
            SimulationId.generate(), ownerId, "To Delete", periodId, List.of(programId1));
    Simulation saved = saveSimulationPort.save(sim);

    // Soft-delete
    Simulation loaded = loadSimulationPort.loadById(saved.getId().value()).orElseThrow();
    loaded.markDeleted();
    saveSimulationPort.save(loaded);

    List<Simulation> active = loadSimulationPort.loadByOwner(ownerId, null, 0, 10);
    assertThat(active).noneMatch(s -> s.getId().equals(saved.getId()));
  }

  @Test
  void list_by_owner_with_period_filter() {
    UUID otherPeriodId = UUID.randomUUID();
    Simulation s1 =
        Simulation.create(
            SimulationId.generate(), ownerId, "Period Match", periodId, List.of(programId1));
    Simulation s2 =
        Simulation.create(
            SimulationId.generate(), ownerId, "Other Period", otherPeriodId, List.of(programId1));
    saveSimulationPort.save(s1);
    saveSimulationPort.save(s2);

    List<Simulation> found = loadSimulationPort.loadByOwner(ownerId, periodId, 0, 10);
    assertThat(found).hasSize(1);
    assertThat(found.get(0).getPeriodId()).isEqualTo(periodId);
  }

  @Test
  void count_by_owner() {
    saveSimulationPort.save(
        Simulation.create(
            SimulationId.generate(), ownerId, "Sim 1", periodId, List.of(programId1)));
    saveSimulationPort.save(
        Simulation.create(
            SimulationId.generate(), ownerId, "Sim 2", periodId, List.of(programId1)));

    long count = loadSimulationPort.countByOwner(ownerId, null);
    assertThat(count).isEqualTo(2);
  }
}
