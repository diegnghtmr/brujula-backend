package co.edu.uniquindio.brujula.curriculum.application.service;

import co.edu.uniquindio.brujula.curriculum.application.dto.DegreeWorkModalityView;
import co.edu.uniquindio.brujula.curriculum.application.port.in.ListDegreeWorkModalitiesQuery;
import co.edu.uniquindio.brujula.curriculum.application.port.out.LoadDegreeWorkModalityPort;
import co.edu.uniquindio.brujula.curriculum.domain.DegreeWorkModality;
import java.util.List;
import java.util.UUID;

/**
 * Use case implementation: list degree work modalities for a study plan.
 *
 * <p>Delegates to {@link LoadDegreeWorkModalityPort#loadByPlanId}. Returns all modalities
 * regardless of status (the REST layer can filter if needed). No Spring annotations — wired via
 * infrastructure configuration (ARQ-002).
 */
public class ListDegreeWorkModalitiesService implements ListDegreeWorkModalitiesQuery {

  private final LoadDegreeWorkModalityPort loadDegreeWorkModalityPort;

  public ListDegreeWorkModalitiesService(LoadDegreeWorkModalityPort loadDegreeWorkModalityPort) {
    this.loadDegreeWorkModalityPort = loadDegreeWorkModalityPort;
  }

  @Override
  public List<DegreeWorkModalityView> listDegreeWorkModalities(UUID planId) {
    return loadDegreeWorkModalityPort.loadByPlanId(planId).stream().map(this::toView).toList();
  }

  // ---------------------------------------------------------------------------
  // Domain → DTO mapper
  // ---------------------------------------------------------------------------

  private DegreeWorkModalityView toView(DegreeWorkModality modality) {
    return new DegreeWorkModalityView(
        modality.id().value(),
        modality.planId(),
        modality.name(),
        modality.credits(),
        modality.curricularWeeklyHours(),
        modality.status().name(),
        modality.version());
  }
}
