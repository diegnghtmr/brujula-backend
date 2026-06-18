package co.edu.uniquindio.brujula.progress.infrastructure.adapter.out.programcatalog;

import co.edu.uniquindio.brujula.programcatalog.application.dto.StudyPlanView;
import co.edu.uniquindio.brujula.programcatalog.application.port.in.GetStudyPlanQuery;
import co.edu.uniquindio.brujula.programcatalog.domain.StudyPlanNotFoundException;
import co.edu.uniquindio.brujula.progress.application.port.out.ValidatePlanStatusPort;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Driven adapter: validates study plan status by delegating to {@link GetStudyPlanQuery}.
 *
 * <p>Implements the {@link ValidatePlanStatusPort} anti-corruption façade so that the {@code
 * progress.application} layer does NOT directly import programcatalog types (Spring Modulith
 * named-interface boundary). The programcatalog import is legal here because this class is in
 * {@code progress.infrastructure}, which may depend on programcatalog.
 *
 * <p>Package-private. Wired by {@link
 * co.edu.uniquindio.brujula.progress.infrastructure.configuration.ProgressConfiguration}.
 */
@Component
class ProgramCatalogPlanStatusAdapter implements ValidatePlanStatusPort {

  private final GetStudyPlanQuery getStudyPlanQuery;

  ProgramCatalogPlanStatusAdapter(GetStudyPlanQuery getStudyPlanQuery) {
    this.getStudyPlanQuery = getStudyPlanQuery;
  }

  @Override
  public String getPlanStatus(UUID planId) {
    try {
      // Use ADMIN role to see the real status (ACTIVE, ARCHIVED, DRAFT) — not filtered by role.
      StudyPlanView view = getStudyPlanQuery.getStudyPlan(planId, "ADMIN");
      return view.status();
    } catch (StudyPlanNotFoundException e) {
      // Plan not found — return null to let the caller decide (likely 422 or 404)
      return null;
    }
  }
}
