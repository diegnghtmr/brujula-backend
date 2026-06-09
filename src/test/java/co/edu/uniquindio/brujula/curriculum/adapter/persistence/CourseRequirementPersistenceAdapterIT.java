package co.edu.uniquindio.brujula.curriculum.adapter.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import co.edu.uniquindio.brujula.AbstractPostgresIntegrationTest;
import co.edu.uniquindio.brujula.curriculum.application.port.out.LoadCourseRequirementPort;
import co.edu.uniquindio.brujula.curriculum.application.port.out.SaveCoursePort;
import co.edu.uniquindio.brujula.curriculum.application.port.out.SaveCourseRequirementPort;
import co.edu.uniquindio.brujula.curriculum.domain.Course;
import co.edu.uniquindio.brujula.curriculum.domain.CourseId;
import co.edu.uniquindio.brujula.curriculum.domain.CourseRequirement;
import co.edu.uniquindio.brujula.curriculum.domain.CourseRequirementId;
import co.edu.uniquindio.brujula.curriculum.domain.CourseRequirementStatus;
import co.edu.uniquindio.brujula.curriculum.domain.CourseStatus;
import co.edu.uniquindio.brujula.curriculum.domain.CourseType;
import co.edu.uniquindio.brujula.curriculum.domain.RequirementType;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * TDD T5.3 — Persistence round-trip integration tests for {@code CourseRequirementPersistenceAdapter}.
 *
 * <p>Uses the shared Testcontainers PostgreSQL via {@link AbstractPostgresIntegrationTest}.
 * Tests verify:
 * <ul>
 *   <li>save + load round-trips all requirement fields including version
 *   <li>the prerequisite graph (findAllByPlanId) reads back correctly for acyclicity checks
 *   <li>version bumps on update (load-bearing proof of @Version on requirements)
 * </ul>
 *
 * Satisfies: CUR-003, IMP-007, SC-009-C, WU5-T5.3.
 */
@SpringBootTest
@Transactional
class CourseRequirementPersistenceAdapterIT extends AbstractPostgresIntegrationTest {

    @Autowired
    private SaveCourseRequirementPort saveRequirementPort;

    @Autowired
    private SaveCoursePort saveCoursePort;

    @Autowired
    private LoadCourseRequirementPort loadCourseRequirementPort;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @PersistenceContext
    private EntityManager entityManager;

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private UUID insertStudyPlan(String name, int credits) {
        UUID programId = UUID.randomUUID();
        jdbcTemplate.update(
            "INSERT INTO catalog.academic_program (id, name, faculty, status, version) VALUES (?, ?, 'Facultad', 'ACTIVE', 0)",
            programId, "Prog-" + UUID.randomUUID());
        UUID planId = UUID.randomUUID();
        jdbcTemplate.update(
            "INSERT INTO catalog.study_plan (id, program_id, name, status, total_credits, version) VALUES (?, ?, ?, 'ACTIVE', ?, 0)",
            planId, programId, name, credits);
        return planId;
    }

    private UUID insertProgramForPlan(UUID planId) {
        return jdbcTemplate.queryForObject(
            "SELECT program_id FROM catalog.study_plan WHERE id = ?", UUID.class, planId);
    }

    /** Save a course via port and return its CourseId. Flushes but does not clear L1 cache. */
    private CourseId saveCourse(UUID planId, UUID programId, String name) {
        CourseId courseId = CourseId.generate();
        Course course = Course.reconstitute(
            courseId, planId, programId, null,
            name, CourseType.REQUIRED, 3, 3, List.of(), CourseStatus.ACTIVE, 0L);
        saveCoursePort.save(course);
        return courseId;
    }

    // -------------------------------------------------------------------------
    // T5.3-1: save + load round-trips all fields (incl. version = 0)
    // -------------------------------------------------------------------------

    @Test
    void save_and_load_requirement_round_trips_all_fields() {
        // ARRANGE: create two courses in the same plan
        UUID planId = insertStudyPlan("Plan Req IT", 160);
        UUID programId = insertProgramForPlan(planId);
        CourseId targetId = saveCourse(planId, programId, "Cálculo II");
        CourseId requiredId = saveCourse(planId, programId, "Cálculo I");
        entityManager.flush();
        entityManager.clear();

        CourseRequirementId reqId = CourseRequirementId.generate();
        CourseRequirement req = CourseRequirement.reconstitute(
            reqId,
            targetId.value(),
            RequirementType.COURSE_PASSED,
            requiredId.value(),
            null,
            false,
            null,
            null,
            CourseRequirementStatus.ACTIVE,
            0L);

        // ACT
        CourseRequirement saved = saveRequirementPort.save(req);
        entityManager.flush();
        entityManager.clear();

        Optional<CourseRequirement> loaded =
            saveRequirementPort.findCurrentById(reqId);

        // ASSERT — load-bearing: each field must persist and round-trip
        assertThat(loaded).isPresent();
        CourseRequirement r = loaded.get();
        assertThat(r.id()).isEqualTo(reqId);
        assertThat(r.targetCourseId()).isEqualTo(targetId.value());
        assertThat(r.type()).isEqualTo(RequirementType.COURSE_PASSED);
        assertThat(r.requiredCourseId()).isEqualTo(requiredId.value());
        assertThat(r.simultaneityAllowed()).isFalse();
        assertThat(r.status()).isEqualTo(CourseRequirementStatus.ACTIVE);
        // Load-bearing: @Version MUST start at 0 after insert
        assertThat(saved.version()).isEqualTo(0L);
    }

    // -------------------------------------------------------------------------
    // T5.3-2: findAllByPlanId reads back the prerequisite graph correctly
    //         (load-bearing for acyclicity checks in CreateCourseRequirementService)
    // -------------------------------------------------------------------------

    @Test
    void findAllByPlanId_returns_all_requirements_for_acyclicity_check() {
        // ARRANGE: plan with A→B and B→C requirements
        UUID planId = insertStudyPlan("Plan Graph IT", 160);
        UUID programId = insertProgramForPlan(planId);
        CourseId courseA = saveCourse(planId, programId, "Curso A");
        CourseId courseB = saveCourse(planId, programId, "Curso B");
        CourseId courseC = saveCourse(planId, programId, "Curso C");
        entityManager.flush();
        entityManager.clear();

        // A is required for B (A→B edge)
        CourseRequirement reqAtoB = CourseRequirement.reconstitute(
            CourseRequirementId.generate(), courseB.value(),
            RequirementType.COURSE_PASSED, courseA.value(),
            null, false, null, null, CourseRequirementStatus.ACTIVE, 0L);
        // B is required for C (B→C edge)
        CourseRequirement reqBtoC = CourseRequirement.reconstitute(
            CourseRequirementId.generate(), courseC.value(),
            RequirementType.COURSE_PASSED, courseB.value(),
            null, false, null, null, CourseRequirementStatus.ACTIVE, 0L);

        saveRequirementPort.save(reqAtoB);
        saveRequirementPort.save(reqBtoC);
        entityManager.flush();
        entityManager.clear();

        // ACT — this is the method used by CreateCourseRequirementService for cycle detection
        List<CourseRequirement> allInPlan = saveRequirementPort.findAllByPlanId(planId);

        // ASSERT — load-bearing: exactly 2 requirements must be returned
        assertThat(allInPlan).hasSize(2);
        assertThat(allInPlan).anySatisfy(r -> {
            assertThat(r.targetCourseId()).isEqualTo(courseB.value());
            assertThat(r.requiredCourseId()).isEqualTo(courseA.value());
        });
        assertThat(allInPlan).anySatisfy(r -> {
            assertThat(r.targetCourseId()).isEqualTo(courseC.value());
            assertThat(r.requiredCourseId()).isEqualTo(courseB.value());
        });
    }

    // -------------------------------------------------------------------------
    // T5.3-3: findAllByPlanId does NOT return requirements from another plan
    // -------------------------------------------------------------------------

    @Test
    void findAllByPlanId_excludes_requirements_from_other_plans() {
        // ARRANGE: two separate plans, each with one requirement
        UUID planA = insertStudyPlan("Plan A", 160);
        UUID programA = insertProgramForPlan(planA);
        UUID planB = insertStudyPlan("Plan B", 160);
        UUID programB = insertProgramForPlan(planB);

        CourseId courseA1 = saveCourse(planA, programA, "Plan A Course 1");
        CourseId courseA2 = saveCourse(planA, programA, "Plan A Course 2");
        CourseId courseB1 = saveCourse(planB, programB, "Plan B Course 1");
        CourseId courseB2 = saveCourse(planB, programB, "Plan B Course 2");
        entityManager.flush();
        entityManager.clear();

        CourseRequirement reqA = CourseRequirement.reconstitute(
            CourseRequirementId.generate(), courseA2.value(),
            RequirementType.COURSE_PASSED, courseA1.value(),
            null, false, null, null, CourseRequirementStatus.ACTIVE, 0L);
        CourseRequirement reqB = CourseRequirement.reconstitute(
            CourseRequirementId.generate(), courseB2.value(),
            RequirementType.COURSE_PASSED, courseB1.value(),
            null, false, null, null, CourseRequirementStatus.ACTIVE, 0L);
        saveRequirementPort.save(reqA);
        saveRequirementPort.save(reqB);
        entityManager.flush();
        entityManager.clear();

        // Load-bearing: planA query must return ONLY planA's requirement
        List<CourseRequirement> planAReqs = saveRequirementPort.findAllByPlanId(planA);
        assertThat(planAReqs).hasSize(1);
        assertThat(planAReqs.get(0).targetCourseId()).isEqualTo(courseA2.value());
    }

    // -------------------------------------------------------------------------
    // T5.3-4: version bumps on update (load-bearing proof of @Version on requirements)
    // -------------------------------------------------------------------------

    @Test
    void version_increments_on_update() {
        UUID planId = insertStudyPlan("Plan Version Req", 160);
        UUID programId = insertProgramForPlan(planId);
        CourseId target = saveCourse(planId, programId, "Target Course");
        CourseId required = saveCourse(planId, programId, "Required Course");
        entityManager.flush();
        entityManager.clear();

        CourseRequirementId reqId = CourseRequirementId.generate();
        CourseRequirement initial = CourseRequirement.reconstitute(
            reqId, target.value(),
            RequirementType.COURSE_PASSED, required.value(),
            null, false, null, null, CourseRequirementStatus.ACTIVE, 0L);

        CourseRequirement afterInsert = saveRequirementPort.save(initial);
        entityManager.flush();
        entityManager.clear();

        // Load-bearing: initial @Version must be 0
        assertThat(afterInsert.version()).isEqualTo(0L);

        // Update with matching expectedVersion=0 → DB bumps to 1
        CourseRequirement toUpdate = CourseRequirement.reconstitute(
            reqId, target.value(),
            RequirementType.SEQUENCE, required.value(),
            null, true, null, null, CourseRequirementStatus.ACTIVE, 0L);

        CourseRequirement afterUpdate = saveRequirementPort.save(toUpdate);
        entityManager.flush();
        entityManager.clear();

        // Load-bearing: @Version MUST be 1 — if @Version is missing, stays 0
        assertThat(afterUpdate.version()).isEqualTo(1L);

        Optional<CourseRequirement> loaded = saveRequirementPort.findCurrentById(reqId);
        assertThat(loaded).isPresent();
        assertThat(loaded.get().version()).isEqualTo(1L);
        assertThat(loaded.get().type()).isEqualTo(RequirementType.SEQUENCE);
        assertThat(loaded.get().simultaneityAllowed()).isTrue();
    }
}
