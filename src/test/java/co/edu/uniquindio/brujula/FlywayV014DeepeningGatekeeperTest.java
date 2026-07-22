package co.edu.uniquindio.brujula;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Asserts the final (post-migration-chain) state of {@code curriculum.sequential_track}'s
 * gatekeeper column:
 *
 * <ul>
 *   <li>The {@code gatekeeper_course_id} column exists (added by V014)
 *   <li>The PERSONAL_ELECTIVE track (Electivas de Formación Personal) has NULL gatekeeper
 *   <li><strong>V028 CORRECTION</strong> (PROP-001-profundizacion-elective-pool, design §0,
 *       2026-07-22, supersedes V014's original seed): the 3 ISC DEEPENING line tracks (Ing.
 *       Software+HCI, Infra T.I., Gestión Información) no longer carry a gatekeeper on the track
 *       itself — each line's prerequisite is now modeled as a {@code COURSE_PASSED} edge on every
 *       elective belonging to that line (see {@code FlywayV028CurriculumProfundizacionPoolTest}).
 *       V014 originally seeded a non-null, distinct gatekeeper per track; V028 relocates that logic
 *       and NULLs the 3 columns.
 * </ul>
 */
@SpringBootTest
class FlywayV014DeepeningGatekeeperTest extends AbstractPostgresIntegrationTest {

  @Autowired private JdbcTemplate jdbc;

  private static final String PROGRAM_NAME = "Ingeniería de Sistemas y Computación";

  // ---------------------------------------------------------------------------
  // Schema: gatekeeper_course_id column must exist on sequential_track
  // ---------------------------------------------------------------------------

  @Test
  void sequential_track_has_gatekeeper_course_id_column() {
    Integer count =
        jdbc.queryForObject(
            """
            SELECT COUNT(*)
            FROM information_schema.columns
            WHERE table_schema = 'curriculum'
              AND table_name   = 'sequential_track'
              AND column_name  = 'gatekeeper_course_id'
            """,
            Integer.class);

    assertThat(count)
        .as("curriculum.sequential_track must have gatekeeper_course_id column (V014 ALTER TABLE)")
        .isEqualTo(1);
  }

  // ---------------------------------------------------------------------------
  // V028 CORRECTION: the 3 ISC DEEPENING line tracks now have NULL gatekeeper
  // (relocated onto per-elective COURSE_PASSED edges — design §0, supersedes V014's seed)
  // ---------------------------------------------------------------------------

  @Test
  void deepening_track_software_hci_gatekeeper_is_null_after_v028_relocation() {
    assertTrackGatekeeperIsNull("%Ingeniería del Software y HCI%");
  }

  @Test
  void deepening_track_infra_ti_gatekeeper_is_null_after_v028_relocation() {
    assertTrackGatekeeperIsNull("%Infraestructura de T.I.%");
  }

  @Test
  void deepening_track_gestion_informacion_gatekeeper_is_null_after_v028_relocation() {
    assertTrackGatekeeperIsNull("%Gestión de la Información%");
  }

  private void assertTrackGatekeeperIsNull(String labelPattern) {
    UUID gatekeeperCourseId =
        jdbc.queryForObject(
            """
            SELECT st.gatekeeper_course_id
            FROM curriculum.sequential_track st
            JOIN catalog.study_plan sp ON st.plan_id = sp.id
            JOIN catalog.academic_program ap ON sp.program_id = ap.id
            WHERE ap.name = ?
              AND st.kind = 'DEEPENING'
              AND st.label LIKE ?
            """,
            (rs, n) -> {
              String id = rs.getString("gatekeeper_course_id");
              return id != null ? UUID.fromString(id) : null;
            },
            PROGRAM_NAME,
            labelPattern);

    assertThat(gatekeeperCourseId)
        .as(
            "V028 must NULL the gatekeeper for DEEPENING track matching '%s' (relocated onto"
                + " per-elective COURSE_PASSED edges)".formatted(labelPattern))
        .isNull();
  }

  // ---------------------------------------------------------------------------
  // All 3 ISC DEEPENING tracks must have NULL gatekeeper after V028 relocation
  // ---------------------------------------------------------------------------

  @Test
  void three_isc_deepening_tracks_have_null_gatekeeper_after_v028_relocation() {
    Integer nonNullCount =
        jdbc.queryForObject(
            """
            SELECT COUNT(*)
            FROM curriculum.sequential_track st
            JOIN catalog.study_plan sp ON st.plan_id = sp.id
            JOIN catalog.academic_program ap ON sp.program_id = ap.id
            WHERE ap.name = ?
              AND st.kind = 'DEEPENING'
              AND st.gatekeeper_course_id IS NOT NULL
            """,
            Integer.class,
            PROGRAM_NAME);

    assertThat(nonNullCount)
        .as(
            "All 3 ISC DEEPENING tracks must have NULL gatekeeper_course_id after V028"
                + " (relocated onto per-elective edges, design §0 CORRECTION)")
        .isEqualTo(0);
  }

  // ---------------------------------------------------------------------------
  // PERSONAL_ELECTIVE track must have NULL gatekeeper
  // ---------------------------------------------------------------------------

  @Test
  void personal_elective_track_has_null_gatekeeper() {
    Integer nullCount =
        jdbc.queryForObject(
            """
            SELECT COUNT(*)
            FROM curriculum.sequential_track st
            JOIN catalog.study_plan sp ON st.plan_id = sp.id
            JOIN catalog.academic_program ap ON sp.program_id = ap.id
            WHERE ap.name = ?
              AND st.kind = 'PERSONAL_ELECTIVE'
              AND st.gatekeeper_course_id IS NULL
            """,
            Integer.class,
            PROGRAM_NAME);

    assertThat(nullCount)
        .as("PERSONAL_ELECTIVE tracks must have NULL gatekeeper_course_id (no gatekeeper applies)")
        .isGreaterThanOrEqualTo(1);
  }
}
