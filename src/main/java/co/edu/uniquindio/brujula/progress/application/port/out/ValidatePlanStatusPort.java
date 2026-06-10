package co.edu.uniquindio.brujula.progress.application.port.out;

import java.util.UUID;

/**
 * Output port: validate that a study plan is in a given status.
 *
 * <p>FIX-W3 (SUB-1-INACTIVE-PLAN): the application service must not depend directly on {@code
 * programcatalog.application.port.in.GetStudyPlanQuery} to avoid violating Spring Modulith
 * named-interface boundaries. This port is a local anti-corruption facade that wraps the
 * programcatalog port and is implemented in the infrastructure layer.
 *
 * <p>No Spring/JPA imports (ARQ-003 — application layer is framework-free). The implementation
 * lives in {@code progress.infrastructure.adapter.out.programcatalog}.
 */
public interface ValidatePlanStatusPort {

  /**
   * Return the status string of the plan (e.g. "ACTIVE", "ARCHIVED", "DRAFT"), or {@code null} if
   * the plan does not exist.
   *
   * @param planId the plan UUID to look up
   * @return the plan status string, or {@code null} if not found
   */
  String getPlanStatus(UUID planId);
}
