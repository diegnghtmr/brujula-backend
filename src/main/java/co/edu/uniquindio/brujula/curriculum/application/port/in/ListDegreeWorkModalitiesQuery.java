package co.edu.uniquindio.brujula.curriculum.application.port.in;

import co.edu.uniquindio.brujula.curriculum.application.dto.DegreeWorkModalityView;
import java.util.List;
import java.util.UUID;

/**
 * Input port: list degree work modalities for a study plan.
 *
 * <p>Backs {@code GET /plans/{planId}/degree-work-modalities} (CUR-006). No Spring/framework imports.
 */
public interface ListDegreeWorkModalitiesQuery {

  /**
   * Return all degree work modalities for the given study plan.
   *
   * @param planId the plan UUID
   * @return the list of {@link DegreeWorkModalityView} DTOs
   */
  List<DegreeWorkModalityView> listDegreeWorkModalities(UUID planId);
}
