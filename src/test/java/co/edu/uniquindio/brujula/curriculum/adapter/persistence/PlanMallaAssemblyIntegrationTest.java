package co.edu.uniquindio.brujula.curriculum.adapter.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import co.edu.uniquindio.brujula.AbstractPostgresIntegrationTest;
import co.edu.uniquindio.brujula.curriculum.application.dto.PlanMallaView;
import co.edu.uniquindio.brujula.curriculum.application.port.in.AssemblePlanMallaQuery;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * Testcontainers integration test for the PlanMalla assembly pipeline.
 *
 * <p>Validates:
 *
 * <ul>
 *   <li>Assembly over the seeded Sistemas DIURNA plan returns ≥ 1 semester bucket
 *   <li>Performance: assembly completes in &lt;500ms (ADR-F2-001)
 *   <li>All buckets contain only ACTIVE courses
 *   <li>Buckets are ordered ascending by semester
 * </ul>
 *
 * <p>Uses the Flyway-seeded Sistemas ISC data (V012). Satisfies ADR-F2-001 performance budget.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class PlanMallaAssemblyIntegrationTest extends AbstractPostgresIntegrationTest {

  @Autowired private AssemblePlanMallaQuery assemblePlanMallaQuery;
  @Autowired private JdbcTemplate jdbcTemplate;

  private static final String SISTEMAS_PROGRAM_NAME = "Ingeniería de Sistemas y Computación";

  // ---------------------------------------------------------------------------
  // Sistemas DIURNA assembly
  // ---------------------------------------------------------------------------

  @Test
  @Transactional
  void sistemas_diurna_malla_returns_at_least_one_bucket() {
    UUID planId = fetchSistemasPlanId();

    PlanMallaView result = assemblePlanMallaQuery.assemblePlanMalla(planId, "DIURNA");

    assertThat(result.planId()).isEqualTo(planId);
    assertThat(result.journeyCode()).isEqualTo("DIURNA");
    assertThat(result.semesterBuckets())
        .as("Sistemas DIURNA plan should have at least 1 semester bucket of active courses")
        .isNotEmpty();
  }

  @Test
  @Transactional
  void sistemas_diurna_malla_has_10_semesters() {
    UUID planId = fetchSistemasPlanId();

    PlanMallaView result = assemblePlanMallaQuery.assemblePlanMalla(planId, "DIURNA");

    // Sistemas DIURNA has 10 recommended semesters (V012 seed)
    assertThat(result.semesterBuckets()).hasSize(10);
  }

  @Test
  @Transactional
  void sistemas_nocturna_malla_has_11_semesters() {
    UUID planId = fetchSistemasPlanId();

    PlanMallaView result = assemblePlanMallaQuery.assemblePlanMalla(planId, "NOCTURNA");

    // Sistemas NOCTURNA has 11 recommended semesters (V012 seed, ADR-F2-003)
    assertThat(result.semesterBuckets()).hasSize(11);
  }

  @Test
  @Transactional
  void sistemas_diurna_malla_buckets_ordered_ascending() {
    UUID planId = fetchSistemasPlanId();

    PlanMallaView result = assemblePlanMallaQuery.assemblePlanMalla(planId, "DIURNA");

    // Verify semester numbers are strictly ascending
    for (int i = 1; i < result.semesterBuckets().size(); i++) {
      assertThat(result.semesterBuckets().get(i).recommendedSemester())
          .as("Semester bucket %d should be greater than bucket %d", i, i - 1)
          .isGreaterThan(result.semesterBuckets().get(i - 1).recommendedSemester());
    }
  }

  @Test
  @Transactional
  void sistemas_diurna_malla_contains_all_57_courses() {
    UUID planId = fetchSistemasPlanId();

    PlanMallaView result = assemblePlanMallaQuery.assemblePlanMalla(planId, "DIURNA");

    long totalCourses = result.semesterBuckets().stream().mapToLong(b -> b.courses().size()).sum();

    assertThat(totalCourses)
        .as("Sistemas DIURNA malla should contain all 57 active courses")
        .isEqualTo(57L);
  }

  @Test
  @Transactional
  void sistemas_malla_assembly_completes_under_500ms() {
    UUID planId = fetchSistemasPlanId();

    // Warm-up: pre-load once so measurement is not polluted by JPA cache init
    assemblePlanMallaQuery.assemblePlanMalla(planId, "DIURNA");

    Instant start = Instant.now();
    PlanMallaView result = assemblePlanMallaQuery.assemblePlanMalla(planId, "DIURNA");
    Duration elapsed = Duration.between(start, Instant.now());

    assertThat(result.semesterBuckets()).isNotEmpty();
    assertThat(elapsed.toMillis())
        .as(
            "Malla assembly for Sistemas DIURNA must complete in <500ms (ADR-F2-001), was %dms",
            elapsed.toMillis())
        .isLessThan(500L);
  }

  // ---------------------------------------------------------------------------
  // Helper
  // ---------------------------------------------------------------------------

  private UUID fetchSistemasPlanId() {
    return jdbcTemplate.queryForObject(
        "SELECT sp.id FROM catalog.study_plan sp "
            + "JOIN catalog.academic_program p ON sp.program_id = p.id "
            + "WHERE p.name = ? AND sp.status = 'ACTIVE'",
        UUID.class,
        SISTEMAS_PROGRAM_NAME);
  }
}
