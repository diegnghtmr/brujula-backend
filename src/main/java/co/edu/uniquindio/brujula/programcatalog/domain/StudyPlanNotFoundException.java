package co.edu.uniquindio.brujula.programcatalog.domain;

/**
 * Domain exception thrown when a study plan is not found by its ID.
 *
 * <p>Maps to HTTP 404 (Not Found) at the REST adapter layer (CAT-002, GetStudyPlanQuery contract).
 * No Spring/JPA imports (ARQ-002).
 */
public class StudyPlanNotFoundException extends RuntimeException {

  public StudyPlanNotFoundException(PlanId planId) {
    super("Study plan not found: " + planId.value());
  }
}
