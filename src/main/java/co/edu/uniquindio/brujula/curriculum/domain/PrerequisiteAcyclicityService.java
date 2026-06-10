package co.edu.uniquindio.brujula.curriculum.domain;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Domain service that validates the prerequisite graph of a study plan.
 *
 * <p>Enforces two invariants (CUR-003 and CUR-004):
 *
 * <ol>
 *   <li><strong>Acyclicity</strong>: the COURSE_PASSED / SEQUENCE requirement graph must be a
 *       directed acyclic graph (DAG). Uses 3-color DFS (WHITE → GRAY → BLACK). A GRAY-to-GRAY
 *       back-edge indicates a cycle → {@link PrerequisiteCycleException}.
 *   <li><strong>Semester ordering</strong>: for every requirement edge, the required course's
 *       recommended semester (per the same journey) must be ≤ the target course's recommended
 *       semester (i.e. the prerequisite must be in the same or earlier semester, not a later one).
 *       Validated via {@link #validateSemesterOrdering}.
 * </ol>
 *
 * <p>Both checks are merged into a single {@link #validate} pass.
 *
 * <p>ADR-F2-006: This service is framework-free — ZERO imports from {@code org.springframework} or
 * {@code jakarta.persistence}. ArchUnit test {@code WU1-3.5} enforces this at build time.
 *
 * <p>No Spring/JPA imports (ARQ-002).
 */
public class PrerequisiteAcyclicityService {

  /** DFS node color for cycle detection. */
  private enum Color {
    WHITE, // not yet visited
    GRAY, // currently in the DFS stack (ancestor path)
    BLACK // fully processed
  }

  // ---------------------------------------------------------------------------
  // Public API
  // ---------------------------------------------------------------------------

  /**
   * Validate only the acyclicity of the given graph.
   *
   * @throws PrerequisiteCycleException if any cycle is detected
   */
  public void validateAcyclicity(PrerequisiteGraph graph) {
    Map<UUID, Color> colors = new HashMap<>();
    for (UUID node : graph.nodes()) {
      colors.put(node, Color.WHITE);
    }
    for (UUID node : graph.nodes()) {
      if (colors.get(node) == Color.WHITE) {
        dfsVisit(graph, node, colors);
      }
    }
  }

  /**
   * Validate both acyclicity and semester ordering in a single pass.
   *
   * @param graph the prerequisite graph to validate
   * @param requirements the list of course requirements providing semester metadata
   * @param courses the list of courses providing semester information per journey
   * @throws PrerequisiteCycleException if a cycle is detected
   * @throws SemesterOrderingViolationException if a forward-semester dependency is found
   */
  public void validate(
      PrerequisiteGraph graph, List<CourseRequirement> requirements, List<Course> courses) {
    validateAcyclicity(graph);
    validateSemesterOrdering(requirements, courses);
  }

  // ---------------------------------------------------------------------------
  // Acyclicity — 3-color DFS
  // ---------------------------------------------------------------------------

  /**
   * DFS visit: marks the node GRAY (in-stack), recurses on successors, marks BLACK when done. A
   * successor already colored GRAY indicates a back-edge → cycle.
   */
  private void dfsVisit(PrerequisiteGraph graph, UUID node, Map<UUID, Color> colors) {
    colors.put(node, Color.GRAY);
    for (UUID successor : graph.successors(node)) {
      Color successorColor = colors.getOrDefault(successor, Color.WHITE);
      if (successorColor == Color.GRAY) {
        // Back-edge detected: cycle found
        throw new PrerequisiteCycleException(
            "Prerequisite cycle detected: edge from "
                + node
                + " to "
                + successor
                + " creates a cycle (back-edge to in-stack node)");
      }
      if (successorColor == Color.WHITE) {
        dfsVisit(graph, successor, colors);
      }
      // BLACK: already fully processed, no cycle through this path
    }
    colors.put(node, Color.BLACK);
  }

  // ---------------------------------------------------------------------------
  // Semester ordering validation (CUR-004)
  // ---------------------------------------------------------------------------

  /**
   * Validate semester ordering: for every COURSE_PASSED/SEQUENCE requirement, the required course's
   * recommended semester must be ≤ the target course's recommended semester (per the same journey).
   * Cross-journey comparisons are NOT enforced.
   *
   * @throws SemesterOrderingViolationException if any forward dependency is found
   */
  public void validateSemesterOrdering(List<CourseRequirement> requirements, List<Course> courses) {
    Map<UUID, Course> courseMap = buildCourseMap(courses);
    for (CourseRequirement req : requirements) {
      if (req.requiredCourseId() == null) {
        continue; // MINIMUM_CREDITS / CONFIGURABLE have no specific course predecessor
      }
      Course requiredCourse = courseMap.get(req.requiredCourseId());
      Course targetCourse = courseMap.get(req.targetCourseId());
      if (requiredCourse == null || targetCourse == null) {
        continue; // incomplete graph: skip (handled at seed validation)
      }
      checkSemesterOrderPerJourney(req, requiredCourse, targetCourse);
    }
  }

  private void checkSemesterOrderPerJourney(
      CourseRequirement req, Course requiredCourse, Course targetCourse) {
    for (RecommendedSemester reqSem : requiredCourse.recommendedSemesters()) {
      String journey = reqSem.journeyCode();
      // Find matching semester for the target course in the same journey
      targetCourse.recommendedSemesters().stream()
          .filter(s -> s.journeyCode().equals(journey))
          .findFirst()
          .ifPresent(
              targetSem -> {
                if (reqSem.semester() > targetSem.semester()) {
                  throw new SemesterOrderingViolationException(
                      req.requiredCourseId(),
                      req.targetCourseId(),
                      journey,
                      reqSem.semester(),
                      targetSem.semester());
                }
              });
    }
  }

  private Map<UUID, Course> buildCourseMap(List<Course> courses) {
    Map<UUID, Course> map = new HashMap<>();
    for (Course course : courses) {
      map.put(course.id().value(), course);
    }
    return map;
  }
}
