package co.edu.uniquindio.brujula;

import static org.assertj.core.api.Assertions.assertThat;

import co.edu.uniquindio.brujula.simulation.application.port.out.SaveSimulationPort;
import co.edu.uniquindio.brujula.simulation.application.port.out.SaveSimulationSelectionPort;
import co.edu.uniquindio.brujula.simulation.domain.CandidateSelection;
import co.edu.uniquindio.brujula.simulation.domain.Simulation;
import co.edu.uniquindio.brujula.simulation.domain.SimulationId;
import co.edu.uniquindio.brujula.simulation.domain.SimulationScopeEntry;
import co.edu.uniquindio.brujula.simulation.domain.SimulationSelection;
import co.edu.uniquindio.brujula.simulation.domain.SimulationSelectionId;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * RED -> GREEN test for V030__backfill_simulation_selection_electives.sql.
 *
 * <p>Change: PROP-001-profundizacion-elective-pool, PR1b. Source of truth: design D6, §9, §11 R1.
 *
 * <p>Existing {@code simulation.simulation_selection} rows persisted before V029 store {@code
 * course_id} pointing at one of the two shared slot ids. V030 remaps them using the SAME normalized
 * {@code course_name} mapping as V029 (kept in sync — see V029's header comment). Unlike V029, an
 * unmatched row is NOT a migration failure: it is left on its original shared slot id as a
 * documented legacy/unknown fallback (task 2.2 / tasks.md PR1b).
 *
 * <p>The real production database has zero {@code simulation_selection} rows at migration time
 * (V018 header comment), so this test seeds representative legacy rows itself via the real
 * persistence ports, then re-applies the actual V030 migration SQL (read from the classpath
 * migration resource, not re-implemented) against that freshly-seeded data — proving the shipped
 * script's UPDATE logic end-to-end.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Transactional
class SimulationSelectionBackfillV030IT extends AbstractPostgresIntegrationTest {

  @Autowired private JdbcTemplate jdbc;
  @Autowired private SaveSimulationPort saveSimulationPort;
  @Autowired private SaveSimulationSelectionPort saveSimulationSelectionPort;

  @PersistenceContext private EntityManager entityManager;

  // --- V012 shared slot ids (superseded target of the backfill) ---
  private static final UUID PROFUNDIZACION_I_SLOT =
      UUID.fromString("c0080000-0000-4000-a000-000000000004");
  private static final UUID PROFUNDIZACION_III_SLOT =
      UUID.fromString("c0090000-0000-4000-a000-000000000004");

  // --- V028 elective ids used by this test (design §3 table) ---
  private static final UUID COMPUTACION_NUBE =
      UUID.fromString("c0dee000-0000-4000-a000-000000000004");
  private static final UUID VISION_ARTIFICIAL =
      UUID.fromString("c0dee000-0000-4000-a000-000000000007");

  // ---------------------------------------------------------------------------
  // Helpers
  // ---------------------------------------------------------------------------

  private UUID seedSimulation() {
    Simulation sim =
        Simulation.create(
            SimulationId.generate(),
            UUID.randomUUID(),
            "V030 backfill fixture",
            UUID.randomUUID(),
            List.of(new SimulationScopeEntry(UUID.randomUUID(), "DIURNA")));
    return saveSimulationPort.save(sim).getId().value();
  }

  private UUID seedLegacySelection(UUID simulationId, UUID legacyCourseId, String courseName) {
    SimulationSelectionId id = SimulationSelectionId.generate();
    CandidateSelection candidate =
        new CandidateSelection(
            UUID.randomUUID(),
            UUID.randomUUID(),
            legacyCourseId,
            courseName,
            UUID.randomUUID(),
            3.0,
            "DIURNA",
            "FINAL",
            0L,
            List.of());
    SimulationSelection selection = SimulationSelection.create(id, simulationId, candidate);
    saveSimulationSelectionPort.save(selection);
    return id.value();
  }

  /** Re-applies the real, already-committed V030 migration script against freshly seeded data. */
  private void runV030Migration() throws IOException {
    String sql =
        new ClassPathResource("db/migration/V030__backfill_simulation_selection_electives.sql")
            .getContentAsString(StandardCharsets.UTF_8);
    entityManager.flush();
    entityManager.clear();
    jdbc.execute(sql);
    entityManager.clear();
  }

  private UUID currentCourseId(UUID selectionId) {
    return jdbc.queryForObject(
        "SELECT course_id FROM simulation.simulation_selection WHERE id = ?::uuid",
        UUID.class,
        selectionId);
  }

  // ---------------------------------------------------------------------------
  // Matched legacy selections remap to the correct elective id
  // ---------------------------------------------------------------------------

  @Test
  void matched_legacy_selections_remap_to_the_correct_elective_id() throws IOException {
    UUID simulationId = seedSimulation();
    UUID sincronicoSelectionId =
        seedLegacySelection(
            simulationId,
            PROFUNDIZACION_I_SLOT,
            "Profundización Computación en la Nube (Sincrónico)");
    UUID dashPrefixSelectionId =
        seedLegacySelection(
            simulationId, PROFUNDIZACION_III_SLOT, "Profundización - Visión Artificial");

    runV030Migration();

    assertThat(currentCourseId(sincronicoSelectionId))
        .as("'(Sincrónico)' variant on the shared slot id must remap to its base elective")
        .isEqualTo(COMPUTACION_NUBE);
    assertThat(currentCourseId(dashPrefixSelectionId))
        .as("'Profundización - ' dash-prefix variant must remap to its elective")
        .isEqualTo(VISION_ARTIFICIAL);
  }

  // ---------------------------------------------------------------------------
  // Unmatched legacy selection keeps the shared slot id (legacy/unknown fallback)
  // ---------------------------------------------------------------------------

  @Test
  void unmatched_legacy_selection_keeps_shared_slot_id_as_legacy_fallback() throws IOException {
    UUID simulationId = seedSimulation();
    UUID unmatchedSelectionId =
        seedLegacySelection(
            simulationId, PROFUNDIZACION_I_SLOT, "Profundización de Curso Inexistente En El Pool");

    runV030Migration();

    assertThat(currentCourseId(unmatchedSelectionId))
        .as(
            "an unrecognized course_name must NOT fail the migration -- it stays on the shared"
                + " slot id as a documented legacy/unknown fallback")
        .isEqualTo(PROFUNDIZACION_I_SLOT);
  }

  // ---------------------------------------------------------------------------
  // Re-running the migration is idempotent: already-remapped rows are left untouched
  // ---------------------------------------------------------------------------

  @Test
  void reapplying_the_migration_is_idempotent() throws IOException {
    UUID simulationId = seedSimulation();
    UUID selectionId =
        seedLegacySelection(
            simulationId, PROFUNDIZACION_I_SLOT, "Profundización Visión Artificial");

    runV030Migration();
    UUID afterFirstRun = currentCourseId(selectionId);
    runV030Migration();
    UUID afterSecondRun = currentCourseId(selectionId);

    assertThat(afterFirstRun).isEqualTo(VISION_ARTIFICIAL);
    assertThat(afterSecondRun).isEqualTo(afterFirstRun);
  }
}
