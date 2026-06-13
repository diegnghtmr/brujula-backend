package co.edu.uniquindio.brujula;

import static org.assertj.core.api.Assertions.assertThat;

import co.edu.uniquindio.brujula.offering.application.dto.AcademicPeriodView;
import co.edu.uniquindio.brujula.offering.application.port.in.ListPeriodsQuery;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * RED → GREEN test for V025__seed_historical_academic_periods.sql.
 *
 * <p>Verifies that:
 *
 * <ol>
 *   <li>After all migrations run, {@code offering.academic_period} contains exactly 8 rows: the 7
 *       new historical periods (2023-1 … 2026-1, status=CLOSED) plus the pre-existing 2026-2
 *       (status=ACTIVE, seeded by V017).
 *   <li>All 7 newly seeded periods have status {@code 'CLOSED'} and their expected names.
 *   <li>{@link ListPeriodsQuery#listPeriods(String)} (which backs {@code GET /periods}) returns all
 *       8 periods (none filtered out), ordered by start_date DESC (2026-2 first, 2023-1 last).
 * </ol>
 *
 * <p>Fails until V025__seed_historical_academic_periods.sql is applied.
 */
@SpringBootTest
class FlywayV025HistoricalPeriodsSeedTest extends AbstractPostgresIntegrationTest {

  @Autowired private JdbcTemplate jdbc;
  @Autowired private ListPeriodsQuery listPeriodsQuery;

  // -----------------------------------------------------------------------
  // 1. Total row count in offering.academic_period must be exactly 8
  // -----------------------------------------------------------------------

  @Test
  void academic_period_table_contains_exactly_8_periods_after_v025() {
    Integer count =
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM offering.academic_period", Integer.class);

    assertThat(count)
        .as(
            "offering.academic_period must contain exactly 8 rows after V017 (2026-2) and"
                + " V025 (2023-1 … 2026-1)")
        .isEqualTo(8);
  }

  // -----------------------------------------------------------------------
  // 2. All 7 newly seeded periods have status CLOSED
  // -----------------------------------------------------------------------

  @Test
  void v025_seeds_exactly_7_closed_periods() {
    Integer closedCount =
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM offering.academic_period WHERE status = 'CLOSED'",
            Integer.class);

    assertThat(closedCount)
        .as("V025 must seed exactly 7 CLOSED periods (2023-1 through 2026-1)")
        .isEqualTo(7);
  }

  @Test
  void v025_seeds_all_expected_historical_period_names() {
    List<String> names =
        jdbc.queryForList(
            "SELECT name FROM offering.academic_period WHERE status = 'CLOSED' ORDER BY start_date",
            String.class);

    assertThat(names)
        .as("V025 must seed exactly these 7 CLOSED period names in chronological order")
        .containsExactly("2023-1", "2023-2", "2024-1", "2024-2", "2025-1", "2025-2", "2026-1");
  }

  // -----------------------------------------------------------------------
  // 3. Pre-existing 2026-2 (V017) is NOT touched
  // -----------------------------------------------------------------------

  @Test
  void v017_period_2026_2_remains_active_after_v025() {
    String status =
        jdbc.queryForObject(
            "SELECT status FROM offering.academic_period WHERE name = '2026-2'",
            String.class);

    assertThat(status)
        .as("V025 must not touch 2026-2 — it must remain ACTIVE as seeded by V017")
        .isEqualTo("ACTIVE");
  }

  // -----------------------------------------------------------------------
  // 4. GET /periods returns all 8 non-ARCHIVED periods ordered start_date DESC
  // -----------------------------------------------------------------------

  @Test
  void list_periods_returns_all_8_periods_none_filtered_out() {
    List<AcademicPeriodView> periods = listPeriodsQuery.listPeriods(null);

    assertThat(periods)
        .as("listPeriods(null) must return all 8 non-ARCHIVED periods (none are ARCHIVED)")
        .hasSize(8);
  }

  @Test
  void list_periods_is_ordered_by_start_date_desc_2026_2_first_2023_1_last() {
    List<AcademicPeriodView> periods = listPeriodsQuery.listPeriods(null);

    assertThat(periods).isNotEmpty();
    assertThat(periods.get(0).name())
        .as("First period must be 2026-2 (latest start_date = 2026-07-15)")
        .isEqualTo("2026-2");
    assertThat(periods.get(periods.size() - 1).name())
        .as("Last period must be 2023-1 (earliest start_date = 2023-01-16)")
        .isEqualTo("2023-1");
  }

  @Test
  void list_periods_contains_all_expected_names() {
    List<String> names =
        listPeriodsQuery.listPeriods(null).stream().map(AcademicPeriodView::name).toList();

    assertThat(names)
        .as("listPeriods must expose all 8 period names for the /periods dropdown")
        .containsExactlyInAnyOrder(
            "2026-2", "2026-1", "2025-2", "2025-1", "2024-2", "2024-1", "2023-2", "2023-1");
  }

  // -----------------------------------------------------------------------
  // 5. Flyway history: V025 recorded as successful
  // -----------------------------------------------------------------------

  @Test
  void flyway_history_records_v025_as_successful() {
    Integer count =
        jdbc.queryForObject(
            """
            SELECT COUNT(*)
            FROM flyway_schema_history
            WHERE script = 'V025__seed_historical_academic_periods.sql'
              AND success = true
            """,
            Integer.class);

    assertThat(count)
        .as("flyway_schema_history must contain exactly one successful entry for V025")
        .isEqualTo(1);
  }
}
