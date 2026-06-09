package co.edu.uniquindio.brujula.simulation.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import co.edu.uniquindio.brujula.simulation.application.dto.SimulationDetailView;
import co.edu.uniquindio.brujula.simulation.application.port.out.CreditLimitPort;
import co.edu.uniquindio.brujula.simulation.application.port.out.CurriculumReadPort;
import co.edu.uniquindio.brujula.simulation.application.port.out.LoadSimulationPort;
import co.edu.uniquindio.brujula.simulation.application.port.out.LoadSimulationSelectionPort;
import co.edu.uniquindio.brujula.simulation.application.port.out.OfferingReadPort;
import co.edu.uniquindio.brujula.simulation.application.port.out.ProgressReadPort;
import co.edu.uniquindio.brujula.simulation.application.port.out.ProgramNameReadPort;
import co.edu.uniquindio.brujula.simulation.domain.PrereqRequirement;
import co.edu.uniquindio.brujula.simulation.domain.Simulation;
import co.edu.uniquindio.brujula.simulation.domain.SimulationId;
import co.edu.uniquindio.brujula.simulation.domain.SimulationLifecycle;
import co.edu.uniquindio.brujula.simulation.domain.SimulationSelection;
import co.edu.uniquindio.brujula.simulation.domain.SimulationSelectionId;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * QAL-PERF-002 — Query-count regression test for {@link GetSimulationService#buildValidationSnapshot}.
 *
 * <p>TDD RED: Asserts that with 30 passed courses, {@code coursesCredits(Set)} is called
 * <strong>exactly once</strong> instead of 30 times (the O(N) per-course loop).
 *
 * <p>Uses a counting in-memory stub for {@link CurriculumReadPort} — no Spring context needed.
 * The stub counts invocations of {@code coursesCredits(Set<UUID>)} and fails the test if it is
 * called more than once, proving the batch refactor is in place.
 */
class GetSimulationServiceQueryCountTest {

    private LoadSimulationPort loadSimulationPort;
    private LoadSimulationSelectionPort loadSimulationSelectionPort;
    private OfferingReadPort offeringReadPort;
    private CreditLimitPort creditLimitPort;
    private ProgressReadPort progressReadPort;
    private CountingCurriculumReadPort curriculumReadPort;
    private ProgramNameReadPort programNameReadPort;
    private GetSimulationService sut;

    private static final UUID OWNER_ID = UUID.randomUUID();
    private static final UUID PROGRAM_ID = UUID.randomUUID();
    private static final UUID PERIOD_ID = UUID.randomUUID();
    private static final int PASSED_COURSE_COUNT = 30;

    @BeforeEach
    void setUp() {
        loadSimulationPort = mock(LoadSimulationPort.class);
        loadSimulationSelectionPort = mock(LoadSimulationSelectionPort.class);
        offeringReadPort = mock(OfferingReadPort.class);
        creditLimitPort = mock(CreditLimitPort.class);
        progressReadPort = mock(ProgressReadPort.class);
        curriculumReadPort = new CountingCurriculumReadPort();
        programNameReadPort = mock(ProgramNameReadPort.class);

        sut = new GetSimulationService(
                loadSimulationPort,
                loadSimulationSelectionPort,
                offeringReadPort,
                creditLimitPort,
                progressReadPort,
                curriculumReadPort,
                programNameReadPort);
    }

    /**
     * QAL-PERF-002-S1: 30 passed courses → batch credits method called exactly once.
     *
     * <p>This test will FAIL (RED) until {@code buildValidationSnapshot} is refactored to use
     * {@code coursesCredits(Set<UUID>)} instead of the per-course {@code courseCredits(UUID)} loop.
     */
    @Test
    void buildValidationSnapshot_withThirtyPassedCourses_callsBatchMethodExactlyOnce() {
        // Arrange: simulation with one active selection
        UUID simId = UUID.randomUUID();
        UUID selId = UUID.randomUUID();
        UUID offeringId = UUID.randomUUID();
        UUID selectedCourseId = UUID.randomUUID();

        Simulation simulation = Simulation.reconstitute(
                new SimulationId(simId), OWNER_ID, "Test Simulation", PERIOD_ID,
                List.of(PROGRAM_ID), SimulationLifecycle.ACTIVE,
                Instant.now(), Instant.now(), 0L);

        SimulationSelection selection = SimulationSelection.reconstitute(
                new SimulationSelectionId(selId), simId, offeringId, UUID.randomUUID(),
                selectedCourseId, "Test Course", PROGRAM_ID, 3.0,
                "DIURNA", "PRELIMINARY", 1L, List.of(), "ACTIVE", 0L);

        // 30 distinct passed course IDs
        Set<UUID> passedCourseIds = new java.util.HashSet<>();
        for (int i = 0; i < PASSED_COURSE_COUNT; i++) {
            passedCourseIds.add(UUID.randomUUID());
        }

        when(loadSimulationPort.loadById(simId)).thenReturn(Optional.of(simulation));
        when(loadSimulationSelectionPort.loadActiveBySimulationId(simId))
                .thenReturn(List.of(selection));
        when(offeringReadPort.resolvePeriodName(PERIOD_ID)).thenReturn("2026-2");
        when(offeringReadPort.currentOfferingVersion(offeringId)).thenReturn(1L);
        when(creditLimitPort.creditLimit(any(), any())).thenReturn(23);
        when(progressReadPort.passedCourseIds(OWNER_ID, PROGRAM_ID)).thenReturn(passedCourseIds);
        when(programNameReadPort.programName(any())).thenReturn("Systems Engineering");

        // Act — triggers buildValidationSnapshot internally
        SimulationDetailView result = sut.getSimulation(simId, OWNER_ID);

        // Assert: coursesCredits was called ONCE (not 30 times)
        assertThat(curriculumReadPort.coursesCreditsCallCount.get())
                .as("coursesCredits(Set<UUID>) must be called exactly once for 30 passed courses, "
                        + "not once-per-course. Refactor the O(N) loop.")
                .isEqualTo(1);

        // Sanity: result returned without error
        assertThat(result).isNotNull();
    }

    /**
     * QAL-PERF-002-S2: Credit-value correctness — accumulated total drives MIN_CREDITS gate.
     *
     * <p>Seeds 30 passed courses × 3 credits each = 90 accumulated credits.
     * Asserts that:
     * <ul>
     *   <li>When required minimum = 80 and accumulated = 90 → NO SIMULATION_MIN_CREDITS_NOT_MET blocker.
     *   <li>When required minimum = 100 and accumulated = 90 → blocker IS present.
     * </ul>
     *
     * <p>A latent bug that sums keys instead of values, or drops courses, would yield a total != 90,
     * causing this test to fail RED. The call-count test alone cannot catch that class of bug.
     *
     * <p>Uses {@link CountingCurriculumReadPort#requirementsForCourseId} to inject a configurable
     * requirement per course without Mockito (the stub is not a Mockito mock).
     */
    @Test
    void buildValidationSnapshot_creditSumDrivesMinCreditsGate_correctlyFlipsAroundThreshold() {
        // 30 distinct passed course IDs — each returns 3 credits → total = 90
        Set<UUID> passedCourseIds = new HashSet<>();
        for (int i = 0; i < PASSED_COURSE_COUNT; i++) {
            passedCourseIds.add(UUID.randomUUID());
        }

        // --- Scenario A: required=80, accumulated=90 → NO SIMULATION_MIN_CREDITS_NOT_MET blocker ---
        UUID simIdA = UUID.randomUUID();
        UUID courseIdA = UUID.randomUUID();
        UUID offeringIdA = UUID.randomUUID();
        UUID selIdA = UUID.randomUUID();

        PrereqRequirement minCreditsReq80 = new PrereqRequirement(
                courseIdA,
                null,               // requiredCourseId is null for MINIMUM_CREDITS type
                "MINIMUM_CREDITS",
                "HIGH",
                false,
                null,               // requiredCourseName
                80);                // threshold: 80 < 90 → gate PASSES

        // Configure the stub to return the threshold-80 requirement for courseIdA
        curriculumReadPort.requirementsForCourseId = courseIdA;
        curriculumReadPort.requirementsForCourseIdResult = List.of(minCreditsReq80);

        Simulation simA = Simulation.reconstitute(
                new SimulationId(simIdA), OWNER_ID, "Test Sim Credits A", PERIOD_ID,
                List.of(PROGRAM_ID), SimulationLifecycle.ACTIVE,
                Instant.now(), Instant.now(), 0L);

        SimulationSelection selA = SimulationSelection.reconstitute(
                new SimulationSelectionId(selIdA), simIdA, offeringIdA, UUID.randomUUID(),
                courseIdA, "Test Course Credits A", PROGRAM_ID, 3.0,
                "DIURNA", "PRELIMINARY", 1L, List.of(), "ACTIVE", 0L);

        when(loadSimulationPort.loadById(simIdA)).thenReturn(Optional.of(simA));
        when(loadSimulationSelectionPort.loadActiveBySimulationId(simIdA))
                .thenReturn(List.of(selA));
        when(offeringReadPort.resolvePeriodName(PERIOD_ID)).thenReturn("2026-2");
        when(offeringReadPort.currentOfferingVersion(offeringIdA)).thenReturn(1L);
        when(creditLimitPort.creditLimit(any(), any())).thenReturn(23);
        when(progressReadPort.passedCourseIds(OWNER_ID, PROGRAM_ID)).thenReturn(passedCourseIds);
        when(programNameReadPort.programName(any())).thenReturn("Systems Engineering");

        SimulationDetailView resultA = sut.getSimulation(simIdA, OWNER_ID);

        assertThat(resultA.validation().blocking())
                .as("30 courses × 3 credits = 90 accumulated; required=80 → NO SIMULATION_MIN_CREDITS_NOT_MET blocker")
                .noneMatch(n -> "SIMULATION_MIN_CREDITS_NOT_MET".equals(n.code()));

        // --- Scenario B: required=100, accumulated=90 → blocker MUST be present ---
        UUID simIdB = UUID.randomUUID();
        UUID courseIdB = UUID.randomUUID();
        UUID offeringIdB = UUID.randomUUID();
        UUID selIdB = UUID.randomUUID();

        PrereqRequirement minCreditsReq100 = new PrereqRequirement(
                courseIdB,
                null,
                "MINIMUM_CREDITS",
                "HIGH",
                false,
                null,
                100);               // threshold: 100 > 90 → gate FAILS

        curriculumReadPort.requirementsForCourseId = courseIdB;
        curriculumReadPort.requirementsForCourseIdResult = List.of(minCreditsReq100);

        Simulation simB = Simulation.reconstitute(
                new SimulationId(simIdB), OWNER_ID, "Test Sim Credits B", PERIOD_ID,
                List.of(PROGRAM_ID), SimulationLifecycle.ACTIVE,
                Instant.now(), Instant.now(), 0L);

        SimulationSelection selB = SimulationSelection.reconstitute(
                new SimulationSelectionId(selIdB), simIdB, offeringIdB, UUID.randomUUID(),
                courseIdB, "Test Course Credits B", PROGRAM_ID, 3.0,
                "DIURNA", "PRELIMINARY", 1L, List.of(), "ACTIVE", 0L);

        when(loadSimulationPort.loadById(simIdB)).thenReturn(Optional.of(simB));
        when(loadSimulationSelectionPort.loadActiveBySimulationId(simIdB))
                .thenReturn(List.of(selB));
        when(offeringReadPort.currentOfferingVersion(offeringIdB)).thenReturn(1L);
        when(progressReadPort.passedCourseIds(OWNER_ID, PROGRAM_ID)).thenReturn(passedCourseIds);

        SimulationDetailView resultB = sut.getSimulation(simIdB, OWNER_ID);

        assertThat(resultB.validation().blocking())
                .as("30 courses × 3 credits = 90 accumulated; required=100 → SIMULATION_MIN_CREDITS_NOT_MET must be present")
                .anyMatch(n -> "SIMULATION_MIN_CREDITS_NOT_MET".equals(n.code()));
    }

    // -------------------------------------------------------------------------
    // In-memory counting stub for CurriculumReadPort
    // -------------------------------------------------------------------------

    /**
     * Counting stub that records how many times {@code coursesCredits(Set<UUID>)} is called.
     *
     * <p>All per-course methods return zero/empty by default. Use
     * {@code requirementsForCourseId} + {@code requirementsForCourseIdResult} to inject a
     * configurable requirement list for a specific course (used by the credit-value test).
     * This avoids Mockito stubbing on a non-mock object.
     */
    private static class CountingCurriculumReadPort implements CurriculumReadPort {

        /** Counts invocations of the batch method. */
        final AtomicInteger coursesCreditsCallCount = new AtomicInteger(0);

        /**
         * Optional: when non-null, {@code requirementsForCourse} returns
         * {@code requirementsForCourseIdResult} for this specific courseId.
         */
        UUID requirementsForCourseId = null;
        List<PrereqRequirement> requirementsForCourseIdResult = List.of();

        @Override
        public List<PrereqRequirement> requirementsForCourse(UUID courseId) {
            if (requirementsForCourseId != null && requirementsForCourseId.equals(courseId)) {
                return requirementsForCourseIdResult;
            }
            return List.of();
        }

        @Override
        public int courseCredits(UUID courseId) {
            return 3;
        }

        /**
         * Batch credits method — counts invocations.
         * Returns a map assigning 3 credits to each requested course ID.
         */
        @Override
        public Map<UUID, Integer> coursesCredits(Set<UUID> courseIds) {
            coursesCreditsCallCount.incrementAndGet();
            Map<UUID, Integer> credits = new HashMap<>();
            for (UUID id : courseIds) {
                credits.put(id, 3);
            }
            return credits;
        }

        @Override
        public co.edu.uniquindio.brujula.simulation.domain.SequentialPredecessor
                sequentialPredecessor(UUID courseId, UUID planId) {
            return new co.edu.uniquindio.brujula.simulation.domain.SequentialPredecessor(
                    courseId, "REQUIRED", null, null);
        }
    }
}
