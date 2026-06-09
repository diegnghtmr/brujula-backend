package co.edu.uniquindio.brujula.programcatalog.application.service;

import co.edu.uniquindio.brujula.programcatalog.application.dto.StudyPlanView;
import co.edu.uniquindio.brujula.programcatalog.application.port.in.ListProgramPlansQuery;
import co.edu.uniquindio.brujula.programcatalog.application.port.out.LoadStudyPlanPort;
import co.edu.uniquindio.brujula.programcatalog.domain.PlanStatus;
import co.edu.uniquindio.brujula.programcatalog.domain.StudyPlan;
import java.util.List;
import java.util.UUID;

/**
 * Use case implementation: list study plans for an academic program.
 *
 * <p>Filters DRAFT and ARCHIVED plans when the caller is a STUDENT (CAT-002). STAFF and ADMIN see
 * all statuses. No Spring annotations — wiring is handled in the infrastructure configuration layer
 * (ARQ-002).
 */
public class ListProgramPlansService implements ListProgramPlansQuery {

  private final LoadStudyPlanPort loadStudyPlanPort;

  public ListProgramPlansService(LoadStudyPlanPort loadStudyPlanPort) {
    this.loadStudyPlanPort = loadStudyPlanPort;
  }

  @Override
  public List<StudyPlanView> listProgramPlans(UUID programId, String callerRole) {
    List<StudyPlan> plans = loadStudyPlanPort.loadByProgramId(programId);
    return plans.stream().filter(plan -> isVisible(plan, callerRole)).map(this::toView).toList();
  }

  // ---------------------------------------------------------------------------
  // Role-based visibility filter
  // ---------------------------------------------------------------------------

  /**
   * Returns {@code true} if the plan is visible to the caller.
   *
   * <p>Uses an ALLOWLIST for privileged access: only {@code "STAFF"} and {@code "ADMIN"} may see
   * DRAFT and ARCHIVED plans. Everything else — including {@code null} and unknown role strings —
   * is treated as unprivileged and sees only ACTIVE plans (CAT-002: unauthenticated =
   * student-level).
   */
  private boolean isVisible(StudyPlan plan, String callerRole) {
    if (isPrivilegedRole(callerRole)) {
      return true; // STAFF and ADMIN see all statuses
    }
    // STUDENT, null, or any unknown role: only ACTIVE plans are visible
    return plan.status() == PlanStatus.ACTIVE;
  }

  /**
   * Returns {@code true} only for explicitly privileged roles.
   *
   * <p>Allowlist: {@code "STAFF"} and {@code "ADMIN"}. {@code null} and any other value returns
   * {@code false}, defaulting to the most restrictive visibility.
   */
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
