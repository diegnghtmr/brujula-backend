package co.edu.uniquindio.brujula.curriculum.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.edu.uniquindio.brujula.audit.application.port.in.AuditRecorder;
import co.edu.uniquindio.brujula.curriculum.application.dto.CourseRequirementView;
import co.edu.uniquindio.brujula.curriculum.application.dto.UpdateCourseRequirementCommand;
import co.edu.uniquindio.brujula.curriculum.application.port.out.LoadCoursePort;
import co.edu.uniquindio.brujula.curriculum.application.port.out.SaveCourseRequirementPort;
import co.edu.uniquindio.brujula.curriculum.application.service.UpdateCourseRequirementService;
import co.edu.uniquindio.brujula.curriculum.domain.Course;
import co.edu.uniquindio.brujula.curriculum.domain.CourseId;
import co.edu.uniquindio.brujula.curriculum.domain.CourseRequirement;
import co.edu.uniquindio.brujula.curriculum.domain.CourseRequirementId;
import co.edu.uniquindio.brujula.curriculum.domain.CourseRequirementNotFoundException;
import co.edu.uniquindio.brujula.curriculum.domain.CourseRequirementStatus;
import co.edu.uniquindio.brujula.curriculum.domain.CourseStatus;
import co.edu.uniquindio.brujula.curriculum.domain.CourseType;
import co.edu.uniquindio.brujula.curriculum.domain.PrerequisiteCycleException;
import co.edu.uniquindio.brujula.curriculum.domain.RequirementType;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.OptimisticLockingFailureException;

/**
 * TDD RED → GREEN: unit tests for {@link UpdateCourseRequirementService}.
 *
 * <p>Load-bearing tests: each assertion will fail if the behaviour it covers is removed or broken.
 * Covers: requirement not found → CourseRequirementNotFoundException; optimistic lock propagates;
 * success → audit recorded + view reflects update; cycle detection on update path (IMP-007 gap fix).
 *
 * WU5 – T5.2 fix (CUR-003, IMP-007, SC-009-C, DEBT-F6-001 verification).
 */
class UpdateCourseRequirementServiceTest {

    private SaveCourseRequirementPort saveRequirementPort;
    private LoadCoursePort loadCoursePort;
    private AuditRecorder auditRecorder;
    private UpdateCourseRequirementService service;

    private final UUID actorId = UUID.randomUUID();
    private final UUID planId = UUID.randomUUID();
    private final UUID targetCourseId = UUID.randomUUID();
    private final UUID requiredCourseId = UUID.randomUUID();
    private final UUID newRequiredCourseId = UUID.randomUUID();
    private final CourseRequirementId reqId = CourseRequirementId.generate();

    @BeforeEach
    void setUp() {
        saveRequirementPort = mock(SaveCourseRequirementPort.class);
        loadCoursePort = mock(LoadCoursePort.class);
        auditRecorder = mock(AuditRecorder.class);
        service = new UpdateCourseRequirementService(saveRequirementPort, loadCoursePort, auditRecorder);
    }

    // Helper: build a stub Course with the given planId and courseId
    private Course stubCourse(UUID courseId, UUID coursePlanId) {
        return Course.reconstitute(
            new CourseId(courseId), coursePlanId, UUID.randomUUID(),
            null, "Stub Course", CourseType.REQUIRED,
            3, 3, Collections.emptyList(),
            CourseStatus.ACTIVE, 0L);
    }

    // -------------------------------------------------------------------------
    // Happy path: update fields, audit recorded, view reflects updated data
    // -------------------------------------------------------------------------

    @Test
    void updates_requirement_and_records_audit() {
        // ARRANGE
        CourseRequirement existing = CourseRequirement.reconstitute(
            reqId, targetCourseId,
            RequirementType.COURSE_PASSED, requiredCourseId,
            null, false, null, null, CourseRequirementStatus.ACTIVE, 1L);
        when(saveRequirementPort.findCurrentById(reqId)).thenReturn(Optional.of(existing));
        // Stub cycle-detection graph loading (no other requirements — acyclicity trivially satisfied)
        when(loadCoursePort.loadById(new CourseId(targetCourseId)))
            .thenReturn(Optional.of(stubCourse(targetCourseId, planId)));
        when(saveRequirementPort.findAllByPlanId(planId)).thenReturn(List.of(existing));

        CourseRequirement saved = CourseRequirement.reconstitute(
            reqId, targetCourseId,
            RequirementType.SEQUENCE, newRequiredCourseId,
            null, true, null, null, CourseRequirementStatus.ACTIVE, 2L);
        when(saveRequirementPort.save(any(CourseRequirement.class))).thenReturn(saved);

        UpdateCourseRequirementCommand command = new UpdateCourseRequirementCommand(
            actorId, reqId.value(), 1L, "SEQUENCE", newRequiredCourseId, null, true);

        // ACT
        CourseRequirementView result = service.updateCourseRequirement(command);

        // ASSERT — load-bearing: wrong type or null course returns wrong view
        assertThat(result.type()).isEqualTo("SEQUENCE");
        assertThat(result.requiredCourseId()).isEqualTo(newRequiredCourseId);
        assertThat(result.simultaneityAllowed()).isTrue();
        // Load-bearing FIX 1: version must be present in view
        assertThat(result.version()).isEqualTo(2L);

        // Load-bearing: audit MUST be recorded; if removed, this fails
        verify(auditRecorder).record(argThat(cmd ->
            "COURSE_REQUIREMENT_UPDATED".equals(cmd.action())));

        // Load-bearing: save MUST be called with the reconstituted entity using expectedVersion
        verify(saveRequirementPort).save(argThat(req ->
            req.id().equals(reqId) && req.version() == 1L));
    }

    @Test
    void updates_with_null_fields_falls_back_to_existing_values() {
        // ARRANGE — command sends all-null changeable fields; existing is COURSE_PASSED so cycle
        // check fires (newType=COURSE_PASSED, newRequiredCourseId=requiredCourseId — no-op edge)
        CourseRequirement existing = CourseRequirement.reconstitute(
            reqId, targetCourseId,
            RequirementType.COURSE_PASSED, requiredCourseId,
            null, false, null, null, CourseRequirementStatus.ACTIVE, 2L);
        when(saveRequirementPort.findCurrentById(reqId)).thenReturn(Optional.of(existing));
        when(loadCoursePort.loadById(new CourseId(targetCourseId)))
            .thenReturn(Optional.of(stubCourse(targetCourseId, planId)));
        // Only the existing requirement is in the graph; after excluding it the graph is empty
        when(saveRequirementPort.findAllByPlanId(planId)).thenReturn(List.of(existing));

        // Save returns same data as existing (no-op update)
        CourseRequirement saved = CourseRequirement.reconstitute(
            reqId, targetCourseId,
            RequirementType.COURSE_PASSED, requiredCourseId,
            null, false, null, null, CourseRequirementStatus.ACTIVE, 3L);
        when(saveRequirementPort.save(any(CourseRequirement.class))).thenReturn(saved);

        UpdateCourseRequirementCommand command = new UpdateCourseRequirementCommand(
            actorId, reqId.value(), 2L, null, null, null, null);

        // ACT
        CourseRequirementView result = service.updateCourseRequirement(command);

        // ASSERT — load-bearing: null fields must NOT zero-out existing values
        assertThat(result.type()).isEqualTo("COURSE_PASSED");
        assertThat(result.requiredCourseId()).isEqualTo(requiredCourseId);
        verify(auditRecorder).record(any());
    }

    // -------------------------------------------------------------------------
    // Not found → CourseRequirementNotFoundException
    // -------------------------------------------------------------------------

    @Test
    void requirement_not_found_throws_CourseRequirementNotFoundException() {
        when(saveRequirementPort.findCurrentById(reqId)).thenReturn(Optional.empty());

        UpdateCourseRequirementCommand command = new UpdateCourseRequirementCommand(
            actorId, reqId.value(), 0L, "COURSE_PASSED", requiredCourseId, null, false);

        // Load-bearing: MUST throw; if findCurrentById contract changes to throw directly,
        // the exception type check still guards the behaviour
        assertThatThrownBy(() -> service.updateCourseRequirement(command))
            .isInstanceOf(CourseRequirementNotFoundException.class);

        verify(saveRequirementPort, never()).save(any());
        verify(auditRecorder, never()).record(any());
    }

    // -------------------------------------------------------------------------
    // Optimistic lock (stale version) → OptimisticLockingFailureException propagates
    // -------------------------------------------------------------------------

    @Test
    void stale_version_propagates_optimistic_lock_exception() {
        CourseRequirement existing = CourseRequirement.reconstitute(
            reqId, targetCourseId,
            RequirementType.COURSE_PASSED, requiredCourseId,
            null, false, null, null, CourseRequirementStatus.ACTIVE, 3L);
        when(saveRequirementPort.findCurrentById(reqId)).thenReturn(Optional.of(existing));
        when(loadCoursePort.loadById(new CourseId(targetCourseId)))
            .thenReturn(Optional.of(stubCourse(targetCourseId, planId)));
        when(saveRequirementPort.findAllByPlanId(planId)).thenReturn(List.of(existing));
        // Load-bearing: if the adapter does NOT throw on stale version, this test fails
        when(saveRequirementPort.save(any(CourseRequirement.class)))
            .thenThrow(new OptimisticLockingFailureException("Version conflict"));

        UpdateCourseRequirementCommand command = new UpdateCourseRequirementCommand(
            actorId, reqId.value(), 2L, "COURSE_PASSED", requiredCourseId, null, false);

        assertThatThrownBy(() -> service.updateCourseRequirement(command))
            .isInstanceOf(OptimisticLockingFailureException.class);

        // Audit MUST NOT be recorded when save throws
        verify(auditRecorder, never()).record(any());
    }

    // -------------------------------------------------------------------------
    // Returned view includes the ID and target course (load-bearing field checks)
    // -------------------------------------------------------------------------

    @Test
    void returned_view_exposes_id_and_target_course() {
        // MINIMUM_CREDITS type — cycle detection is skipped
        CourseRequirement existing = CourseRequirement.reconstitute(
            reqId, targetCourseId,
            RequirementType.MINIMUM_CREDITS, null,
            8, null, null, null, CourseRequirementStatus.ACTIVE, 0L);
        when(saveRequirementPort.findCurrentById(reqId)).thenReturn(Optional.of(existing));

        CourseRequirement saved = CourseRequirement.reconstitute(
            reqId, targetCourseId,
            RequirementType.MINIMUM_CREDITS, null,
            10, null, null, null, CourseRequirementStatus.ACTIVE, 1L);
        when(saveRequirementPort.save(any(CourseRequirement.class))).thenReturn(saved);

        UpdateCourseRequirementCommand command = new UpdateCourseRequirementCommand(
            actorId, reqId.value(), 0L, null, null, 10, null);

        CourseRequirementView result = service.updateCourseRequirement(command);

        // Load-bearing: id and targetCourseId MUST be propagated in the view
        assertThat(result.id()).isEqualTo(reqId.value());
        assertThat(result.targetCourseId()).isEqualTo(targetCourseId);
        assertThat(result.requiredCredits()).isEqualTo(10);
        assertThat(result.type()).isEqualTo("MINIMUM_CREDITS");
        // Load-bearing FIX 1: version present
        assertThat(result.version()).isEqualTo(1L);
    }

    // -------------------------------------------------------------------------
    // FIX 2: Cycle detection on update path (IMP-007 gap)
    // -------------------------------------------------------------------------

    /**
     * Load-bearing: existing A→B; updating a requirement to introduce B→A → PrerequisiteCycleException;
     * save is never called. If cycle detection is removed from update, this test FAILS.
     */
    @Test
    void update_introducing_cycle_throws_PrerequisiteCycleException() {
        // Setup: courseA → courseB (A is required for B)
        UUID courseA = UUID.randomUUID();
        UUID courseB = UUID.randomUUID();

        CourseRequirementId existingAbId = CourseRequirementId.generate();
        CourseRequirement existingAtoB = CourseRequirement.reconstitute(
            existingAbId, courseB,
            RequirementType.COURSE_PASSED, courseA,
            null, false, null, null, CourseRequirementStatus.ACTIVE, 0L);

        // The requirement being updated: currently MINIMUM_CREDITS (no edge)
        CourseRequirementId updatedReqId = CourseRequirementId.generate();
        CourseRequirement existingReq = CourseRequirement.reconstitute(
            updatedReqId, courseA,
            RequirementType.MINIMUM_CREDITS, null,
            8, null, null, null, CourseRequirementStatus.ACTIVE, 0L);

        when(saveRequirementPort.findCurrentById(updatedReqId)).thenReturn(Optional.of(existingReq));
        when(loadCoursePort.loadById(new CourseId(courseA)))
            .thenReturn(Optional.of(stubCourse(courseA, planId)));
        // All plan requirements: A→B and the requirement being updated
        when(saveRequirementPort.findAllByPlanId(planId))
            .thenReturn(List.of(existingAtoB, existingReq));

        // Command: change to COURSE_PASSED with requiredCourse=courseB — this creates B→A (cycle)
        UpdateCourseRequirementCommand command = new UpdateCourseRequirementCommand(
            actorId, updatedReqId.value(), 0L, "COURSE_PASSED", courseB, null, false);

        // Load-bearing: MUST throw PrerequisiteCycleException — if check is absent, save proceeds
        assertThatThrownBy(() -> service.updateCourseRequirement(command))
            .isInstanceOf(PrerequisiteCycleException.class);

        verify(saveRequirementPort, never()).save(any());
        verify(auditRecorder, never()).record(any());
    }

    /**
     * Load-bearing: a legitimate update that does NOT create a cycle must succeed.
     * A→B exists; updating a different requirement to C→A is acyclic → no exception.
     */
    @Test
    void legitimate_update_not_creating_cycle_succeeds() {
        UUID courseA = UUID.randomUUID();
        UUID courseB = UUID.randomUUID();
        UUID courseC = UUID.randomUUID();

        CourseRequirementId existingAbId = CourseRequirementId.generate();
        CourseRequirement existingAtoB = CourseRequirement.reconstitute(
            existingAbId, courseB,
            RequirementType.COURSE_PASSED, courseA,
            null, false, null, null, CourseRequirementStatus.ACTIVE, 0L);

        // The requirement being updated: currently has a different edge
        CourseRequirementId updatedReqId = CourseRequirementId.generate();
        CourseRequirement existingReq = CourseRequirement.reconstitute(
            updatedReqId, courseA,
            RequirementType.COURSE_PASSED, courseC,
            null, false, null, null, CourseRequirementStatus.ACTIVE, 1L);

        when(saveRequirementPort.findCurrentById(updatedReqId)).thenReturn(Optional.of(existingReq));
        when(loadCoursePort.loadById(new CourseId(courseA)))
            .thenReturn(Optional.of(stubCourse(courseA, planId)));
        when(saveRequirementPort.findAllByPlanId(planId))
            .thenReturn(List.of(existingAtoB, existingReq));

        CourseRequirement saved = CourseRequirement.reconstitute(
            updatedReqId, courseA,
            RequirementType.COURSE_PASSED, courseC,
            null, true, null, null, CourseRequirementStatus.ACTIVE, 2L);
        when(saveRequirementPort.save(any(CourseRequirement.class))).thenReturn(saved);

        // Update: keep same edge (C→A), just flip simultaneityAllowed — must NOT throw
        UpdateCourseRequirementCommand command = new UpdateCourseRequirementCommand(
            actorId, updatedReqId.value(), 1L, "COURSE_PASSED", courseC, null, true);

        CourseRequirementView result = service.updateCourseRequirement(command);

        // Load-bearing: if false-positive cycle detection fires, save is never called and this fails
        assertThat(result.type()).isEqualTo("COURSE_PASSED");
        verify(saveRequirementPort).save(any());
        verify(auditRecorder).record(any());
    }

    /**
     * Load-bearing no-op: updating the requirement to the SAME edge value must not be false-positive.
     * A→B exists and we're updating the same A→B requirement — old edge excluded, new edge = old edge
     * → acyclic.
     */
    @Test
    void no_op_update_same_edge_does_not_false_positive() {
        UUID courseA = UUID.randomUUID();
        UUID courseB = UUID.randomUUID();

        // The requirement being updated is itself the A→B edge
        CourseRequirementId reqIdAB = CourseRequirementId.generate();
        CourseRequirement existingReq = CourseRequirement.reconstitute(
            reqIdAB, courseB,
            RequirementType.COURSE_PASSED, courseA,
            null, false, null, null, CourseRequirementStatus.ACTIVE, 0L);

        when(saveRequirementPort.findCurrentById(reqIdAB)).thenReturn(Optional.of(existingReq));
        when(loadCoursePort.loadById(new CourseId(courseB)))
            .thenReturn(Optional.of(stubCourse(courseB, planId)));
        // Only this requirement in the plan (the one being updated)
        when(saveRequirementPort.findAllByPlanId(planId)).thenReturn(List.of(existingReq));

        CourseRequirement saved = CourseRequirement.reconstitute(
            reqIdAB, courseB,
            RequirementType.COURSE_PASSED, courseA,
            null, true, null, null, CourseRequirementStatus.ACTIVE, 1L);
        when(saveRequirementPort.save(any(CourseRequirement.class))).thenReturn(saved);

        // Update: same type and requiredCourseId (no-op edge), just change simultaneityAllowed
        UpdateCourseRequirementCommand command = new UpdateCourseRequirementCommand(
            actorId, reqIdAB.value(), 0L, "COURSE_PASSED", courseA, null, true);

        // Load-bearing: MUST NOT throw — old edge excluded, new edge = A→B, no cycle
        CourseRequirementView result = service.updateCourseRequirement(command);
        assertThat(result.type()).isEqualTo("COURSE_PASSED");
        verify(saveRequirementPort).save(any());
    }
}
