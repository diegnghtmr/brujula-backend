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
 * End-to-end integration test for OFFERINGS import confirm (T7.2 — the real-path keystone proof).
 *
 * <p>Uses the REAL {@code ConfirmImportService} wired with the REAL {@code CreateOfferingService}
 * (no {@code @MockitoBean} on any use-case). Drives against a live Testcontainers Postgres
 * instance via {@link AbstractPostgresIntegrationTest}.
 *
 * <p><b>What this test proves</b> (and why it is load-bearing):
 * <ol>
 *   <li>(a) Confirm succeeds — batch status becomes CONFIRMED in DB.</li>
 *   <li>(b) An offering row is persisted in {@code offering.course_offering}.</li>
 *   <li>(c) The persisted offering status is {@code DRAFT} (NFR-003 load-bearing: proves the real
 *       domain aggregate enforces DRAFT at creation, NOT just a mock echo).</li>
 *   <li>(d) The persisted session {@code day} is {@code "MONDAY"} — proves the
 *       {@code expandDay("MON" → "MONDAY")} translation in {@code replayOfferingRow} works
 *       end-to-end. <strong>Without the {@code expandDay} fix this assertion FAILS</strong>:
 *       {@code Weekday.valueOf("MON")} throws {@link IllegalArgumentException} (or the DB CHECK
 *       constraint rejects the value) before any offering is persisted.</li>
 * </ol>
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
}
