package co.edu.uniquindio.brujula.simulation.domain;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Pure domain service: assembles the weekly schedule grid from all active selections (spec
 * DET-001).
 *
 * <p>Produces one {@link WeeklyScheduleEntry} per {@link SimScheduleSession} per selection. Marks
 * {@code inClash=true} for any session that overlaps with at least one OTHER session in the full
 * simulation set (not just pairwise with a candidate — this is a whole-simulation view).
 *
 * <p>Clash detection: reuses {@link SimScheduleSession#overlaps(SimScheduleSession)} — same
 * algorithm as {@link ScheduleClashDetector} but applied across all session pairs in the entire
 * set.
 *
 * <p>Pure Java — no Spring/JPA (ARQ-002, ADR-F4-004).
 */
public class WeeklyScheduleAssembler {

  /**
   * Input record: one active selection with its sessions.
   *
   * @param selectionId the selection UUID
   * @param courseName denormalized course name
   * @param programId program UUID
   * @param sessions list of schedule sessions for this selection
   */
  public record SelectionSessions(
      UUID selectionId, String courseName, UUID programId, List<SimScheduleSession> sessions) {}

  /**
   * Assemble the weekly schedule grid from all active selections.
   *
   * <p>Algorithm:
   *
   * <ol>
   *   <li>Flatten all (selectionId, session) pairs.
   *   <li>For each session, check if it overlaps with any session from a DIFFERENT selection.
   *   <li>Build one entry per session, setting {@code inClash} accordingly.
   * </ol>
   *
   * @param selections the active selections in the simulation (may be empty)
   * @return list of weekly schedule entries, one per session
   */
  public List<WeeklyScheduleEntry> assemble(List<SelectionSessions> selections) {
    if (selections == null || selections.isEmpty()) {
      return List.of();
    }

    // Flatten all sessions with their selection context
    record SessionWithContext(
        UUID selectionId, String courseName, UUID programId, SimScheduleSession session) {}

    List<SessionWithContext> allSessions = new ArrayList<>();
    for (SelectionSessions sel : selections) {
      for (SimScheduleSession s : sel.sessions()) {
        allSessions.add(
            new SessionWithContext(sel.selectionId(), sel.courseName(), sel.programId(), s));
      }
    }

    // Compute the set of indices that are in a clash
    Set<Integer> clashingIndices = new HashSet<>();
    for (int i = 0; i < allSessions.size(); i++) {
      SessionWithContext a = allSessions.get(i);
      for (int j = 0; j < allSessions.size(); j++) {
        if (i == j) continue;
        SessionWithContext b = allSessions.get(j);
        // Only clash with sessions from a DIFFERENT selection
        if (!a.selectionId().equals(b.selectionId()) && a.session().overlaps(b.session())) {
          clashingIndices.add(i);
          clashingIndices.add(j);
        }
      }
    }

    // Build entries
    List<WeeklyScheduleEntry> entries = new ArrayList<>();
    for (int i = 0; i < allSessions.size(); i++) {
      SessionWithContext ctx = allSessions.get(i);
      SimScheduleSession s = ctx.session();
      entries.add(
          new WeeklyScheduleEntry(
              s.day(),
              s.startsAt().toString(),
              s.endsAt().toString(),
              ctx.courseName(),
              ctx.programId(),
              ctx.selectionId(),
              clashingIndices.contains(i)));
    }

    return entries;
  }
}
