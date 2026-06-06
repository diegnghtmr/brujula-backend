package co.edu.uniquindio.brujula.programcatalog.application.port.in;

import co.edu.uniquindio.brujula.programcatalog.application.dto.StudyPlanView;
import java.util.List;
import java.util.UUID;

/**
 * Input port: list study plans for a given academic program.
 *
 * <p>Backs {@code GET /programs/{programId}/plans} (CAT-002). When {@code callerRole} is
 * {@code STUDENT}, only ACTIVE plans are returned. DRAFT and ARCHIVED require STAFF/ADMIN role.
 *
 * <p>No Spring/framework imports (ARQ-003).
 */
public interface ListProgramPlansQuery {

  /**
   * Return study plans for a program, filtered by visibility for the caller's role.
   *
   * @param programId the program UUID
   * @param callerRole the role of the requesting user (e.g. "STUDENT", "STAFF", "ADMIN")
   * @return list of {@link StudyPlanView} DTOs (never null; may be empty)
   */
  List<StudyPlanView> listProgramPlans(UUID programId, String callerRole);
}
