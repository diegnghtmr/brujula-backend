package co.edu.uniquindio.brujula.progress.domain;

/**
 * Computed/visual status of a course in the curriculum view.
 *
 * <p>This is DISTINCT from {@link AttemptStatus} — it is a derived presentation status:
 *
 * <ul>
 *   <li>PENDING — no attempt; prerequisites not evaluated yet.
 *   <li>PASSED — effective status across all attempts = PASSED.
 *   <li>FAILED — latest attempt is FAILED, no PASSED attempt exists.
 *   <li>CANCELLED — latest attempt is CANCELLED, no PASSED attempt exists.
 *   <li>AVAILABLE — no attempt; all prerequisites met (or no prerequisites).
 *   <li>BLOCKED — no attempt; at least one HARD prerequisite is unmet.
 * </ul>
 */
public enum CourseProgressStatus {
  PENDING,
  PASSED,
  FAILED,
  CANCELLED,
  AVAILABLE,
  BLOCKED
}
