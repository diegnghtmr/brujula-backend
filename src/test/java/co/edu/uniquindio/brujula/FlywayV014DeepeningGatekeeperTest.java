package co.edu.uniquindio.brujula;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Asserts that after V014 Flyway migration has run:
 *
 * <ul>
 *   <li>The {@code gatekeeper_course_id} column exists on {@code curriculum.sequential_track}
 *   <li>The 3 ISC DEEPENING tracks each point to their correct per-line gatekeeper course (Ing.
 *       Software+HCI → IS I; Infra TI → Admin Infra TI; Gestión Información → BD I)
 *   <li>The PERSONAL_ELECTIVE track (Electivas de Formación Personal) has NULL gatekeeper
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
  // Gatekeeper: Línea Ingeniería del Software y HCI → Ingeniería de Software I
  // ---------------------------------------------------------------------------

  @Test
  void deepening_track_software_hci_gatekeeper_is_ingenieria_de_software_i() {
    String gatekeeperName =
        jdbc.queryForObject(
            """
            SELECT c.name
            FROM curriculum.sequential_track st
            JOIN curriculum.course c ON st.gatekeeper_course_id = c.id
            JOIN catalog.study_plan sp ON st.plan_id = sp.id
            JOIN catalog.academic_program ap ON sp.program_id = ap.id
            WHERE ap.name = ?
              AND st.kind = 'DEEPENING'
              AND st.label LIKE '%Ingeniería del Software y HCI%'
            """,
            String.class, PROGRAM_NAME);

    assertThat(gatekeeperName)
        .as(
            "Línea Ingeniería del Software y HCI gatekeeper must be 'Ingeniería de Software I' (PDF oficial)")
        .isEqualTo("Ingeniería de Software I");
  }

  // ---------------------------------------------------------------------------
  // Gatekeeper: Línea Infraestructura de T.I. → Administración de Infraestructura de TI
  // ---------------------------------------------------------------------------

  @Test
  void deepening_track_infra_ti_gatekeeper_is_administracion_de_infraestructura_de_ti() {
    String gatekeeperName =
        jdbc.queryForObject(
            """
            SELECT c.name
            FROM curriculum.sequential_track st
            JOIN curriculum.course c ON st.gatekeeper_course_id = c.id
            JOIN catalog.study_plan sp ON st.plan_id = sp.id
            JOIN catalog.academic_program ap ON sp.program_id = ap.id
            WHERE ap.name = ?
              AND st.kind = 'DEEPENING'
              AND st.label LIKE '%Infraestructura de T.I.%'
            """,
            String.class, PROGRAM_NAME);

    assertThat(gatekeeperName)
        .as(
            "Línea Infraestructura de T.I. gatekeeper must be 'Administración de Infraestructura de TI' (PDF oficial)")
        .isEqualTo("Administración de Infraestructura de TI");
  }

  // ---------------------------------------------------------------------------
  // Gatekeeper: Línea Gestión de la Información y el Conocimiento → Bases de Datos I
  // ---------------------------------------------------------------------------

  @Test
  void deepening_track_gestion_informacion_gatekeeper_is_bases_de_datos_i() {
    String gatekeeperName =
        jdbc.queryForObject(
            """
            SELECT c.name
            FROM curriculum.sequential_track st
            JOIN curriculum.course c ON st.gatekeeper_course_id = c.id
            JOIN catalog.study_plan sp ON st.plan_id = sp.id
            JOIN catalog.academic_program ap ON sp.program_id = ap.id
            WHERE ap.name = ?
              AND st.kind = 'DEEPENING'
              AND st.label LIKE '%Gestión de la Información%'
            """,
            String.class, PROGRAM_NAME);

    assertThat(gatekeeperName)
        .as("Línea Gestión de la Información gatekeeper must be 'Bases de Datos I' (PDF oficial)")
        .isEqualTo("Bases de Datos I");
  }

  // ---------------------------------------------------------------------------
  // Each of the 3 ISC DEEPENING tracks must have a DISTINCT gatekeeper
  // ---------------------------------------------------------------------------

  @Test
  void three_isc_deepening_tracks_have_distinct_gatekeepers() {
    List<Map<String, Object>> rows =
        jdbc.queryForList(
            """
            SELECT st.label, c.name AS gatekeeper_name, c.official_code AS gatekeeper_code
            FROM curriculum.sequential_track st
            JOIN curriculum.course c ON st.gatekeeper_course_id = c.id
            JOIN catalog.study_plan sp ON st.plan_id = sp.id
            JOIN catalog.academic_program ap ON sp.program_id = ap.id
            WHERE ap.name = ?
              AND st.kind = 'DEEPENING'
            ORDER BY st.label
            """,
            PROGRAM_NAME);

    assertThat(rows)
        .as(
            "All 3 ISC DEEPENING tracks must have a non-null gatekeeper (gatekeeper_course_id IS NOT NULL)")
        .hasSize(3);

    List<Object> gatekeeperNames = rows.stream().map(r -> r.get("gatekeeper_name")).toList();
    assertThat(gatekeeperNames)
        .as("Each of the 3 ISC DEEPENING tracks must have a DISTINCT gatekeeper course")
        .doesNotHaveDuplicates();
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
