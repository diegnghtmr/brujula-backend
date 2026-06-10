package co.edu.uniquindio.brujula.programcatalog.application.service;

import co.edu.uniquindio.brujula.programcatalog.application.dto.StudyPlanView;
import co.edu.uniquindio.brujula.programcatalog.application.port.in.GetStudyPlanQuery;
import co.edu.uniquindio.brujula.programcatalog.application.port.out.LoadStudyPlanPort;
import co.edu.uniquindio.brujula.programcatalog.domain.PlanId;
import co.edu.uniquindio.brujula.programcatalog.domain.PlanStatus;
import co.edu.uniquindio.brujula.programcatalog.domain.StudyPlan;
import co.edu.uniquindio.brujula.programcatalog.domain.StudyPlanNotFoundException;
import java.util.UUID;

/**
 * Use case implementation: get a single study plan by ID.
 *
 * <p>Applies role-based visibility: STUDENT sees only ACTIVE plans, STAFF/ADMIN see all. Throws
 * {@link StudyPlanNotFoundException} (maps to HTTP 404) if the plan is not found or not visible to
 * the caller.
 *
 * <p>No Spring annotations — wired via infrastructure configuration (ARQ-002).
 */
public class GetStudyPlanService implements GetStudyPlanQuery {

  private final LoadStudyPlanPort loadStudyPlanPort;

  public GetStudyPlanService(LoadStudyPlanPort loadStudyPlanPort) {
    this.loadStudyPlanPort = loadStudyPlanPort;
  }

  @Override
  public StudyPlanView getStudyPlan(UUID rawId, String callerRole) {
    PlanId planId = new PlanId(rawId);
    StudyPlan plan =
        loadStudyPlanPort
            .loadById(planId)
            .orElseThrow(() -> new StudyPlanNotFoundException(planId));

    // Apply visibility: STUDENT (or unknown role) sees ACTIVE plans only
    if (!isPrivilegedRole(callerRole) && plan.status() != PlanStatus.ACTIVE) {
      throw new StudyPlanNotFoundException(planId);
    }

    return toView(plan);
  }

  // ---------------------------------------------------------------------------
  // Role helper
  // ---------------------------------------------------------------------------

  private boolean isPrivilegedRole(String callerRole) {
    return "STAFF".equalsIgnoreCase(callerRole) || "ADMIN".equalsIgnoreCase(callerRole);
  }

  // ---------------------------------------------------------------------------
  // Domain → DTO mapper
  // ---------------------------------------------------------------------------

  private StudyPlanView toView(StudyPlan plan) {
    return new StudyPlanView(
        plan.id().value(),
        plan.programId(),
        plan.name(),
        plan.status().name(),
        plan.totalCredits(),
        plan.referenceYear(),
        plan.version());
  }
}
