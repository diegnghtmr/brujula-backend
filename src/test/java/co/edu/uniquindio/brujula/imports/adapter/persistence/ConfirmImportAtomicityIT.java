package co.edu.uniquindio.brujula.imports.adapter.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import co.edu.uniquindio.brujula.AbstractPostgresIntegrationTest;
import co.edu.uniquindio.brujula.curriculum.domain.PrerequisiteCycleException;
import co.edu.uniquindio.brujula.imports.application.dto.ConfirmImportCommand;
import co.edu.uniquindio.brujula.imports.application.port.in.ConfirmImportUseCase;
import co.edu.uniquindio.brujula.imports.application.port.out.LoadImportBatchPort;
import co.edu.uniquindio.brujula.imports.application.port.out.SaveImportBatchPort;
import co.edu.uniquindio.brujula.imports.domain.model.ImportBatch;
import co.edu.uniquindio.brujula.imports.domain.model.ImportBatchId;
import co.edu.uniquindio.brujula.imports.domain.model.ImportKind;
import co.edu.uniquindio.brujula.imports.domain.model.ImportRow;
import co.edu.uniquindio.brujula.imports.domain.model.ImportStatus;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Keystone atomicity integration test: proves that the single-{@code @Transactional} boundary in
 * {@code ConfirmImportService#confirm} rolls back ALL row writes when ANY row fails. Uses a REAL
 * Testcontainers Postgres instance (via {@link AbstractPostgresIntegrationTest}) and REAL service
 * beans — no {@code @MockitoBean} on any curriculum use-case.
 *
 * <h3>Scenario A: REQUIREMENTS cycle (the load-bearing rollback proof)</h3>
 *
 * <ol>
 *   <li>Seed two courses (A and B) in a fresh plan using direct JDBC.
 *   <li>Seed a VALIDATED REQUIREMENTS {@code ImportBatch} with two rows: row 0 = A→B (valid); row 1
 *       = B→A (creates a cycle once row 0's edge is flushed in the same TX).
 *   <li>Call the REAL {@code confirmImportUseCase.confirm()}.
 *   <li>Assert (a): {@link PrerequisiteCycleException} propagates.
 *   <li>Assert (b): The batch remains VALIDATED in DB (not CONFIRMED).
 *   <li>Assert (c) — THE LOAD-BEARING assertion: {@code SELECT COUNT(*) FROM
 *       curriculum.course_requirement WHERE target_course_id IN (courseA, courseB)} == 0. Row 0's
 *       A→B edge WAS flushed to the DB session (JPA flush-on-query semantics inside {@code
 *       findAllByPlanId}), allowing row 1's cycle check to see it — and then the TX rollback erased
 *       it. If {@code @Transactional} were removed from {@code confirm()}, or if {@code
 *       CreateCourseRequirementService} used {@code REQUIRES_NEW}, row 0's edge would survive and
 *       the count would be 1.
 * </ol>
 *
 * <h3>Scenario B: happy-path confirm (positive counterpart)</h3>
 *
 * Proves that when all rows succeed the batch is committed as CONFIRMED and the requirement row IS
 * persisted.
 *
 * <p>Test isolation: no class-level {@code @Transactional} — each test must let the service's own
 * transaction start fresh and actually commit or roll back on DB so the assertions read real
 * post-commit state.
 *
 * <p>No {@code @MockitoBean} on any curriculum use-case: both scenarios drive the REAL {@code
 * CreateCourseRequirementService} → REAL {@code SaveCourseRequirementPort} → REAL Postgres.
 */
@SpringBootTest
class ConfirmImportAtomicityIT extends AbstractPostgresIntegrationTest {

  @Autowired private ConfirmImportUseCase confirmImportUseCase;

  @Autowired private SaveImportBatchPort saveImportBatchPort;

  @Autowired private LoadImportBatchPort loadImportBatchPort;

  @Autowired private JdbcTemplate jdbcTemplate;

  // -------------------------------------------------------------------------
  // Scenario A: requirements cycle rolls back ALL writes (keystone rollback proof)
  // -------------------------------------------------------------------------

  /**
   * Seeds two courses A and B plus a 2-row REQUIREMENTS batch. Row 0 (A→B) is flushed to the
   * session when row 1 triggers {@code findAllByPlanId}. Row 1 (B→A) detects a cycle and throws
   * {@link PrerequisiteCycleException}. The TX rolls back, erasing row 0's edge — COUNT == 0 proves
   * real rollback.
   *
   * <p>WHY this would FAIL if {@code @Transactional} were removed: Without the outer transaction,
   * row 0's requirement is committed before row 1 attempts to execute. The cycle exception on row 1
   * would leave row 0's edge in the DB, and {@code COUNT(*) == 1} would violate the assertion.
   */
  @Test
  void requirements_cycle_rolls_back_row0_edge_and_batch_stays_validated() {
    // ---- Arrange: fresh program + plan to isolate this test ----------------
    UUID programId = UUID.randomUUID();
    jdbcTemplate.update(
        "INSERT INTO catalog.academic_program (id, name, faculty, status, version) VALUES (?, ?, 'Facultad', 'ACTIVE', 0)",
        programId,
        "AtomicityProg-" + UUID.randomUUID());

    UUID planId = UUID.randomUUID();
    jdbcTemplate.update(
        "INSERT INTO catalog.study_plan (id, program_id, name, status, total_credits, version) VALUES (?, ?, ?, 'ACTIVE', 100, 0)",
        planId,
        programId,
        "AtomicityPlan-" + UUID.randomUUID());

    // Seed course A and course B in the plan (real rows with FKs satisfied)
    UUID courseAId = UUID.randomUUID();
    jdbcTemplate.update(
        "INSERT INTO curriculum.course (id, plan_id, program_id, name, type, credits, curricular_weekly_hours, status, version) VALUES (?, ?, ?, ?, 'REQUIRED', 3, 3, 'ACTIVE', 0)",
        courseAId,
        planId,
        programId,
        "Atomicity Course A");

    UUID courseBId = UUID.randomUUID();
    jdbcTemplate.update(
        "INSERT INTO curriculum.course (id, plan_id, program_id, name, type, credits, curricular_weekly_hours, status, version) VALUES (?, ?, ?, ?, 'REQUIRED', 3, 3, 'ACTIVE', 0)",
        courseBId,
        planId,
        programId,
        "Atomicity Course B");

    // Seed a VALIDATED REQUIREMENTS ImportBatch:
    //   row 0 = A→B (targetCourse=B, requiredCourse=A) — valid, no cycle yet
    //   row 1 = B→A (targetCourse=A, requiredCourse=B) — creates a cycle once row 0 is flushed
    UUID importId = UUID.randomUUID();
    ImportBatchId batchId = ImportBatchId.of(importId);

    List<ImportRow> rows =
        List.of(
            new ImportRow(
                0,
                Map.of(
                    "destination_course",
                    courseBId.toString(), // B requires A
                    "required_course_or_value",
                    courseAId.toString(),
                    "requirement_type",
                    "COURSE_PASSED",
                    "simultaneous_allowed",
                    "false")),
            new ImportRow(
                1,
                Map.of(
                    "destination_course",
                    courseAId.toString(), // A requires B → cycle!
                    "required_course_or_value",
                    courseBId.toString(),
                    "requirement_type",
                    "COURSE_PASSED",
                    "simultaneous_allowed",
                    "false")));

    ImportBatch batch =
        ImportBatch.reconstitute(
            batchId,
            ImportKind.REQUIREMENTS,
            programId,
            planId,
            null,
            ImportStatus.VALIDATED,
            2,
            2,
            List.of(),
            rows,
            ImportBatch.CURRENT_SCHEMA_VERSION,
            0L,
            Instant.now());

    saveImportBatchPort.save(batch);
    assertThat(loadImportBatchPort.findById(batchId)).isPresent();

    ConfirmImportCommand command =
        new ConfirmImportCommand(
            importId,
            "admin@test.com",
            "ADMIN",
            UUID.randomUUID(),
            0L,
            UUID.randomUUID(),
            "atomicity-req-cycle-hash-" + UUID.randomUUID());

    // ---- Act + Assert (a): PrerequisiteCycleException propagates -------------
    assertThatThrownBy(() -> confirmImportUseCase.confirm(command))
        .isInstanceOf(PrerequisiteCycleException.class)
        .as("Row 1's cycle MUST propagate out of confirm()");

    // ---- Assert (b): batch status is still VALIDATED in DB ------------------
    Optional<ImportBatch> reloaded = loadImportBatchPort.findById(batchId);
    assertThat(reloaded).as("ImportBatch must still be present after rollback").isPresent();
    assertThat(reloaded.get().getStatus())
        .as("ImportBatch status must remain VALIDATED (NOT CONFIRMED) after rollback")
        .isEqualTo(ImportStatus.VALIDATED);

    // ---- Assert (c) — THE LOAD-BEARING PROOF: row 0's flushed edge was rolled back -----
    // Row 0 (A→B) WAS flushed to the DB session by JPA flush-on-query semantics when
    // row 1 called findAllByPlanId(). That flush made A→B visible to row 1's cycle check.
    // Row 1 detected a cycle and threw PrerequisiteCycleException. The @Transactional
    // boundary on confirm() caught the exception and rolled back the ENTIRE transaction —
    // including row 0's already-flushed INSERT. COUNT == 0 proves real rollback.
    //
    // If @Transactional were REMOVED from confirm():
    //   - Row 0's requirement would be committed independently.
    //   - Row 1 would fail with PrerequisiteCycleException (exception propagates, row 1 never
    // committed).
    //   - COUNT would be 1 (row 0 survived) → THIS ASSERTION WOULD FAIL.
    //
    // If CreateCourseRequirementService used REQUIRES_NEW (forbidden — all-or-nothing contract):
    //   - Row 0 would commit in its own nested TX before the outer TX rolls back.
    //   - COUNT would be 1 → THIS ASSERTION WOULD FAIL.
    int requirementCount =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM curriculum.course_requirement WHERE target_course_id IN (?, ?)",
            Integer.class,
            courseAId,
            courseBId);
    assertThat(requirementCount)
        .as(
            "Zero requirement edges must survive after rolled-back confirm — "
                + "row 0's flushed A→B edge must have been rolled back (all-or-nothing contract)")
        .isEqualTo(0);
  }

  // -------------------------------------------------------------------------
  // Scenario B: happy-path confirm — single REQUIREMENTS row commits successfully
  // -------------------------------------------------------------------------

  /**
   * Positive counterpart: a 1-row REQUIREMENTS batch (no cycle) commits as CONFIRMED. Also verifies
   * that the requirement row IS persisted in DB after confirm.
   *
   * <p>If T6.3-1 passes but T6.3-2 fails, the service never commits at all (TX mis-config).
   */
  @Test
  void happy_path_requirements_confirm_commits_batch_and_requirement() {
    // ---- Arrange: fresh program + plan -------------------------------------
    UUID programId = UUID.randomUUID();
    jdbcTemplate.update(
        "INSERT INTO catalog.academic_program (id, name, faculty, status, version) VALUES (?, ?, 'Facultad', 'ACTIVE', 0)",
        programId,
        "HappyProg-" + UUID.randomUUID());

    UUID planId = UUID.randomUUID();
    jdbcTemplate.update(
        "INSERT INTO catalog.study_plan (id, program_id, name, status, total_credits, version) VALUES (?, ?, ?, 'ACTIVE', 100, 0)",
        planId,
        programId,
        "HappyPlan-" + UUID.randomUUID());

    // Seed courses X (prerequisite) and Y (target)
    UUID courseXId = UUID.randomUUID();
    jdbcTemplate.update(
        "INSERT INTO curriculum.course (id, plan_id, program_id, name, type, credits, curricular_weekly_hours, status, version) VALUES (?, ?, ?, ?, 'REQUIRED', 3, 3, 'ACTIVE', 0)",
        courseXId,
        planId,
        programId,
        "Happy Course X");

    UUID courseYId = UUID.randomUUID();
    jdbcTemplate.update(
        "INSERT INTO curriculum.course (id, plan_id, program_id, name, type, credits, curricular_weekly_hours, status, version) VALUES (?, ?, ?, ?, 'REQUIRED', 3, 3, 'ACTIVE', 0)",
        courseYId,
        planId,
        programId,
        "Happy Course Y");

    // Seed 1-row REQUIREMENTS batch: Y requires X (no cycle)
    UUID importId = UUID.randomUUID();
    ImportBatchId batchId = ImportBatchId.of(importId);

    List<ImportRow> rows =
        List.of(
            new ImportRow(
                0,
                Map.of(
                    "destination_course",
                    courseYId.toString(),
                    "required_course_or_value",
                    courseXId.toString(),
                    "requirement_type",
                    "COURSE_PASSED",
                    "simultaneous_allowed",
                    "false")));

    ImportBatch batch =
        ImportBatch.reconstitute(
            batchId,
            ImportKind.REQUIREMENTS,
            programId,
            planId,
            null,
            ImportStatus.VALIDATED,
            1,
            1,
            List.of(),
            rows,
            ImportBatch.CURRENT_SCHEMA_VERSION,
            0L,
            Instant.now());

    saveImportBatchPort.save(batch);

    ConfirmImportCommand command =
        new ConfirmImportCommand(
            importId,
            "admin@test.com",
            "ADMIN",
            UUID.randomUUID(),
            0L,
            UUID.randomUUID(),
            "atomicity-happy-req-hash-" + UUID.randomUUID());

    // ---- Act ---------------------------------------------------------------
    co.edu.uniquindio.brujula.imports.application.dto.ImportSummary result =
        confirmImportUseCase.confirm(command);

    // ---- Assert ------------------------------------------------------------
    assertThat(result.status())
        .as("Successful confirm must return CONFIRMED status")
        .isEqualTo("CONFIRMED");

    // Batch must be CONFIRMED in DB
    Optional<ImportBatch> reloaded = loadImportBatchPort.findById(batchId);
    assertThat(reloaded).isPresent();
    assertThat(reloaded.get().getStatus())
        .as("ImportBatch status must be CONFIRMED after successful confirm")
        .isEqualTo(ImportStatus.CONFIRMED);

    // The requirement row must exist in DB
    int requirementCount =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM curriculum.course_requirement WHERE target_course_id = ?",
            Integer.class,
            courseYId);
    assertThat(requirementCount)
        .as("Requirement edge must be persisted after successful confirm")
        .isEqualTo(1);
  }
}
