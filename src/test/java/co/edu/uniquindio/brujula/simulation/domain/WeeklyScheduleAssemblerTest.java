package co.edu.uniquindio.brujula.simulation.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link WeeklyScheduleAssembler} (spec DET-001).
 *
 * <p>Pure Java — no Spring context needed.
 */
class WeeklyScheduleAssemblerTest {

  private final WeeklyScheduleAssembler assembler = new WeeklyScheduleAssembler();

  private static final UUID PROGRAM_A = UUID.randomUUID();
  private static final UUID PROGRAM_B = UUID.randomUUID();

  private WeeklyScheduleAssembler.SelectionSessions makeSelection(
      UUID id, String courseName, UUID programId,
      String day, LocalTime start, LocalTime end) {
    return new WeeklyScheduleAssembler.SelectionSessions(
        id, courseName, programId,
        List.of(new SimScheduleSession(day, start, end)));
  }

  // -------------------------------------------------------------------------
  // Empty case
  // -------------------------------------------------------------------------

  @Test
  void assemble_emptySelections_returnsEmptyList() {
    List<WeeklyScheduleEntry> result = assembler.assemble(List.of());
    assertThat(result).isEmpty();
  }

  @Test
  void assemble_nullSelections_returnsEmptyList() {
    List<WeeklyScheduleEntry> result = assembler.assemble(null);
    assertThat(result).isEmpty();
  }

  // -------------------------------------------------------------------------
  // Non-overlapping sessions — all inClash=false
  // -------------------------------------------------------------------------

  @Test
  void assemble_twoNonOverlappingSessions_allInClashFalse() {
    // Selection A: MONDAY 08:00-10:00
    // Selection B: MONDAY 10:00-12:00 (touching — NOT a clash)
    UUID idA = UUID.randomUUID();
    UUID idB = UUID.randomUUID();

    List<WeeklyScheduleAssembler.SelectionSessions> selections = List.of(
        makeSelection(idA, "Algebra", PROGRAM_A, "MONDAY",
            LocalTime.of(8, 0), LocalTime.of(10, 0)),
        makeSelection(idB, "Calculus", PROGRAM_A, "MONDAY",
            LocalTime.of(10, 0), LocalTime.of(12, 0)));

    List<WeeklyScheduleEntry> entries = assembler.assemble(selections);

    assertThat(entries).hasSize(2);
    assertThat(entries).allMatch(e -> !e.inClash());
  }

  @Test
  void assemble_differentDaysSameTime_noClash() {
    UUID idA = UUID.randomUUID();
    UUID idB = UUID.randomUUID();

    List<WeeklyScheduleAssembler.SelectionSessions> selections = List.of(
        makeSelection(idA, "Algebra", PROGRAM_A, "MONDAY",
            LocalTime.of(8, 0), LocalTime.of(10, 0)),
        makeSelection(idB, "Calculus", PROGRAM_A, "TUESDAY",
            LocalTime.of(8, 0), LocalTime.of(10, 0)));

    List<WeeklyScheduleEntry> entries = assembler.assemble(selections);

    assertThat(entries).hasSize(2);
    assertThat(entries).allMatch(e -> !e.inClash());
  }

  // -------------------------------------------------------------------------
  // Overlapping sessions — both inClash=true
  // -------------------------------------------------------------------------

  @Test
  void assemble_twoOverlappingSessions_bothInClashTrue() {
    // Selection A: WEDNESDAY 18:00-20:00
    // Selection B: WEDNESDAY 19:00-21:00 (overlap)
    UUID idA = UUID.randomUUID();
    UUID idB = UUID.randomUUID();

    List<WeeklyScheduleAssembler.SelectionSessions> selections = List.of(
        makeSelection(idA, "Software Engineering", PROGRAM_A, "WEDNESDAY",
            LocalTime.of(18, 0), LocalTime.of(20, 0)),
        makeSelection(idB, "Databases", PROGRAM_A, "WEDNESDAY",
            LocalTime.of(19, 0), LocalTime.of(21, 0)));

    List<WeeklyScheduleEntry> entries = assembler.assemble(selections);

    assertThat(entries).hasSize(2);
    assertThat(entries).allMatch(e -> e.inClash());
  }

  @Test
  void assemble_exactSameTimeOnSameDay_bothInClashTrue() {
    UUID idA = UUID.randomUUID();
    UUID idB = UUID.randomUUID();

    List<WeeklyScheduleAssembler.SelectionSessions> selections = List.of(
        makeSelection(idA, "Course A", PROGRAM_A, "FRIDAY",
            LocalTime.of(14, 0), LocalTime.of(16, 0)),
        makeSelection(idB, "Course B", PROGRAM_B, "FRIDAY",
            LocalTime.of(14, 0), LocalTime.of(16, 0)));

    List<WeeklyScheduleEntry> entries = assembler.assemble(selections);

    assertThat(entries).hasSize(2);
    assertThat(entries).allMatch(e -> e.inClash());
  }

  // -------------------------------------------------------------------------
  // Real data: courseName and programId flow through
  // -------------------------------------------------------------------------

  @Test
  void assemble_entriesCarryRealCourseNameAndProgramId() {
    UUID selId = UUID.randomUUID();
    List<WeeklyScheduleAssembler.SelectionSessions> selections = List.of(
        makeSelection(selId, "Ingeniería de Software I", PROGRAM_A, "THURSDAY",
            LocalTime.of(16, 0), LocalTime.of(18, 0)));

    List<WeeklyScheduleEntry> entries = assembler.assemble(selections);

    assertThat(entries).hasSize(1);
    WeeklyScheduleEntry entry = entries.getFirst();
    assertThat(entry.courseName()).isEqualTo("Ingeniería de Software I");
    assertThat(entry.programId()).isEqualTo(PROGRAM_A);
    assertThat(entry.selectionId()).isEqualTo(selId);
    assertThat(entry.day()).isEqualTo("THURSDAY");
  }

  // -------------------------------------------------------------------------
  // Multiple sessions per selection
  // -------------------------------------------------------------------------

  @Test
  void assemble_multipleSessionsPerSelection_allSessionsFlattened() {
    UUID selId = UUID.randomUUID();
    List<SimScheduleSession> sessions = List.of(
        new SimScheduleSession("MONDAY", LocalTime.of(8, 0), LocalTime.of(10, 0)),
        new SimScheduleSession("WEDNESDAY", LocalTime.of(8, 0), LocalTime.of(10, 0)));

    List<WeeklyScheduleAssembler.SelectionSessions> selections = List.of(
        new WeeklyScheduleAssembler.SelectionSessions(selId, "Physics", PROGRAM_A, sessions));

    List<WeeklyScheduleEntry> entries = assembler.assemble(selections);

    assertThat(entries).hasSize(2);
    assertThat(entries).extracting(WeeklyScheduleEntry::day)
        .containsExactlyInAnyOrder("MONDAY", "WEDNESDAY");
    assertThat(entries).allMatch(e -> !e.inClash()); // same selection, can't clash with itself
  }

  // -------------------------------------------------------------------------
  // Three selections, partial overlap
  // -------------------------------------------------------------------------

  @Test
  void assemble_threeSelections_onlyOverlappingMarkedInClash() {
    UUID idA = UUID.randomUUID();
    UUID idB = UUID.randomUUID();
    UUID idC = UUID.randomUUID();

    List<WeeklyScheduleAssembler.SelectionSessions> selections = List.of(
        // A: MONDAY 08:00-10:00
        makeSelection(idA, "Algebra", PROGRAM_A, "MONDAY",
            LocalTime.of(8, 0), LocalTime.of(10, 0)),
        // B: MONDAY 09:00-11:00 (overlaps A)
        makeSelection(idB, "Calculus", PROGRAM_A, "MONDAY",
            LocalTime.of(9, 0), LocalTime.of(11, 0)),
        // C: TUESDAY 08:00-10:00 (no overlap with A or B)
        makeSelection(idC, "Physics", PROGRAM_A, "TUESDAY",
            LocalTime.of(8, 0), LocalTime.of(10, 0)));

    List<WeeklyScheduleEntry> entries = assembler.assemble(selections);

    assertThat(entries).hasSize(3);
    // A and B clash, C does not
    WeeklyScheduleEntry entryA = entries.stream()
        .filter(e -> e.selectionId().equals(idA)).findFirst().orElseThrow();
    WeeklyScheduleEntry entryB = entries.stream()
        .filter(e -> e.selectionId().equals(idB)).findFirst().orElseThrow();
    WeeklyScheduleEntry entryC = entries.stream()
        .filter(e -> e.selectionId().equals(idC)).findFirst().orElseThrow();

    assertThat(entryA.inClash()).isTrue();
    assertThat(entryB.inClash()).isTrue();
    assertThat(entryC.inClash()).isFalse();
  }
}
