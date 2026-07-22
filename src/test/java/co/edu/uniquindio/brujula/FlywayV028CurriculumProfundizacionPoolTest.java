package co.edu.uniquindio.brujula;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * RED -> GREEN test for V028__curriculum_profundizacion_pool.sql.
 *
 * <p>Change: PROP-001-profundizacion-elective-pool, PR1a. Source of truth: design §0 CORRECTION
 * (authoritative, 2026-07-22) + design §3/§5.
 *
 * <p>Product-owner ruling: Profundización I-IV are NOT four ordered slots — they are a pool of
 * &gt;= 4 distinct interchangeable electives. Each elective keeps ITS LINE's entry prerequisite
 * (relocated from {@code sequential_track.gatekeeper_course_id} onto a per-elective {@code
 * COURSE_PASSED} edge). The 4 slot->slot / thesis-slot ordering edges seeded in V012 are archived
 * (distinctness + max-4 replaces the ordinal rule).
 *
 * <p>Covers tasks 1.1-1.4 of PR1a:
 *
 * <ul>
 *   <li>1.1 — 8 DEEPENING elective courses exist with stable official_codes
 *   <li>1.2 — the 4 superseded ordering edges (V012:733-746) are ARCHIVED
 *   <li>1.3 — exactly one CONFIGURABLE pool gate on Trabajo de Grado (required_credits=4,
 *       required_course_id=NULL)
 *   <li>1.4 — 8 COURSE_PASSED elective -> line-gatekeeper edges exist; the 3 DEEPENING track
 *       gatekeepers are NULL
 * </ul>
 */
@SpringBootTest
class FlywayV028CurriculumProfundizacionPoolTest extends AbstractPostgresIntegrationTest {

  @Autowired private JdbcTemplate jdbc;

  private static final String PROGRAM_NAME = "Ingeniería de Sistemas y Computación";

  // --- The 8 new elective official_codes (design §3) ---
  private static final List<String> ELECTIVE_OFFICIAL_CODES =
      List.of(
          "ISC-DEEP-DISENOCENTRADOENELUSUARIO",
          "ISC-DEEP-DESARROLLODEAPLICACIONESPARADISPOSITIVOSMOVILES",
          "ISC-DEEP-USABILIDADYEXPERIENCIADEUSUARIO",
          "ISC-DEEP-COMPUTACIONENLANUBE",
          "ISC-DEEP-ARQUITECTURAORIENTADAAMICROSERVICIOS",
          "ISC-DEEP-VISUALIZACIONDEDATOS",
          "ISC-DEEP-VISIONARTIFICIAL",
          "ISC-DEEP-INTRODUCCIONALBIGDATA");

  // --- Line gatekeepers (V012 official_codes) ---
  private static final String INGENIERIA_SOFTWARE_I = "ISC-S5-INGENIERIADESOFTWAREI";
  private static final String ADMIN_INFRAESTRUCTURA_TI =
      "ISC-S7-ADMINISTRACIONDEINFRAESTRUCTURADETI";
  private static final String BASES_DE_DATOS_I = "ISC-S6-BASESDEDATOSI";

  // --- design §0 CORRECTION table: elective official_code -> line gatekeeper official_code ---
  private static final Map<String, String> ELECTIVE_TO_GATEKEEPER =
      Map.of(
          "ISC-DEEP-DISENOCENTRADOENELUSUARIO", INGENIERIA_SOFTWARE_I,
          "ISC-DEEP-DESARROLLODEAPLICACIONESPARADISPOSITIVOSMOVILES", INGENIERIA_SOFTWARE_I,
          "ISC-DEEP-USABILIDADYEXPERIENCIADEUSUARIO", INGENIERIA_SOFTWARE_I,
          "ISC-DEEP-COMPUTACIONENLANUBE", ADMIN_INFRAESTRUCTURA_TI,
          "ISC-DEEP-ARQUITECTURAORIENTADAAMICROSERVICIOS", ADMIN_INFRAESTRUCTURA_TI,
          "ISC-DEEP-VISUALIZACIONDEDATOS", BASES_DE_DATOS_I,
          "ISC-DEEP-VISIONARTIFICIAL", BASES_DE_DATOS_I,
          "ISC-DEEP-INTRODUCCIONALBIGDATA", BASES_DE_DATOS_I);

  // --- V012 slot / thesis course ids (stable seed constants, never change) ---
  private static final String PROFUNDIZACION_I = "c0080000-0000-4000-a000-000000000004";
  private static final String PROFUNDIZACION_II = "c0080000-0000-4000-a000-000000000005";
  private static final String PROFUNDIZACION_III = "c0090000-0000-4000-a000-000000000004";
  private static final String PROFUNDIZACION_IV = "c0090000-0000-4000-a000-000000000005";
  private static final String TRABAJO_DE_GRADO = "c0100000-0000-4000-a000-000000000001";

  // --- V012 DEEPENING sequential-track ids (stable seed constants, never change) ---
  private static final String TRACK_SOFTWARE_HCI = "e0a20000-0000-4000-a000-000000000001";
  private static final String TRACK_INFRA_TI = "e0a30000-0000-4000-a000-000000000001";
  private static final String TRACK_GESTION_INFO = "e0a40000-0000-4000-a000-000000000001";

  // ---------------------------------------------------------------------------
  // 1.1 — 8 DEEPENING electives exist with stable official_codes
  // ---------------------------------------------------------------------------

  @Test
  void eight_deepening_electives_exist_with_stable_official_codes() {
    List<String> codes =
        jdbc.queryForList(
            """
            SELECT c.official_code
            FROM curriculum.course c
            JOIN catalog.study_plan sp ON c.plan_id = sp.id
            JOIN catalog.academic_program ap ON sp.program_id = ap.id
            WHERE ap.name = ?
              AND c.official_code LIKE 'ISC-DEEP-%'
            """,
            String.class, PROGRAM_NAME);

    assertThat(codes)
        .as("V028 must seed exactly the 8 DEEPENING elective official_codes from design §3")
        .containsExactlyInAnyOrderElementsOf(ELECTIVE_OFFICIAL_CODES);
  }

  @Test
  void eight_deepening_electives_have_type_deepening_3_credits_and_active_status() {
    Integer mismatchCount =
        jdbc.queryForObject(
            """
            SELECT COUNT(*)
            FROM curriculum.course c
            JOIN catalog.study_plan sp ON c.plan_id = sp.id
            JOIN catalog.academic_program ap ON sp.program_id = ap.id
            WHERE ap.name = ?
              AND c.official_code LIKE 'ISC-DEEP-%'
              AND (c.type != 'DEEPENING' OR c.credits != 3 OR c.status != 'ACTIVE')
            """,
            Integer.class, PROGRAM_NAME);

    assertThat(mismatchCount)
        .as("All 8 new electives must be type=DEEPENING, credits=3, status=ACTIVE")
        .isEqualTo(0);
  }

  @Test
  void deepening_electives_have_no_recommended_semester_rows() {
    // design D2: no course_recommended_semester rows -> malla assembler does not render them
    Integer count =
        jdbc.queryForObject(
            """
            SELECT COUNT(*)
            FROM curriculum.course_recommended_semester crs
            JOIN curriculum.course c ON crs.course_id = c.id
            WHERE c.official_code LIKE 'ISC-DEEP-%'
            """,
            Integer.class);

    assertThat(count)
        .as("The 8 pool electives must have NO course_recommended_semester rows (design D2)")
        .isEqualTo(0);
  }

  // ---------------------------------------------------------------------------
  // 1.2 — the 4 superseded ordering edges (V012:733-746) are ARCHIVED
  // ---------------------------------------------------------------------------

  @Test
  void slot_to_slot_and_thesis_slot_ordering_edges_are_archived() {
    record EdgeKey(String target, String required) {}

    List<EdgeKey> archivedEdges =
        List.of(
            new EdgeKey(PROFUNDIZACION_III, PROFUNDIZACION_I),
            new EdgeKey(PROFUNDIZACION_IV, PROFUNDIZACION_II),
            new EdgeKey(TRABAJO_DE_GRADO, PROFUNDIZACION_III),
            new EdgeKey(TRABAJO_DE_GRADO, PROFUNDIZACION_IV));

    for (EdgeKey edge : archivedEdges) {
      String status =
          jdbc.queryForObject(
              """
              SELECT status
              FROM curriculum.course_requirement
              WHERE target_course_id = ?::uuid
                AND required_course_id = ?::uuid
                AND type = 'COURSE_PASSED'
              """,
              String.class,
              edge.target(),
              edge.required());

      assertThat(status)
          .as(
              "V028 must archive the superseded ordering edge target=%s required=%s"
                  .formatted(edge.target(), edge.required()))
          .isEqualTo("ARCHIVED");
    }
  }

  // ---------------------------------------------------------------------------
  // 1.3 — exactly one CONFIGURABLE pool gate on Trabajo de Grado
  // ---------------------------------------------------------------------------

  @Test
  void trabajo_de_grado_has_exactly_one_configurable_pool_gate() {
    record ConfigurableRow(UUID requiredCourseId, Integer requiredCredits) {}

    List<ConfigurableRow> rows =
        jdbc.query(
            """
            SELECT required_course_id, required_credits
            FROM curriculum.course_requirement
            WHERE target_course_id = ?::uuid
              AND type = 'CONFIGURABLE'
              AND status = 'ACTIVE'
            """,
            (rs, n) -> {
              String reqId = rs.getString("required_course_id");
              return new ConfigurableRow(
                  reqId != null ? UUID.fromString(reqId) : null, rs.getInt("required_credits"));
            },
            TRABAJO_DE_GRADO);

    assertThat(rows)
        .as("Trabajo de Grado must have exactly one ACTIVE CONFIGURABLE pool-gate row")
        .hasSize(1);

    ConfigurableRow gate = rows.get(0);
    assertThat(gate.requiredCourseId())
        .as("Pool gate required_course_id must be NULL (pool = set of DEEPENING electives)")
        .isNull();
    assertThat(gate.requiredCredits())
        .as("Pool gate required_credits must be 4 (>= 4 distinct passed pool members)")
        .isEqualTo(4);
  }

  // ---------------------------------------------------------------------------
  // 1.4 — 8 COURSE_PASSED elective -> line-gatekeeper edges; 3 track gatekeepers NULL
  // ---------------------------------------------------------------------------

  @Test
  void eight_electives_have_course_passed_edge_to_correct_line_gatekeeper() {
    for (Map.Entry<String, String> entry : ELECTIVE_TO_GATEKEEPER.entrySet()) {
      Integer count =
          jdbc.queryForObject(
              """
              SELECT COUNT(*)
              FROM curriculum.course_requirement cr
              JOIN curriculum.course elective  ON cr.target_course_id   = elective.id
              JOIN curriculum.course gatekeeper ON cr.required_course_id = gatekeeper.id
              WHERE elective.official_code   = ?
                AND gatekeeper.official_code  = ?
                AND cr.type   = 'COURSE_PASSED'
                AND cr.status = 'ACTIVE'
              """,
              Integer.class,
              entry.getKey(),
              entry.getValue());

      assertThat(count)
          .as(
              "Elective %s must have exactly one ACTIVE COURSE_PASSED edge to its line gatekeeper %s"
                  .formatted(entry.getKey(), entry.getValue()))
          .isEqualTo(1);
    }
  }

  @Test
  void three_deepening_line_tracks_have_null_gatekeeper_after_relocation() {
    Integer nonNullCount =
        jdbc.queryForObject(
            """
            SELECT COUNT(*)
            FROM curriculum.sequential_track
            WHERE id IN (?::uuid, ?::uuid, ?::uuid)
              AND gatekeeper_course_id IS NOT NULL
            """,
            Integer.class,
            TRACK_SOFTWARE_HCI,
            TRACK_INFRA_TI,
            TRACK_GESTION_INFO);

    assertThat(nonNullCount)
        .as(
            "V028 must NULL the 3 DEEPENING line gatekeepers (relocated onto per-elective edges,"
                + " design §0 CORRECTION)")
        .isEqualTo(0);
  }

  // ---------------------------------------------------------------------------
  // Flyway history: V028 recorded as successful
  // ---------------------------------------------------------------------------

  @Test
  void flyway_history_records_v028_as_successful() {
    Integer count =
        jdbc.queryForObject(
            """
            SELECT COUNT(*)
            FROM flyway_schema_history
            WHERE script = 'V028__curriculum_profundizacion_pool.sql'
              AND success = true
            """,
            Integer.class);

    assertThat(count)
        .as("flyway_schema_history must contain exactly one successful entry for V028")
        .isEqualTo(1);
  }
}
