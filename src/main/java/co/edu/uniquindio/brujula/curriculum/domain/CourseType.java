package co.edu.uniquindio.brujula.curriculum.domain;

/**
 * Classification of a course within a curricular plan.
 *
 * <p>No Spring/JPA imports. Mirrors the CourseType enum in the OpenAPI contract.
 *
 * <ul>
 *   <li>{@link #REQUIRED} — mandatory course within the core curriculum track.
 *   <li>{@link #PERSONAL_ELECTIVE} — personal formation elective (electiva de formación personal).
 *   <li>{@link #DEEPENING} — deepening/profundización course in a professional line.
 *   <li>{@link #DEGREE_WORK} — degree-work option (modalidad de trabajo de grado).
 *   <li>{@link #INSTITUTIONAL} — institutional/transversal course.
 *   <li>{@link #OPTIONAL} — freely optional course not aligned to a track.
 * </ul>
 */
public enum CourseType {
  REQUIRED,
  PERSONAL_ELECTIVE,
  DEEPENING,
  DEGREE_WORK,
  INSTITUTIONAL,
  OPTIONAL
}
