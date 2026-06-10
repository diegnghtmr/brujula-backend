package co.edu.uniquindio.brujula.progress.application.port.in.api;

import java.util.Set;
import java.util.UUID;

/**
 * Published input port — read-only access to a student's declared course history.
 *
 * <p>Used by the simulation module to evaluate feasibility validation rules:
 *
 * <ul>
 *   <li>AlreadyPassedRule — checks if a course is already PASSED
 *   <li>PrerequisiteEvaluator — checks whether required prerequisite courses were passed
 * </ul>
 *
 * <p>ZERO mutation to progress domain behavior — this is a read-only projection port (ADR-F4-002).
 *
 * <p>Published via {@code @NamedInterface("api")} in {@code port/in/api/package-info.java}.
 * Cross-module consumers (e.g. simulation) must access this port ONLY through {@code progress::api}
 * (INV-001, ADR-F4-002).
 *
 * <p>ARQ-001: method signatures use only UUID and String primitives — no domain entities.
 */
public interface GetStudentCourseHistoryQuery {

  /**
   * Return the set of course IDs for which the student has an effective status of PASSED, within
   * the given program subscription.
   *
   * <p>Uses {@code EffectiveAttemptResolver} — the most favorable status wins: PASSED beats FAILED
   * beats CANCELLED.
   *
   * @param ownerId the student's user UUID
   * @param programId the program UUID to scope the lookup to
   * @return set of courseIds with effective PASSED status; empty if none or no subscription found
   */
  Set<UUID> passedCourseIds(UUID ownerId, UUID programId);

  /**
   * Return the effective attempt status name for a specific course within the given program
   * subscription.
   *
   * <p>Delegates to {@code EffectiveAttemptResolver}. Returns the most favorable status name string
   * ("PASSED", "FAILED", "CANCELLED"), or the sentinel "NONE" when no attempt exists or no
   * subscription matches.
   *
   * @param ownerId the student's user UUID
   * @param programId the program UUID to scope the lookup to
   * @param courseId the course UUID
   * @return "PASSED", "FAILED", "CANCELLED", or "NONE" sentinel when no attempt
   */
  String effectiveStatus(UUID ownerId, UUID programId, UUID courseId);
}
