package co.edu.uniquindio.brujula.programcatalog.application.port.in;

import co.edu.uniquindio.brujula.programcatalog.application.dto.StudyPlanView;
import java.util.UUID;

/**
 * Input port: retrieve a single study plan by ID.
 *
 * <p>Backs {@code GET /plans/{planId}}. Throws {@link
 * co.edu.uniquindio.brujula.programcatalog.domain.StudyPlanNotFoundException} (mapped to HTTP 404)
 * when the plan does not exist.
 *
 * <p>The caller role is used for visibility filtering: STUDENTs see only ACTIVE plans; STAFF/ADMIN
 * see all statuses.
 *
 * <p>Accepts plain {@link UUID} (not a domain VO) so REST adapters do not need to import domain
 * types. No Spring/framework imports.
 */
public interface GetStudyPlanQuery {

  /**
   * Return the study plan with the given ID, if visible to the caller.
   *
   * @param planId the plan UUID
   * @param callerRole the role of the requesting user (e.g. "STUDENT", "STAFF", "ADMIN")
   * @return the {@link StudyPlanView} DTO
   * @throws co.edu.uniquindio.brujula.programcatalog.domain.StudyPlanNotFoundException if not found
   */
  StudyPlanView getStudyPlan(UUID planId, String callerRole);
}
