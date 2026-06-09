package co.edu.uniquindio.brujula.curriculum.domain;

import java.util.UUID;

/**
 * Domain exception thrown when a course requirement creates a forward-semester dependency.
 *
 * <p>A forward dependency occurs when the required course's recommended semester (for the same
 * journey) is strictly later than the target course's recommended semester — meaning the
 * prerequisite is in a higher semester than the course requiring it, which is impossible.
 *
 * <p>Maps to HTTP 422 (Unprocessable Entity) at the REST adapter layer (CUR-004). No Spring/JPA
 * imports (ARQ-002).
 */
public class SemesterOrderingViolationException extends RuntimeException {

  private final UUID requiredCourseId;
  private final UUID targetCourseId;
  private final String journeyCode;
  private final int requiredSemester;
  private final int targetSemester;

  public SemesterOrderingViolationException(
      UUID requiredCourseId,
      UUID targetCourseId,
      String journeyCode,
      int requiredSemester,
      int targetSemester) {
    super(
        ("Semester ordering violation in journey '%s': required course (semester %d) is in a later"
                + " semester than target course (semester %d). Required course: %s, Target course: %s")
            .formatted(
                journeyCode,
                requiredSemester,
                targetSemester,
                requiredCourseId.toString(),
                targetCourseId.toString()));
    this.requiredCourseId = requiredCourseId;
    this.targetCourseId = targetCourseId;
    this.journeyCode = journeyCode;
    this.requiredSemester = requiredSemester;
    this.targetSemester = targetSemester;
  }

  public UUID requiredCourseId() {
    return requiredCourseId;
  }

  public UUID targetCourseId() {
    return targetCourseId;
  }

  public String journeyCode() {
    return journeyCode;
  }

  public int requiredSemester() {
    return requiredSemester;
  }

  public int targetSemester() {
    return targetSemester;
  }
}
