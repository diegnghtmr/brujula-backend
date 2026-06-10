package co.edu.uniquindio.brujula.curriculum.application.port.in;

import co.edu.uniquindio.brujula.curriculum.application.dto.PlanMallaView;
import java.util.UUID;

/**
 * Input port: assemble the plan-level malla (curricular grid) for a given journey.
 *
 * <p>Backs {@code GET /plans/{planId}/malla?journeyCode=...} (design ADR-F2-001). Returns ACTIVE
 * courses grouped by recommended semester for the requested journey code. Assembly is done
 * in-memory: one indexed query per plan + group-by-semester in Java (&lt;500ms p95).
 *
 * <p>No Spring/framework imports (ARQ-003).
 */
public interface AssemblePlanMallaQuery {

  /**
   * Assemble the malla view for a study plan and journey code.
   *
   * @param planId the study plan UUID
   * @param journeyCode the journey code to use for semester lookup (e.g. "DIURNA", "NOCTURNA")
   * @return {@link PlanMallaView} with semester buckets ordered ascending; never null
   */
  PlanMallaView assemblePlanMalla(UUID planId, String journeyCode);
}
