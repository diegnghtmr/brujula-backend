package co.edu.uniquindio.brujula.curriculum.domain;

/**
 * Value object representing the recommended semester for a course within a specific journey.
 *
 * <p>Immutable (record). A course may have one {@code RecommendedSemester} per journey when the
 * program offers multiple journeys (e.g. DIURNA semester 3, NOCTURNA semester 4).
 *
 * <p>Invariants:
 *
 * <ul>
 *   <li>{@code journeyCode} must not be null or blank.
 *   <li>{@code semester} must be ≥ 1 (semesters are 1-based, positive integers).
 * </ul>
 *
 * <p>No Spring/JPA imports.
 */
public record RecommendedSemester(String journeyCode, int semester) {

  public RecommendedSemester {
    if (journeyCode == null || journeyCode.isBlank()) {
      throw new IllegalArgumentException("journeyCode must not be null or blank");
    }
    if (semester <= 0) {
      throw new IllegalArgumentException("semester must be >= 1, got: " + semester);
    }
  }
}
