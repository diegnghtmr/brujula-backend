package co.edu.uniquindio.brujula;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * RED -> GREEN test for V029__repoint_2026_2_profundizacion_offerings.sql.
 *
 * <p>Change: PROP-001-profundizacion-elective-pool, PR1b. Source of truth: design D6, §9, §11
 * R-offering-map.
 *
 * <p>V022 mapped every named "Profundización ..." offering in period 2026-2 to one of the two
 * generic slot ids (Profundización I / Profundización III), before the 8 specific elective courses
 * existed. V028 (PR1a) created those 8 electives. V029 re-points {@code
 * offering.course_offering.course_id} from the two shared slot ids to the specific elective id,
 * matched by a normalized {@code course_name} mapping (accent-fold, strip
 * "Profundización"/"Profundización -" prefix, strip "(Sincrónico)"/"(Asincrónico)" suffix,
 * trim/upper).
 *
 * <p>Covers task 2.1 of PR1b:
 *
 * <ul>
 *   <li>Every 2026-2 profundización offering resolves to one of the 8 elective ids
 *   <li>Zero offerings remain on a shared slot id after re-point
 *   <li>Specific name -&gt; elective mappings, including a "(Sincrónico)" variant collapsing to its
 *       base elective
 * </ul>
 */
@SpringBootTest
class OfferingRepointV029IT extends AbstractPostgresIntegrationTest {

  @Autowired private JdbcTemplate jdbc;

  // --- V012 shared slot ids (superseded target of the re-point) ---
  private static final String PROFUNDIZACION_I_SLOT = "c0080000-0000-4000-a000-000000000004";
  private static final String PROFUNDIZACION_III_SLOT = "c0090000-0000-4000-a000-000000000004";

  // --- V028 elective ids (design §3 table) ---
  private static final UUID DISENO_CENTRADO_EL_USUARIO =
      UUID.fromString("c0dee000-0000-4000-a000-000000000001");
  private static final UUID DESARROLLO_APPS_MOVILES =
      UUID.fromString("c0dee000-0000-4000-a000-000000000002");
  private static final UUID USABILIDAD_UX = UUID.fromString("c0dee000-0000-4000-a000-000000000003");
  private static final UUID COMPUTACION_NUBE =
      UUID.fromString("c0dee000-0000-4000-a000-000000000004");
  private static final UUID ARQUITECTURA_MICROSERVICIOS =
      UUID.fromString("c0dee000-0000-4000-a000-000000000005");
  private static final UUID VISUALIZACION_DATOS =
      UUID.fromString("c0dee000-0000-4000-a000-000000000006");
  private static final UUID VISION_ARTIFICIAL =
      UUID.fromString("c0dee000-0000-4000-a000-000000000007");
  private static final UUID INTRODUCCION_BIG_DATA =
      UUID.fromString("c0dee000-0000-4000-a000-000000000008");

  private static final List<UUID> ELECTIVE_IDS =
      List.of(
          DISENO_CENTRADO_EL_USUARIO,
          DESARROLLO_APPS_MOVILES,
          USABILIDAD_UX,
          COMPUTACION_NUBE,
          ARQUITECTURA_MICROSERVICIOS,
          VISUALIZACION_DATOS,
          VISION_ARTIFICIAL,
          INTRODUCCION_BIG_DATA);

  // ---------------------------------------------------------------------------
  // Every 2026-2 profundización offering resolves to one of the 8 elective ids
  // ---------------------------------------------------------------------------

  @Test
  void every_2026_2_profundizacion_offering_resolves_to_one_of_the_eight_elective_ids() {
    List<UUID> courseIds =
        jdbc.queryForList(
            """
            SELECT co.course_id
            FROM offering.course_offering co
            JOIN offering.academic_period ap ON ap.id = co.period_id
            WHERE ap.name = '2026-2'
              AND co.course_name ILIKE 'Profundización%'
            """,
            UUID.class);

    // V022 seeds exactly 18 named profundización offerings (design §3 "V022 offerings collapsed"
    // column totals: 2+2+2+4+2+2+2+2 = 18).
    assertThat(courseIds).hasSize(18);
    assertThat(courseIds)
        .as("every profundización offering must resolve to one of the 8 elective ids post-V029")
        .allSatisfy(id -> assertThat(ELECTIVE_IDS).contains(id));
  }

  // ---------------------------------------------------------------------------
  // Zero offerings remain on a shared slot id
  // ---------------------------------------------------------------------------

  @Test
  void zero_profundizacion_offerings_remain_on_a_shared_slot_id() {
    Integer remaining =
        jdbc.queryForObject(
            """
            SELECT COUNT(*)
            FROM offering.course_offering co
            JOIN offering.academic_period ap ON ap.id = co.period_id
            WHERE ap.name = '2026-2'
              AND co.course_id IN (?::uuid, ?::uuid)
            """,
            Integer.class,
            PROFUNDIZACION_I_SLOT,
            PROFUNDIZACION_III_SLOT);

    assertThat(remaining)
        .as("V029 must re-point every profundización offering off the shared slot ids")
        .isZero();
  }

  // ---------------------------------------------------------------------------
  // Specific name -> elective mappings, including a "(Sincrónico)" variant
  // ---------------------------------------------------------------------------

  @Test
  void specific_offerings_map_to_the_correct_elective_including_sincronico_variant() {
    // "Profundización Computación en la Nube" (base, DIURNA) and
    // "Profundización Computación en la Nube (Sincrónico)" (DIURNA) must collapse to the SAME
    // elective (design §3 row 4).
    assertCourseId("02200085-0000-4001-a000-000000000085", COMPUTACION_NUBE);
    assertCourseId("02200086-0000-4001-a000-000000000086", COMPUTACION_NUBE);

    // "Profundización Diseño Centrado en el Usuario" (DIURNA, prefix without dash).
    assertCourseId("02200091-0000-4001-a000-000000000091", DISENO_CENTRADO_EL_USUARIO);

    // "Profundización Usabilidad y Experiencia de Usuario" (NOCTURNA, originally mapped to the
    // Profundización III shared slot, not I).
    assertCourseId("02200179-0000-4001-a000-000000000179", USABILIDAD_UX);

    // "Profundización - Visión Artificial" (NOCTURNA, "Profundización - " dash-prefix variant,
    // originally mapped to the Profundización III shared slot).
    assertCourseId("02200180-0000-4001-a000-000000000180", VISION_ARTIFICIAL);
  }

  private void assertCourseId(String offeringId, UUID expectedElectiveId) {
    UUID courseId =
        jdbc.queryForObject(
            "SELECT course_id FROM offering.course_offering WHERE id = ?::uuid",
            UUID.class,
            offeringId);

    assertThat(courseId)
        .as("offering %s must resolve to elective %s".formatted(offeringId, expectedElectiveId))
        .isEqualTo(expectedElectiveId);
  }

  // ---------------------------------------------------------------------------
  // Regression: V029 only UPDATEs course_id, it must not change the total offering count
  // ---------------------------------------------------------------------------

  @Test
  void total_2026_2_offering_count_is_unchanged_by_the_repoint() {
    Integer offeringCount =
        jdbc.queryForObject(
            """
            SELECT COUNT(*)
            FROM offering.course_offering co
            JOIN offering.academic_period ap ON ap.id = co.period_id
            WHERE ap.name = '2026-2'
            """,
            Integer.class);

    assertThat(offeringCount)
        .as("V029 must only UPDATE course_id on existing rows, never insert/delete offerings")
        .isEqualTo(185);
  }

  // ---------------------------------------------------------------------------
  // Flyway history: V029 recorded as successful
  // ---------------------------------------------------------------------------

  @Test
  void flyway_history_records_v029_as_successful() {
    Integer count =
        jdbc.queryForObject(
            """
            SELECT COUNT(*)
            FROM flyway_schema_history
            WHERE script = 'V029__repoint_2026_2_profundizacion_offerings.sql'
              AND success = true
            """,
            Integer.class);

    assertThat(count)
        .as("flyway_schema_history must contain exactly one successful entry for V029")
        .isEqualTo(1);
  }
}
