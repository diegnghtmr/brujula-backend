package co.edu.uniquindio.brujula.imports.adapter.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import co.edu.uniquindio.brujula.AbstractPostgresIntegrationTest;
import co.edu.uniquindio.brujula.imports.application.dto.ConfirmImportCommand;
import co.edu.uniquindio.brujula.imports.application.dto.ImportSummary;
import co.edu.uniquindio.brujula.imports.application.port.in.ConfirmImportUseCase;
import co.edu.uniquindio.brujula.imports.application.port.out.LoadImportBatchPort;
import co.edu.uniquindio.brujula.imports.application.port.out.SaveImportBatchPort;
import co.edu.uniquindio.brujula.imports.domain.model.ImportBatch;
import co.edu.uniquindio.brujula.imports.domain.model.ImportBatchId;
import co.edu.uniquindio.brujula.imports.domain.model.ImportKind;
import co.edu.uniquindio.brujula.imports.domain.model.ImportRow;
import co.edu.uniquindio.brujula.imports.domain.model.ImportStatus;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * End-to-end integration test for OFFERINGS import confirm.
 *
 * <p>Uses the REAL {@code ConfirmImportService} wired with the REAL {@code CreateOfferingService}
 * (no {@code @MockitoBean} on any use-case). Drives against a live Testcontainers Postgres
 * instance via {@link AbstractPostgresIntegrationTest}.
 *
 * <h3>T7.2 — Single-session offering confirm (regression, expandDay proof)</h3>
 * <ul>
 *   <li>(a) Confirm succeeds — batch status becomes CONFIRMED in DB.</li>
 *   <li>(b) An offering row is persisted in {@code offering.course_offering}.</li>
 *   <li>(c) The persisted offering status is {@code DRAFT} (NFR-003 load-bearing).</li>
 *   <li>(d) The persisted session {@code day} is {@code "MONDAY"} — proves expandDay works.</li>
 * </ul>
 *
 * <h3>WU4-IT1 — Multi-session grouping keystone proof (QAL-IMP-001 / DEBT-F6-002)</h3>
 * <p>THE load-bearing DB-count assertion: 3 CSV rows for the same offering on MON/WED/FRI
 * MUST yield exactly <b>1</b> {@code course_offering}, <b>1</b> {@code schedule_option}, and
 * <b>3</b> {@code schedule_session} rows — NOT 3 offerings. A mock-based test would prove
 * nothing because it mocks away the grouping boundary. Only the real service chain + real DB
 * can verify the fix.
 *
 * <h3>WU4-IT2 — Atomicity with multi-session grouping (QAL-IMP-001-S2)</h3>
 * A batch where one row fails must roll back ALL offering writes (COUNT == 0).
 *
 * <p>No class-level {@code @Transactional} — tests must let the service TX commit to DB so the
 * post-commit JDBC assertions read real persisted state.
 */
@SpringBootTest
class ConfirmImportOfferingsIT extends AbstractPostgresIntegrationTest {

    @Autowired
    private ConfirmImportUseCase confirmImportUseCase;

    @Autowired
    private SaveImportBatchPort saveImportBatchPort;

    @Autowired
    private LoadImportBatchPort loadImportBatchPort;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // -------------------------------------------------------------------------
    // T7.2 — Happy-path OFFERINGS confirm: DRAFT status + MONDAY session day
    // -------------------------------------------------------------------------

    /**
     * Seeds a real course (curriculum.course), a real academic period (offering.academic_period),
     * and a VALIDATED OFFERINGS {@code ImportBatch} with one row whose {@code day = "MON"}.
     *
     * <p>Calls the REAL {@code confirmImportUseCase.confirm()} with NO mocks on any use-case bean.
     *
     * <p>Assertions (c) and (d) are load-bearing:
     * <ul>
     *   <li>(c) fails if {@code CourseOffering.create()} ever stops hardcoding DRAFT status</li>
     *   <li>(d) fails if {@code expandDay} is removed or broken — "MON" reaches
     *       {@code Weekday.valueOf} and throws, or the DB CHECK constraint rejects it</li>
     * </ul>
     */
    @Test
    void offerings_confirm_persists_draft_offering_with_monday_session() {
        // ---- Arrange: fresh program (opaque ref, no FK required in offering schema) ----
        UUID programId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO catalog.academic_program (id, name, faculty, status, version) VALUES (?, ?, 'Facultad', 'ACTIVE', 0)",
                programId, "OfferingIT-Prog-" + UUID.randomUUID());

        UUID planId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO catalog.study_plan (id, program_id, name, status, total_credits, version) VALUES (?, ?, ?, 'ACTIVE', 100, 0)",
                planId, programId, "OfferingIT-Plan-" + UUID.randomUUID());

        // Seed a real course so GetCourseQuery can resolve the courseName
        UUID courseId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO curriculum.course (id, plan_id, program_id, name, type, credits, curricular_weekly_hours, status, version) VALUES (?, ?, ?, ?, 'REQUIRED', 3, 3, 'ACTIVE', 0)",
                courseId, planId, programId, "OfferingIT Course");

        // Seed a real academic period so the offering.period_id FK is satisfied
        UUID periodId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO offering.academic_period (id, name, status, version) VALUES (?, ?, 'UPCOMING', 0)",
                periodId, "OfferingIT-Period-" + UUID.randomUUID());

        // Seed a VALIDATED OFFERINGS ImportBatch with one row: day = "MON"
        UUID importId = UUID.randomUUID();
        ImportBatchId batchId = ImportBatchId.of(importId);

        List<ImportRow> rows = List.of(
                new ImportRow(0, Map.of(
                        "course", courseId.toString(),
                        "option_label", "G1",
                        "journey", "DIURNA",
                        "day", "MON",
                        "start_time", "08:00",
                        "end_time", "10:00",
                        "status", "ACTIVE")));

        ImportBatch batch = ImportBatch.reconstitute(
                batchId,
                ImportKind.OFFERINGS,
                programId,
                null,
                periodId,
                ImportStatus.VALIDATED,
                1, 1,
                List.of(),
                rows,
                ImportBatch.CURRENT_SCHEMA_VERSION,
                0L,
                Instant.now());

        saveImportBatchPort.save(batch);
        assertThat(loadImportBatchPort.findById(batchId)).isPresent();

        ConfirmImportCommand command = new ConfirmImportCommand(
                importId,
                "admin@test.com",
                "ADMIN",
                UUID.randomUUID(),
                0L,
                UUID.randomUUID(),
                "offering-it-hash-" + UUID.randomUUID());

        // ---- Act -------------------------------------------------------------------
        ImportSummary result = confirmImportUseCase.confirm(command);

        // ---- Assert (a): confirm succeeds — batch is CONFIRMED ---------------------
        assertThat(result.status())
                .as("(a) confirm() must return CONFIRMED status")
                .isEqualTo("CONFIRMED");

        Optional<ImportBatch> reloaded = loadImportBatchPort.findById(batchId);
        assertThat(reloaded).isPresent();
        assertThat(reloaded.get().getStatus())
                .as("(a) ImportBatch must be CONFIRMED in DB after successful confirm")
                .isEqualTo(ImportStatus.CONFIRMED);

        // ---- Assert (b): an offering was persisted ---------------------------------
        Integer offeringCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM offering.course_offering WHERE course_id = ? AND program_id = ? AND period_id = ?",
                Integer.class,
                courseId, programId, periodId);
        assertThat(offeringCount)
                .as("(b) Exactly one CourseOffering must be persisted for the imported row")
                .isEqualTo(1);

        // ---- Assert (c): offering status is DRAFT (NFR-003 load-bearing) ----------
        // CourseOffering.create() hardcodes DRAFT. This assertion fails if that invariant breaks.
        String offeringStatus = jdbcTemplate.queryForObject(
                "SELECT status FROM offering.course_offering WHERE course_id = ? AND program_id = ? AND period_id = ?",
                String.class,
                courseId, programId, periodId);
        assertThat(offeringStatus)
                .as("(c) NFR-003: persisted offering status MUST be DRAFT — domain invariant")
                .isEqualTo("DRAFT");

        // ---- Assert (d): session day is MONDAY (proves expandDay translation) -----
        // Without expandDay: Weekday.valueOf("MON") throws IllegalArgumentException before
        // persist — confirm() fails and this assertion is never reached.
        // With DB CHECK constraint: even if valueOf() somehow passed, inserting "MON" into
        // offering.schedule_session.day would violate the CHECK constraint.
        String sessionDay = jdbcTemplate.queryForObject(
                "SELECT ss.day FROM offering.schedule_session ss "
                + "JOIN offering.schedule_option so ON so.id = ss.schedule_option_id "
                + "JOIN offering.course_offering co ON co.id = so.offering_id "
                + "WHERE co.course_id = ? AND co.program_id = ? AND co.period_id = ?",
                String.class,
                courseId, programId, periodId);
        assertThat(sessionDay)
                .as("(d) Session day MUST be 'MONDAY' — expandDay must translate 'MON' → 'MONDAY'")
                .isEqualTo("MONDAY");
    }

    // -------------------------------------------------------------------------
    // WU4-IT1 — Multi-session grouping keystone: 3 rows → 1 offering, 1 option, 3 sessions
    //   (QAL-IMP-001 / DEBT-F6-002 — THE load-bearing DB-count proof)
    // -------------------------------------------------------------------------

    /**
     * THE keystone test for DEBT-F6-002 fix (QAL-IMP-001-S1).
     *
     * <p>Seeds an OFFERINGS batch with 3 rows for the same course/option_label on MON, WED, FRI.
     * Drives the REAL ConfirmImportService + REAL CreateOfferingService + REAL Testcontainers DB.
     * No {@code @MockitoBean} on any use-case.
     *
     * <p>Before the fix: 3 offerings, 3 schedule_options, 3 sessions.
     * After the fix: 1 offering, 1 schedule_option, 3 sessions.
     *
     * <p>A mock-based test that stubs {@code CreateOfferingUseCase} would be fake-green before
     * and after the fix, since it never actually writes to the DB. Only this real-stack test
     * distinguishes the fixed from the unfixed state.
     */
    @Test
    void multi_session_offering_grouped_into_one_course_offering_with_three_sessions() {
        // ---- Arrange: fresh program, plan, course, and academic period --------
        UUID programId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO catalog.academic_program (id, name, faculty, status, version) VALUES (?, ?, 'Facultad', 'ACTIVE', 0)",
                programId, "WU4-IT1-Prog-" + UUID.randomUUID());

        UUID planId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO catalog.study_plan (id, program_id, name, status, total_credits, version) VALUES (?, ?, ?, 'ACTIVE', 100, 0)",
                planId, programId, "WU4-IT1-Plan-" + UUID.randomUUID());

        UUID courseId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO curriculum.course (id, plan_id, program_id, name, type, credits, curricular_weekly_hours, status, version) VALUES (?, ?, ?, ?, 'REQUIRED', 3, 3, 'ACTIVE', 0)",
                courseId, planId, programId, "WU4-IT1 Course");

        UUID periodId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO offering.academic_period (id, name, status, version) VALUES (?, ?, 'UPCOMING', 0)",
                periodId, "WU4-IT1-Period-" + UUID.randomUUID());

        // Seed a VALIDATED OFFERINGS ImportBatch with 3 rows:
        // same course + option_label "G1" on MON, WED, FRI (3 weekly sessions of one offering)
        UUID importId = UUID.randomUUID();
        ImportBatchId batchId = ImportBatchId.of(importId);

        List<ImportRow> rows = List.of(
                new ImportRow(0, Map.of(
                        "course", courseId.toString(),
                        "option_label", "G1",
                        "journey", "DIURNA",
                        "day", "MON",
                        "start_time", "08:00",
                        "end_time", "10:00",
                        "status", "ACTIVE")),
                new ImportRow(1, Map.of(
                        "course", courseId.toString(),
                        "option_label", "G1",
                        "journey", "DIURNA",
                        "day", "WED",
                        "start_time", "08:00",
                        "end_time", "10:00",
                        "status", "ACTIVE")),
                new ImportRow(2, Map.of(
                        "course", courseId.toString(),
                        "option_label", "G1",
                        "journey", "DIURNA",
                        "day", "FRI",
                        "start_time", "08:00",
                        "end_time", "10:00",
                        "status", "ACTIVE")));

        ImportBatch batch = ImportBatch.reconstitute(
                batchId,
                ImportKind.OFFERINGS,
                programId,
                null,
                periodId,
                ImportStatus.VALIDATED,
                3, 3,
                List.of(),
                rows,
                ImportBatch.CURRENT_SCHEMA_VERSION,
                0L,
                Instant.now());

        saveImportBatchPort.save(batch);
        assertThat(loadImportBatchPort.findById(batchId)).isPresent();

        ConfirmImportCommand command = new ConfirmImportCommand(
                importId,
                "admin@test.com",
                "ADMIN",
                UUID.randomUUID(),
                0L,
                UUID.randomUUID(),
                "wu4-it1-hash-" + UUID.randomUUID());

        // ---- Act ---------------------------------------------------------------
        ImportSummary result = confirmImportUseCase.confirm(command);

        // ---- Assert: confirm succeeds ------------------------------------------
        assertThat(result.status())
                .as("confirm() must return CONFIRMED status")
                .isEqualTo("CONFIRMED");

        // ---- THE LOAD-BEARING DB-COUNT ASSERTIONS (QAL-IMP-001-S1) -------------
        // Before fix: 3 course_offering rows, 3 schedule_option rows, 3 schedule_session rows.
        // After fix:  1 course_offering row,  1 schedule_option row,  3 schedule_session rows.

        Integer offeringCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM offering.course_offering WHERE course_id = ? AND program_id = ? AND period_id = ?",
                Integer.class,
                courseId, programId, periodId);
        assertThat(offeringCount)
                .as("(DB-1) MUST be exactly 1 course_offering — 3 CSV rows for the same " +
                    "course/option_label must be GROUPED into one offering (DEBT-F6-002 fix). " +
                    "If this is 3, the grouping is NOT implemented.")
                .isEqualTo(1);

        Integer optionCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM offering.schedule_option so " +
                "JOIN offering.course_offering co ON co.id = so.offering_id " +
                "WHERE co.course_id = ? AND co.program_id = ? AND co.period_id = ?",
                Integer.class,
                courseId, programId, periodId);
        assertThat(optionCount)
                .as("(DB-2) MUST be exactly 1 schedule_option — one option 'G1' containing all sessions. " +
                    "If this is 3, each row incorrectly created its own option.")
                .isEqualTo(1);

        Integer sessionCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM offering.schedule_session ss " +
                "JOIN offering.schedule_option so ON so.id = ss.schedule_option_id " +
                "JOIN offering.course_offering co ON co.id = so.offering_id " +
                "WHERE co.course_id = ? AND co.program_id = ? AND co.period_id = ?",
                Integer.class,
                courseId, programId, periodId);
        assertThat(sessionCount)
                .as("(DB-3) MUST be exactly 3 schedule_session rows — MON, WED, FRI sessions. " +
                    "All three CSV rows must contribute one session each to the single offering.")
                .isEqualTo(3);

        // Verify the three expected weekdays are all present
        List<String> sessionDays = jdbcTemplate.queryForList(
                "SELECT ss.day FROM offering.schedule_session ss " +
                "JOIN offering.schedule_option so ON so.id = ss.schedule_option_id " +
                "JOIN offering.course_offering co ON co.id = so.offering_id " +
                "WHERE co.course_id = ? AND co.program_id = ? AND co.period_id = ? " +
                "ORDER BY ss.day",
                String.class,
                courseId, programId, periodId);
        assertThat(sessionDays)
                .as("(DB-4) The three sessions must be MONDAY, WEDNESDAY, FRIDAY (expandDay applied)")
                .containsExactlyInAnyOrder("FRIDAY", "MONDAY", "WEDNESDAY");
    }

    // -------------------------------------------------------------------------
    // WU4-IT2 — Atomicity with multi-session grouping: invalid offering rolls back ALL
    //   (QAL-IMP-001-S2)
    // -------------------------------------------------------------------------

    /**
     * Atomicity keystone: a batch where one row refers to a non-existent course UUID causes
     * {@code CreateOfferingService} to fail (course not found → exception) and rolls back
     * ALL offering writes within the transaction — COUNT == 0 in the DB.
     *
     * <p>Seeded: one valid 2-session offering (courseValid / G1 on MON+WED) + one row with a
     * random UUID that does not exist in {@code curriculum.course}. The valid group would create
     * a course_offering if committed, but the invalid group failure rolls back everything.
     *
     * <p>This is load-bearing: without {@code @Transactional} on {@code ConfirmImportService.confirm},
     * the valid offering would be committed before the invalid row throws — COUNT would be 1 instead
     * of 0.
     */
    @Test
    void atomicity_invalid_offering_row_rolls_back_all_offering_writes() {
        // ---- Arrange: fresh program, plan, ONE valid course, ONE missing course UUID ---
        UUID programId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO catalog.academic_program (id, name, faculty, status, version) VALUES (?, ?, 'Facultad', 'ACTIVE', 0)",
                programId, "WU4-IT2-Prog-" + UUID.randomUUID());

        UUID planId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO catalog.study_plan (id, program_id, name, status, total_credits, version) VALUES (?, ?, ?, 'ACTIVE', 100, 0)",
                planId, programId, "WU4-IT2-Plan-" + UUID.randomUUID());

        UUID courseValidId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO curriculum.course (id, plan_id, program_id, name, type, credits, curricular_weekly_hours, status, version) VALUES (?, ?, ?, ?, 'REQUIRED', 3, 3, 'ACTIVE', 0)",
                courseValidId, planId, programId, "WU4-IT2 Valid Course");

        UUID periodId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO offering.academic_period (id, name, status, version) VALUES (?, ?, 'UPCOMING', 0)",
                periodId, "WU4-IT2-Period-" + UUID.randomUUID());

        // courseInvalidId: a random UUID that does NOT exist in curriculum.course
        // CreateOfferingService calls GetCourseQuery to resolve the course name — this will fail.
        UUID courseInvalidId = UUID.randomUUID();

        UUID importId = UUID.randomUUID();
        ImportBatchId batchId = ImportBatchId.of(importId);

        // Batch: 2 rows for the valid course (G1 / MON + WED) + 1 row for the missing course (G1 / FRI)
        // After grouping: 2 groups. Group 1 (courseValidId/G1) has 2 sessions; group 2 (courseInvalidId/G1)
        // has 1 session. Group 2's createOffering call will fail (course not found).
        List<ImportRow> rows = List.of(
                new ImportRow(0, Map.of(
                        "course", courseValidId.toString(),
                        "option_label", "G1",
                        "journey", "DIURNA",
                        "day", "MON",
                        "start_time", "08:00",
                        "end_time", "10:00",
                        "status", "ACTIVE")),
                new ImportRow(1, Map.of(
                        "course", courseValidId.toString(),
                        "option_label", "G1",
                        "journey", "DIURNA",
                        "day", "WED",
                        "start_time", "08:00",
                        "end_time", "10:00",
                        "status", "ACTIVE")),
                new ImportRow(2, Map.of(
                        "course", courseInvalidId.toString(),  // missing course — will throw
                        "option_label", "G2",
                        "journey", "NOCTURNA",
                        "day", "FRI",
                        "start_time", "18:00",
                        "end_time", "20:00",
                        "status", "ACTIVE")));

        ImportBatch batch = ImportBatch.reconstitute(
                batchId,
                ImportKind.OFFERINGS,
                programId,
                null,
                periodId,
                ImportStatus.VALIDATED,
                3, 3,
                List.of(),
                rows,
                ImportBatch.CURRENT_SCHEMA_VERSION,
                0L,
                Instant.now());

        saveImportBatchPort.save(batch);
        assertThat(loadImportBatchPort.findById(batchId)).isPresent();

        ConfirmImportCommand command = new ConfirmImportCommand(
                importId,
                "admin@test.com",
                "ADMIN",
                UUID.randomUUID(),
                0L,
                UUID.randomUUID(),
                "wu4-it2-hash-" + UUID.randomUUID());

        // ---- Act + Assert: exception propagates --------------------------------
        // CreateOfferingService must throw when the course UUID is not found in curriculum.
        // The exception propagates out of confirm(), rolling back the entire TX.
        org.assertj.core.api.Assertions.assertThatThrownBy(
                () -> confirmImportUseCase.confirm(command))
                .as("Confirm must throw when a row references a missing course — " +
                    "all-or-nothing (ADR-F6-001, QAL-IMP-001-S2)")
                .isInstanceOf(Exception.class);

        // ---- THE ATOMICITY ASSERTION: ZERO offering rows in DB -----------------
        // If @Transactional is missing or REQUIRES_NEW is used, the valid group (courseValidId/G1)
        // would have committed BEFORE the invalid group failed — count would be 1.
        // COUNT == 0 proves the full-TX rollback is intact after the grouping refactor.
        Integer offeringCountAfterRollback = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM offering.course_offering WHERE program_id = ? AND period_id = ?",
                Integer.class,
                programId, periodId);
        assertThat(offeringCountAfterRollback)
                .as("(ATOMICITY) ZERO course_offering rows must survive after rollback. " +
                    "The valid group was written inside the same TX as the invalid group — " +
                    "the rollback must erase both. If this is 1, the TX boundary is broken.")
                .isEqualTo(0);

        // Batch must remain VALIDATED (not CONFIRMED)
        Optional<ImportBatch> reloaded = loadImportBatchPort.findById(batchId);
        assertThat(reloaded).isPresent();
        assertThat(reloaded.get().getStatus())
                .as("ImportBatch must remain VALIDATED after rollback (not CONFIRMED)")
                .isEqualTo(ImportStatus.VALIDATED);
    }
}
