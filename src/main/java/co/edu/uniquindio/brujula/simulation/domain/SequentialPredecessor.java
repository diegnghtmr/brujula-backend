package co.edu.uniquindio.brujula.simulation.domain;

import java.util.Objects;
import java.util.UUID;

/**
 * Sim-local record describing the sequential-track ordering constraint for a PERSONAL_ELECTIVE or
 * DEEPENING course.
 *
 * <p>Mapped from curriculum::api {@code SequentialTrackView} at the adapter edge (ARQ-002,
 * INV-001). If the candidate course is in a sequential track and that track has a {@code
 * gatekeeperCourseId}, the student must have passed that gatekeeper before adding the course.
 *
 * @param courseId the course being validated
 * @param courseType the course type string (e.g. "DEEPENING", "PERSONAL_ELECTIVE", "REQUIRED")
 * @param predecessorCourseId the gatekeeper/prerequisite course UUID that must be passed first
 *     (null when the course type is not DEEPENING/PERSONAL_ELECTIVE or when no track gatekeeper is
 *     defined)
 * @param predecessorCourseName human-readable name for error messages (nullable)
 */
public record SequentialPredecessor(
    UUID courseId, String courseType, UUID predecessorCourseId, String predecessorCourseName) {

  public SequentialPredecessor {
    Objects.requireNonNull(courseId, "courseId required");
    Objects.requireNonNull(courseType, "courseType required");
  }

  /**
   * Returns true when this course is subject to sequential track ordering evaluation (i.e. the
   * course type is DEEPENING or PERSONAL_ELECTIVE).
   */
  public boolean isSequentialTrackKind() {
    return "DEEPENING".equalsIgnoreCase(courseType)
        || "PERSONAL_ELECTIVE".equalsIgnoreCase(courseType);
  }

  /** Returns true when a predecessor constraint exists (track has a gatekeeper course). */
  public boolean hasPredecessor() {
    return predecessorCourseId != null;
  }
}
