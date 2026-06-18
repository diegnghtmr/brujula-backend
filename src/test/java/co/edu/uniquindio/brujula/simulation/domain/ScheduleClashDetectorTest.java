package co.edu.uniquindio.brujula.simulation.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link ScheduleClashDetector}.
 *
 * <p>Pure domain — no Spring, no mocks.
 */
class ScheduleClashDetectorTest {

  private ScheduleClashDetector detector;

  @BeforeEach
  void setUp() {
    detector = new ScheduleClashDetector();
  }

  // -------------------------------------------------------------------------
  // SimScheduleSession.overlaps boundary tests (used by detector)
  // -------------------------------------------------------------------------

  @Test
  void touchingSessionsDoNotClash() {
    // existing: WEDNESDAY 18:00–20:00; candidate starts at 20:00 (adjacent, non-overlapping)
    SimScheduleSession existing =
        new SimScheduleSession("WEDNESDAY", LocalTime.of(18, 0), LocalTime.of(20, 0));
    SimScheduleSession candidate =
        new SimScheduleSession("WEDNESDAY", LocalTime.of(20, 0), LocalTime.of(22, 0));
    assertThat(candidate.overlaps(existing)).isFalse();
  }

  @Test
  void partialOverlapIsClash() {
    // existing: WEDNESDAY 18:00–20:00; candidate: 19:00–21:00 — 1-hour overlap
    SimScheduleSession existing =
        new SimScheduleSession("WEDNESDAY", LocalTime.of(18, 0), LocalTime.of(20, 0));
    SimScheduleSession candidate =
        new SimScheduleSession("WEDNESDAY", LocalTime.of(19, 0), LocalTime.of(21, 0));
    assertThat(candidate.overlaps(existing)).isTrue();
  }

  @Test
  void exactSameSlotIsClash() {
    SimScheduleSession s1 =
        new SimScheduleSession("WEDNESDAY", LocalTime.of(18, 0), LocalTime.of(20, 0));
    SimScheduleSession s2 =
        new SimScheduleSession("WEDNESDAY", LocalTime.of(18, 0), LocalTime.of(20, 0));
    assertThat(s1.overlaps(s2)).isTrue();
  }

  @Test
  void differentDayNoClash() {
    SimScheduleSession monday =
        new SimScheduleSession("MONDAY", LocalTime.of(18, 0), LocalTime.of(20, 0));
    SimScheduleSession wednesday =
        new SimScheduleSession("WEDNESDAY", LocalTime.of(18, 0), LocalTime.of(20, 0));
    assertThat(monday.overlaps(wednesday)).isFalse();
  }

  // -------------------------------------------------------------------------
  // ScheduleClashDetector end-to-end tests
  // -------------------------------------------------------------------------

  @Test
  void noClashWhenNoExistingSelections() {
    CandidateSelection candidate = buildCandidate("WEDNESDAY", 18, 0, 20, 0);
    Optional<ValidationNotice> result = detector.detect(candidate, List.of());
    assertThat(result).isEmpty();
  }

  @Test
  void clashDetectedWithExistingSelection() {
    // existing has session WEDNESDAY 18:00–20:00; candidate also WEDNESDAY 18:00–20:00
    ExistingSelection existing = buildExisting("WEDNESDAY", 18, 0, 20, 0, "Cálculo I");
    CandidateSelection candidate = buildCandidate("WEDNESDAY", 18, 0, 20, 0);

    Optional<ValidationNotice> result = detector.detect(candidate, List.of(existing));

    assertThat(result).isPresent();
    ValidationNotice notice = result.get();
    assertThat(notice.code()).isEqualTo("SIMULATION_SCHEDULE_CLASH");
    assertThat(notice.metadata()).containsEntry("conflictingCourseName", "Cálculo I");
    assertThat(notice.metadata()).containsKey("day");
    assertThat(notice.metadata()).containsKey("startsAt");
    assertThat(notice.metadata()).containsKey("endsAt");
  }

  @Test
  void noClashWhenDifferentDays() {
    ExistingSelection existing = buildExisting("MONDAY", 18, 0, 20, 0, "Cálculo I");
    CandidateSelection candidate = buildCandidate("WEDNESDAY", 18, 0, 20, 0);
    assertThat(detector.detect(candidate, List.of(existing))).isEmpty();
  }

  @Test
  void noClashWhenAdjacentSlots() {
    ExistingSelection existing = buildExisting("WEDNESDAY", 18, 0, 20, 0, "Física I");
    // candidate starts exactly when existing ends
    CandidateSelection candidate = buildCandidate("WEDNESDAY", 20, 0, 22, 0);
    assertThat(detector.detect(candidate, List.of(existing))).isEmpty();
  }

  // -------------------------------------------------------------------------
  // Helper builders
  // -------------------------------------------------------------------------

  private CandidateSelection buildCandidate(
      String day, int startH, int startM, int endH, int endM) {
    SimScheduleSession session =
        new SimScheduleSession(day, LocalTime.of(startH, startM), LocalTime.of(endH, endM));
    return new CandidateSelection(
        UUID.randomUUID(),
        UUID.randomUUID(),
        UUID.randomUUID(),
        "Test Course",
        UUID.randomUUID(),
        3.0,
        "DIURNA",
        "FINAL",
        1L,
        List.of(session));
  }

  private ExistingSelection buildExisting(
      String day, int startH, int startM, int endH, int endM, String courseName) {
    SimScheduleSession session =
        new SimScheduleSession(day, LocalTime.of(startH, startM), LocalTime.of(endH, endM));
    return new ExistingSelection(
        UUID.randomUUID(), UUID.randomUUID(), courseName, UUID.randomUUID(), 3.0, List.of(session));
  }
}
