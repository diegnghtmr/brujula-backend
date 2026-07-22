package co.edu.uniquindio.brujula.progress.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import co.edu.uniquindio.brujula.progress.application.port.in.api.GetStudentCourseHistoryQuery;
import co.edu.uniquindio.brujula.progress.application.port.out.LoadCourseAttemptPort;
import co.edu.uniquindio.brujula.progress.application.port.out.LoadSubscriptionPort;
import co.edu.uniquindio.brujula.progress.application.service.GetStudentCourseHistoryService;
import co.edu.uniquindio.brujula.progress.domain.AttemptId;
import co.edu.uniquindio.brujula.progress.domain.AttemptStatus;
import co.edu.uniquindio.brujula.progress.domain.CourseAttempt;
import co.edu.uniquindio.brujula.progress.domain.Grade;
import co.edu.uniquindio.brujula.progress.domain.Subscription;
import co.edu.uniquindio.brujula.progress.domain.SubscriptionId;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link GetStudentCourseHistoryService} — TDD RED→GREEN.
 *
 * <p>Scenarios:
 *
 * <ul>
 *   <li>PASSED attempt → courseId included in passedCourseIds
 *   <li>FAILED attempt only → courseId NOT in passedCourseIds
 *   <li>No attempt → courseId NOT in passedCourseIds
 *   <li>effectiveStatus returns PASSED when student has a PASSED attempt
 *   <li>effectiveStatus returns FAILED when student has only a FAILED attempt
 *   <li>effectiveStatus returns "NONE" sentinel when no attempt exists
 *   <li>Multi-attempt: FAILED then PASSED → courseId IS in passedCourseIds (PASSED wins)
 *   <li>Multi-attempt: PASSED then FAILED → courseId IS still in passedCourseIds (PASSED wins)
 * </ul>
 */
class GetStudentCourseHistoryServiceTest {

  private LoadSubscriptionPort loadSubscriptionPort;
  private LoadCourseAttemptPort loadCourseAttemptPort;
  private GetStudentCourseHistoryQuery service;

  private final UUID ownerId = UUID.randomUUID();
  private final UUID programId = UUID.randomUUID();
  private final UUID courseId1 = UUID.randomUUID();
  private final UUID courseId2 = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    loadSubscriptionPort = mock(LoadSubscriptionPort.class);
    loadCourseAttemptPort = mock(LoadCourseAttemptPort.class);
    service = new GetStudentCourseHistoryService(loadSubscriptionPort, loadCourseAttemptPort);
  }

  // -------------------------------------------------------------------------
  // passedCourseIds
  // -------------------------------------------------------------------------

  @Test
  void passedAttemptIncludesCourseIdInPassedSet() {
    UUID subscriptionId = UUID.randomUUID();
    Subscription sub = subscriptionWith(subscriptionId, ownerId, programId);
    when(loadSubscriptionPort.loadActiveByUserId(ownerId)).thenReturn(List.of(sub));

    CourseAttempt passed = passedAttempt(subscriptionId, courseId1);
    when(loadCourseAttemptPort.loadBySubscriptionId(subscriptionId)).thenReturn(List.of(passed));

    Set<UUID> result = service.passedCourseIds(ownerId, programId);

    assertThat(result).containsExactly(courseId1);
  }

  @Test
  void failedOnlyAttemptIsNotIncludedInPassedSet() {
    UUID subscriptionId = UUID.randomUUID();
    Subscription sub = subscriptionWith(subscriptionId, ownerId, programId);
    when(loadSubscriptionPort.loadActiveByUserId(ownerId)).thenReturn(List.of(sub));

    CourseAttempt failed = failedAttempt(subscriptionId, courseId1);
    when(loadCourseAttemptPort.loadBySubscriptionId(subscriptionId)).thenReturn(List.of(failed));

    Set<UUID> result = service.passedCourseIds(ownerId, programId);

    assertThat(result).isEmpty();
  }

  @Test
  void noAttemptMeansNotInPassedSet() {
    UUID subscriptionId = UUID.randomUUID();
    Subscription sub = subscriptionWith(subscriptionId, ownerId, programId);
    when(loadSubscriptionPort.loadActiveByUserId(ownerId)).thenReturn(List.of(sub));
    when(loadCourseAttemptPort.loadBySubscriptionId(subscriptionId)).thenReturn(List.of());

    Set<UUID> result = service.passedCourseIds(ownerId, programId);

    assertThat(result).isEmpty();
  }

  @Test
  void passedCourseIdsOnlyCountsCourseFromMatchingProgram() {
    UUID subscriptionId = UUID.randomUUID();
    UUID otherProgramId = UUID.randomUUID();
    Subscription matchingProgram = subscriptionWith(subscriptionId, ownerId, programId);
    UUID otherSubId = UUID.randomUUID();
    Subscription otherProgram = subscriptionWith(otherSubId, ownerId, otherProgramId);

    when(loadSubscriptionPort.loadActiveByUserId(ownerId))
        .thenReturn(List.of(matchingProgram, otherProgram));

    CourseAttempt passedInMatching = passedAttempt(subscriptionId, courseId1);
    CourseAttempt passedInOther = passedAttempt(otherSubId, courseId2);
    when(loadCourseAttemptPort.loadBySubscriptionId(subscriptionId))
        .thenReturn(List.of(passedInMatching));
    when(loadCourseAttemptPort.loadBySubscriptionId(otherSubId)).thenReturn(List.of(passedInOther));

    Set<UUID> result = service.passedCourseIds(ownerId, programId);

    // Only courses from the matching program subscription
    assertThat(result).containsExactly(courseId1);
    assertThat(result).doesNotContain(courseId2);
  }

  // -------------------------------------------------------------------------
  // effectiveStatus
  // -------------------------------------------------------------------------

  @Test
  void effectiveStatusReturnsPASSEDWhenStudentHasPassedAttempt() {
    UUID subscriptionId = UUID.randomUUID();
    Subscription sub = subscriptionWith(subscriptionId, ownerId, programId);
    when(loadSubscriptionPort.loadActiveByUserId(ownerId)).thenReturn(List.of(sub));

    CourseAttempt passed = passedAttempt(subscriptionId, courseId1);
    when(loadCourseAttemptPort.loadBySubscriptionId(subscriptionId)).thenReturn(List.of(passed));

    String status = service.effectiveStatus(ownerId, programId, courseId1);

    assertThat(status).isEqualTo("PASSED");
  }

  @Test
  void effectiveStatusReturnsFAILEDWhenOnlyFailedAttemptExists() {
    UUID subscriptionId = UUID.randomUUID();
    Subscription sub = subscriptionWith(subscriptionId, ownerId, programId);
    when(loadSubscriptionPort.loadActiveByUserId(ownerId)).thenReturn(List.of(sub));

    CourseAttempt failed = failedAttempt(subscriptionId, courseId1);
    when(loadCourseAttemptPort.loadBySubscriptionId(subscriptionId)).thenReturn(List.of(failed));

    String status = service.effectiveStatus(ownerId, programId, courseId1);

    assertThat(status).isEqualTo("FAILED");
  }

  @Test
  void effectiveStatusReturnsNONESentinelWhenNoAttemptExists() {
    UUID subscriptionId = UUID.randomUUID();
    Subscription sub = subscriptionWith(subscriptionId, ownerId, programId);
    when(loadSubscriptionPort.loadActiveByUserId(ownerId)).thenReturn(List.of(sub));
    when(loadCourseAttemptPort.loadBySubscriptionId(subscriptionId)).thenReturn(List.of());

    String status = service.effectiveStatus(ownerId, programId, courseId1);

    assertThat(status).isEqualTo("NONE");
  }

  @Test
  void effectiveStatusReturnsNONEWhenNoSubscriptionMatchesProgram() {
    when(loadSubscriptionPort.loadActiveByUserId(ownerId)).thenReturn(List.of());

    String status = service.effectiveStatus(ownerId, programId, courseId1);

    assertThat(status).isEqualTo("NONE");
  }

  // -------------------------------------------------------------------------
  // Multi-attempt PASSED-wins scenarios (S-1 fix)
  // -------------------------------------------------------------------------

  @Test
  void failedThenPassedForSameCourseResultsInCourseInPassedSet() {
    UUID subscriptionId = UUID.randomUUID();
    Subscription sub = subscriptionWith(subscriptionId, ownerId, programId);
    when(loadSubscriptionPort.loadActiveByUserId(ownerId)).thenReturn(List.of(sub));

    CourseAttempt firstFailed = failedAttempt(subscriptionId, courseId1);
    CourseAttempt secondPassed = passedAttempt(subscriptionId, courseId1);
    when(loadCourseAttemptPort.loadBySubscriptionId(subscriptionId))
        .thenReturn(List.of(firstFailed, secondPassed));

    Set<UUID> result = service.passedCourseIds(ownerId, programId);

    // PASSED wins: courseId1 is present exactly once despite the earlier FAILED attempt
    assertThat(result).containsExactly(courseId1);
  }

  @Test
  void passedThenFailedForSameCourseStillCountsAsPassed() {
    UUID subscriptionId = UUID.randomUUID();
    Subscription sub = subscriptionWith(subscriptionId, ownerId, programId);
    when(loadSubscriptionPort.loadActiveByUserId(ownerId)).thenReturn(List.of(sub));

    CourseAttempt firstPassed = passedAttempt(subscriptionId, courseId1);
    CourseAttempt laterFailed = failedAttempt(subscriptionId, courseId1);
    when(loadCourseAttemptPort.loadBySubscriptionId(subscriptionId))
        .thenReturn(List.of(firstPassed, laterFailed));

    Set<UUID> result = service.passedCourseIds(ownerId, programId);

    // PASSED wins regardless of insertion order: EffectiveAttemptResolver picks most favorable
    assertThat(result).containsExactly(courseId1);
  }

  // -------------------------------------------------------------------------
  // PROP-001 (REQ-6, regression guard): distinct profundización elective courseIds must NOT
  // collapse into one PASSED entry. Before V028+V029 repointed offerings, all profundización
  // attempts shared one of 4 slot ids; a student passing 4 distinct electives would incorrectly
  // resolve to only 1-4 collapsed ids. With real per-elective ids, distinctness is a property of
  // the Set<UUID> return type — no code change needed here, only real ids upstream.
  // -------------------------------------------------------------------------

  @Test
  void fourDistinctProfundizacionElectivesPassed_allFourRemainDistinctInPassedSet() {
    UUID subscriptionId = UUID.randomUUID();
    Subscription sub = subscriptionWith(subscriptionId, ownerId, programId);
    when(loadSubscriptionPort.loadActiveByUserId(ownerId)).thenReturn(List.of(sub));

    UUID electiveDiseno = UUID.fromString("c0dee000-0000-4000-a000-000000000001");
    UUID electiveMoviles = UUID.fromString("c0dee000-0000-4000-a000-000000000002");
    UUID electiveUsabilidad = UUID.fromString("c0dee000-0000-4000-a000-000000000003");
    UUID electiveNube = UUID.fromString("c0dee000-0000-4000-a000-000000000004");

    when(loadCourseAttemptPort.loadBySubscriptionId(subscriptionId))
        .thenReturn(
            List.of(
                passedAttempt(subscriptionId, electiveDiseno),
                passedAttempt(subscriptionId, electiveMoviles),
                passedAttempt(subscriptionId, electiveUsabilidad),
                passedAttempt(subscriptionId, electiveNube)));

    Set<UUID> result = service.passedCourseIds(ownerId, programId);

    // No collapse: all 4 distinct real elective ids are present
    assertThat(result)
        .containsExactlyInAnyOrder(
            electiveDiseno, electiveMoviles, electiveUsabilidad, electiveNube);
  }

  // -------------------------------------------------------------------------
  // Helpers
  // -------------------------------------------------------------------------

  private Subscription subscriptionWith(UUID subscriptionId, UUID userId, UUID progId) {
    return Subscription.reconstitute(
        SubscriptionId.of(subscriptionId),
        userId,
        progId,
        UUID.randomUUID(),
        "DIURNA",
        co.edu.uniquindio.brujula.progress.domain.SubscriptionStatus.ACTIVE,
        java.time.Instant.now(),
        0L);
  }

  private CourseAttempt passedAttempt(UUID subscriptionId, UUID courseId) {
    return CourseAttempt.reconstitute(
        AttemptId.of(UUID.randomUUID()),
        SubscriptionId.of(subscriptionId),
        courseId,
        AttemptStatus.PASSED,
        new Grade(new BigDecimal("4.0")),
        UUID.randomUUID(),
        "DIURNA",
        java.time.Instant.now(),
        0L);
  }

  private CourseAttempt failedAttempt(UUID subscriptionId, UUID courseId) {
    return CourseAttempt.reconstitute(
        AttemptId.of(UUID.randomUUID()),
        SubscriptionId.of(subscriptionId),
        courseId,
        AttemptStatus.FAILED,
        new Grade(new BigDecimal("2.0")),
        UUID.randomUUID(),
        "DIURNA",
        java.time.Instant.now(),
        0L);
  }
}
