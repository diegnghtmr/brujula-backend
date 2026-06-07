package co.edu.uniquindio.brujula.curriculum.application.port.out;

import co.edu.uniquindio.brujula.curriculum.domain.DegreeWorkModality;
import java.util.List;
import java.util.UUID;

/**
 * Output port: load degree work modalities from persistence.
 *
 * <p>Named by need (ISP) per hexagonal architecture conventions. No Spring/framework imports.
 */
public interface LoadDegreeWorkModalityPort {

  /**
   * Load all degree work modalities for a study plan.
   *
   * @param planId the study plan UUID
   * @return all modalities (all statuses — filtering happens in the service if needed)
   */
  List<DegreeWorkModality> loadByPlanId(UUID planId);
}
