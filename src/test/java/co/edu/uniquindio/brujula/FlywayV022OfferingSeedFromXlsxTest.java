package co.edu.uniquindio.brujula;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

/** Verifies that V022 replaces the V017 offering fixtures with the real 2026-2 XLSX catalog. */
@SpringBootTest
class FlywayV022OfferingSeedFromXlsxTest extends AbstractPostgresIntegrationTest {

  @Autowired private JdbcTemplate jdbc;

  @Test
  void v022_loads_real_2026_2_offering_counts_from_xlsx() {
    Integer offeringCount =
        jdbc.queryForObject(
            """
            SELECT COUNT(*)
            FROM offering.course_offering co
            JOIN offering.academic_period ap ON ap.id = co.period_id
            WHERE ap.name = '2026-2'
            """,
            Integer.class);
    Integer optionCount =
        jdbc.queryForObject(
            """
            SELECT COUNT(*)
            FROM offering.schedule_option so
            JOIN offering.course_offering co ON co.id = so.offering_id
            JOIN offering.academic_period ap ON ap.id = co.period_id
            WHERE ap.name = '2026-2'
            """,
            Integer.class);
    Integer sessionCount =
        jdbc.queryForObject(
            """
            SELECT COUNT(*)
            FROM offering.schedule_session ss
            JOIN offering.schedule_option so ON so.id = ss.schedule_option_id
            JOIN offering.course_offering co ON co.id = so.offering_id
            JOIN offering.academic_period ap ON ap.id = co.period_id
            WHERE ap.name = '2026-2'
            """,
            Integer.class);

    assertThat(offeringCount).isEqualTo(185);
    assertThat(optionCount).isEqualTo(185);
    assertThat(sessionCount).isEqualTo(338);
  }

  @Test
  void v022_includes_both_diurna_and_nocturna_final_sistemas_catalog_offerings() {
    Map<String, Integer> optionsByJourney =
        jdbc.query(
            """
            SELECT so.journey_code, COUNT(*) AS total
            FROM offering.schedule_option so
            JOIN offering.course_offering co ON co.id = so.offering_id
            JOIN offering.academic_period ap ON ap.id = co.period_id
            WHERE ap.name = '2026-2'
              AND co.program_id = '11111111-0000-4000-a000-000000000001'::uuid
            GROUP BY so.journey_code
            """,
            rs -> {
              java.util.Map<String, Integer> result = new java.util.HashMap<>();
              while (rs.next()) {
                result.put(rs.getString("journey_code"), rs.getInt("total"));
              }
              return result;
            });

    assertThat(optionsByJourney).containsKeys("DIURNA", "NOCTURNA");
    assertThat(optionsByJourney.get("DIURNA")).isGreaterThan(0);
    assertThat(optionsByJourney.get("NOCTURNA")).isGreaterThan(0);

    Integer nonFinalSistemasRows =
        jdbc.queryForObject(
            """
            SELECT COUNT(*)
            FROM offering.course_offering co
            JOIN offering.academic_period ap ON ap.id = co.period_id
            WHERE ap.name = '2026-2'
              AND co.program_id = '11111111-0000-4000-a000-000000000001'::uuid
              AND co.status <> 'FINAL'
            """,
            Integer.class);

    assertThat(nonFinalSistemasRows).isZero();
  }

  @Test
  void v022_maps_named_deepening_offerings_without_inventing_course_ids() {
    Integer mappedDeepeningRows =
        jdbc.queryForObject(
            """
            SELECT COUNT(*)
            FROM offering.course_offering co
            JOIN offering.academic_period ap ON ap.id = co.period_id
            WHERE ap.name = '2026-2'
              AND co.course_name ILIKE 'Profundización%'
              AND co.notes ILIKE '%generic Profundización%'
              AND co.course_id IN (
                  'c0080000-0000-4000-a000-000000000004'::uuid,
                  'c0090000-0000-4000-a000-000000000004'::uuid
              )
            """,
            Integer.class);

    // V022 inserts 18 named deepening offerings (14 mapped to the Profundización I
    // slot, 4 to Profundización III), each reusing an existing generic course_id.
    // The header comment on line 6 also matches 'generic Profundización' in the file
    // but is never persisted, so the row count in the table is 18, not 19.
    assertThat(mappedDeepeningRows).isEqualTo(18);
  }

  @Test
  void v022_preserves_validated_lenguas_fixture_when_workbook_has_no_lenguas_catalog() {
    Integer lenguasRows =
        jdbc.queryForObject(
            """
            SELECT COUNT(*)
            FROM offering.course_offering co
            JOIN offering.academic_period ap ON ap.id = co.period_id
            WHERE ap.name = '2026-2'
              AND co.program_id = '11111111-0000-4000-b000-000000000001'::uuid
              AND co.course_id = 'a0050000-0000-4000-b000-000000000003'::uuid
              AND co.notes ILIKE '%personal schedule, not a source catalog%'
            """,
            Integer.class);

    assertThat(lenguasRows).isEqualTo(1);

    String lenguasStatus =
        jdbc.queryForObject(
            """
            SELECT co.status
            FROM offering.course_offering co
            JOIN offering.academic_period ap ON ap.id = co.period_id
            WHERE ap.name = '2026-2'
              AND co.program_id = '11111111-0000-4000-b000-000000000001'::uuid
              AND co.course_id = 'a0050000-0000-4000-b000-000000000003'::uuid
              AND co.notes ILIKE '%personal schedule, not a source catalog%'
            """,
            String.class);

    assertThat(lenguasStatus).isEqualTo("PRELIMINARY");
  }
}
