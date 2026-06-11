package co.edu.uniquindio.brujula;

import static org.assertj.core.api.Assertions.assertThat;

import co.edu.uniquindio.brujula.progress.domain.CourseRequirementEdge;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * RED → GREEN test for V021__promote_isc_hard_prereqs.sql.
 *
 * <p>Product decision (2026-06-10): Two ISC malla edges that were seeded as SOFT
 * (confidence=MEDIUM, inferred=true) in V012 must be promoted to HARD (confidence=HIGH,
 * inferred=false) so the availability engine blocks the dependent course when the prerequisite is
 * unmet.
 *
 * <p>Edges under test:
 *
 * <ul>
 *   <li>Matemáticas Discretas ← Álgebra Lineal (target=c0030000-…-000000000003,
 *       required=c0020000-…-000000000002)
 *   <li>Formulación y Evaluación de Proyectos ← Ingeniería Económica
 *       (target=c0080000-…-000000000001, required=c0070000-…-000000000001)
 * </ul>
 *
 * <p>Verified via:
 *
 * <ol>
 *   <li>Raw column values: {@code confidence='HIGH'} and {@code inferred=false} in {@code
 *       curriculum.course_requirement}.
 *   <li>Domain rule: {@link CourseRequirementEdge#isHard()} returns {@code true} for both edges.
 * </ol>
 *
 * <p>Fails until V021__promote_isc_hard_prereqs.sql is applied. Reversible: to revert, set
 * confidence='MEDIUM', inferred=true for the two rows (matched by target/required/type tuple).
 */
@SpringBootTest
class FlywayV021PromoteIscHardPrereqsTest extends AbstractPostgresIntegrationTest {

  @Autowired private JdbcTemplate jdbc;

  // Stable course UUIDs from V012 (never change — they are seed constants).
  private static final String MATEMATICAS_DISCRETAS_ID = "c0030000-0000-4000-a000-000000000003";
  private static final String ALGEBRA_LINEAL_ID = "c0020000-0000-4000-a000-000000000002";
  private static final String FORMULACION_PROYECTOS_ID = "c0080000-0000-4000-a000-000000000001";
  private static final String INGENIERIA_ECONOMICA_ID = "c0070000-0000-4000-a000-000000000001";

  // ---------------------------------------------------------------------------
  // Edge 1: Matemáticas Discretas ← Álgebra Lineal
  // ---------------------------------------------------------------------------

  @Test
  void matematicas_discretas_algebra_lineal_edge_has_confidence_high() {
    String confidence =
        jdbc.queryForObject(
            """
            SELECT confidence
            FROM curriculum.course_requirement
            WHERE target_course_id = ?::uuid
              AND required_course_id = ?::uuid
              AND type = 'COURSE_PASSED'
            """,
            String.class,
            MATEMATICAS_DISCRETAS_ID,
            ALGEBRA_LINEAL_ID);

    assertThat(confidence)
        .as(
            "V021: Matemáticas Discretas ← Álgebra Lineal edge must have confidence=HIGH"
                + " after promotion")
        .isEqualTo("HIGH");
  }

  @Test
  void matematicas_discretas_algebra_lineal_edge_has_inferred_false() {
    Boolean inferred =
        jdbc.queryForObject(
            """
            SELECT inferred
            FROM curriculum.course_requirement
            WHERE target_course_id = ?::uuid
              AND required_course_id = ?::uuid
              AND type = 'COURSE_PASSED'
            """,
            Boolean.class,
            MATEMATICAS_DISCRETAS_ID,
            ALGEBRA_LINEAL_ID);

    assertThat(inferred)
        .as(
            "V021: Matemáticas Discretas ← Álgebra Lineal edge must have inferred=false"
                + " after promotion")
        .isFalse();
  }

  @Test
  void matematicas_discretas_algebra_lineal_edge_is_hard() {
    record EdgeRow(String confidence, Boolean inferred) {}

    EdgeRow row =
        jdbc.queryForObject(
            """
            SELECT confidence, inferred
            FROM curriculum.course_requirement
            WHERE target_course_id = ?::uuid
              AND required_course_id = ?::uuid
              AND type = 'COURSE_PASSED'
            """,
            (rs, n) -> new EdgeRow(rs.getString("confidence"), rs.getBoolean("inferred")),
            MATEMATICAS_DISCRETAS_ID,
            ALGEBRA_LINEAL_ID);

    CourseRequirementEdge edge =
        new CourseRequirementEdge(
            java.util.UUID.fromString(MATEMATICAS_DISCRETAS_ID),
            java.util.UUID.fromString(ALGEBRA_LINEAL_ID),
            "COURSE_PASSED",
            row.confidence(),
            row.inferred());

    assertThat(edge.isHard())
        .as(
            "V021: Matemáticas Discretas ← Álgebra Lineal must compute as HARD"
                + " (inferred=false OR confidence=HIGH)")
        .isTrue();
  }

  // ---------------------------------------------------------------------------
  // Edge 2: Formulación y Evaluación de Proyectos ← Ingeniería Económica
  // ---------------------------------------------------------------------------

  @Test
  void formulacion_proyectos_ingenieria_economica_edge_has_confidence_high() {
    String confidence =
        jdbc.queryForObject(
            """
            SELECT confidence
            FROM curriculum.course_requirement
            WHERE target_course_id = ?::uuid
              AND required_course_id = ?::uuid
              AND type = 'COURSE_PASSED'
            """,
            String.class,
            FORMULACION_PROYECTOS_ID,
            INGENIERIA_ECONOMICA_ID);

    assertThat(confidence)
        .as(
            "V021: Formulación y Evaluación de Proyectos ← Ingeniería Económica edge must have"
                + " confidence=HIGH after promotion")
        .isEqualTo("HIGH");
  }

  @Test
  void formulacion_proyectos_ingenieria_economica_edge_has_inferred_false() {
    Boolean inferred =
        jdbc.queryForObject(
            """
            SELECT inferred
            FROM curriculum.course_requirement
            WHERE target_course_id = ?::uuid
              AND required_course_id = ?::uuid
              AND type = 'COURSE_PASSED'
            """,
            Boolean.class,
            FORMULACION_PROYECTOS_ID,
            INGENIERIA_ECONOMICA_ID);

    assertThat(inferred)
        .as(
            "V021: Formulación y Evaluación de Proyectos ← Ingeniería Económica edge must have"
                + " inferred=false after promotion")
        .isFalse();
  }

  @Test
  void formulacion_proyectos_ingenieria_economica_edge_is_hard() {
    record EdgeRow(String confidence, Boolean inferred) {}

    EdgeRow row =
        jdbc.queryForObject(
            """
            SELECT confidence, inferred
            FROM curriculum.course_requirement
            WHERE target_course_id = ?::uuid
              AND required_course_id = ?::uuid
              AND type = 'COURSE_PASSED'
            """,
            (rs, n) -> new EdgeRow(rs.getString("confidence"), rs.getBoolean("inferred")),
            FORMULACION_PROYECTOS_ID,
            INGENIERIA_ECONOMICA_ID);

    CourseRequirementEdge edge =
        new CourseRequirementEdge(
            java.util.UUID.fromString(FORMULACION_PROYECTOS_ID),
            java.util.UUID.fromString(INGENIERIA_ECONOMICA_ID),
            "COURSE_PASSED",
            row.confidence(),
            row.inferred());

    assertThat(edge.isHard())
        .as(
            "V021: Formulación y Evaluación de Proyectos ← Ingeniería Económica must compute as HARD"
                + " (inferred=false OR confidence=HIGH)")
        .isTrue();
  }

  // ---------------------------------------------------------------------------
  // Triangulation: verify exactly 2 rows were touched (no collateral damage)
  // ---------------------------------------------------------------------------

  @Test
  void v021_touches_exactly_two_medium_inferred_edges_for_isc() {
    // After V021, the ISC plan must have ZERO remaining MEDIUM/inferred COURSE_PASSED edges.
    // V012 seeded exactly 2 such edges; V021 promotes both to HIGH/non-inferred.
    Integer remainingMediumInferred =
        jdbc.queryForObject(
            """
            SELECT COUNT(*)
            FROM curriculum.course_requirement cr
            JOIN curriculum.course c ON cr.target_course_id = c.id
            JOIN catalog.study_plan sp ON c.plan_id = sp.id
            JOIN catalog.academic_program ap ON sp.program_id = ap.id
            WHERE ap.name = 'Ingeniería de Sistemas y Computación'
              AND cr.type = 'COURSE_PASSED'
              AND cr.confidence = 'MEDIUM'
              AND cr.inferred = true
            """,
            Integer.class);

    assertThat(remainingMediumInferred)
        .as(
            "V021: after promotion, ISC must have 0 remaining MEDIUM/inferred COURSE_PASSED edges"
                + " (V012 seeded exactly 2; V021 promotes both)")
        .isEqualTo(0);
  }

  // ---------------------------------------------------------------------------
  // Triangulation: non-ISC MEDIUM/inferred edges are NOT touched
  // ---------------------------------------------------------------------------

  @Test
  void v021_does_not_touch_non_isc_edges() {
    // This test uses stable known HIGH/false edges from ISC itself to confirm scope.
    // The two newly-promoted edges (confidence=HIGH, inferred=false) must exist exactly once each.
    Integer edgeOneCount =
        jdbc.queryForObject(
            """
            SELECT COUNT(*)
            FROM curriculum.course_requirement
            WHERE target_course_id = ?::uuid
              AND required_course_id = ?::uuid
              AND type = 'COURSE_PASSED'
              AND confidence = 'HIGH'
              AND inferred = false
            """,
            Integer.class,
            MATEMATICAS_DISCRETAS_ID,
            ALGEBRA_LINEAL_ID);

    Integer edgeTwoCount =
        jdbc.queryForObject(
            """
            SELECT COUNT(*)
            FROM curriculum.course_requirement
            WHERE target_course_id = ?::uuid
              AND required_course_id = ?::uuid
              AND type = 'COURSE_PASSED'
              AND confidence = 'HIGH'
              AND inferred = false
            """,
            Integer.class,
            FORMULACION_PROYECTOS_ID,
            INGENIERIA_ECONOMICA_ID);

    assertThat(edgeOneCount)
        .as("V021: exactly 1 promoted Matemáticas Discretas ← Álgebra Lineal edge (HIGH/false)")
        .isEqualTo(1);
    assertThat(edgeTwoCount)
        .as(
            "V021: exactly 1 promoted Formulación y Evaluación ← Ingeniería Económica edge (HIGH/false)")
        .isEqualTo(1);
  }

  // ---------------------------------------------------------------------------
  // Flyway history: V021 recorded as successful
  // ---------------------------------------------------------------------------

  @Test
  void flyway_history_records_v021_as_successful() {
    Integer count =
        jdbc.queryForObject(
            """
            SELECT COUNT(*)
            FROM flyway_schema_history
            WHERE script = 'V021__promote_isc_hard_prereqs.sql'
              AND success = true
            """,
            Integer.class);

    assertThat(count)
        .as("flyway_schema_history must contain exactly one successful entry for V021")
        .isEqualTo(1);
  }
}
