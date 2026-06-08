package co.edu.uniquindio.brujula.simulation.infrastructure.adapter.in.rest;

import co.edu.uniquindio.brujula.generated.model.Simulation;
import co.edu.uniquindio.brujula.generated.model.SimulationPage;
import co.edu.uniquindio.brujula.generated.model.SimulationStatus;
import co.edu.uniquindio.brujula.simulation.application.dto.SimulationPageView;
import co.edu.uniquindio.brujula.simulation.application.dto.SimulationView;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Maps between simulation application-layer views and generated API models.
 *
 * <p>Package-private per ARQ-006b.
 */
@Component
class SimulationRestMapper {

  Simulation toDto(SimulationView view) {
    OffsetDateTime createdAt = view.createdAt() != null
        ? view.createdAt().atOffset(ZoneOffset.UTC)
        : OffsetDateTime.now(ZoneOffset.UTC);
    OffsetDateTime updatedAt = view.updatedAt() != null
        ? view.updatedAt().atOffset(ZoneOffset.UTC)
        : OffsetDateTime.now(ZoneOffset.UTC);

    SimulationStatus status = parseStatus(view.status());

    return new Simulation(
        view.id(),
        view.name(),
        view.periodId(),
        view.periodName(),
        view.programIds(),
        status,
        createdAt,
        updatedAt,
        view.version());
  }

  SimulationPage toPageDto(SimulationPageView pageView) {
    List<Simulation> content = pageView.content().stream().map(this::toDto).toList();
    return new SimulationPage(
        pageView.page(),
        pageView.size(),
        pageView.totalElements(),
        pageView.totalPages(),
        content);
  }

  private SimulationStatus parseStatus(String status) {
    if (status == null) return SimulationStatus.CURRENT;
    return switch (status) {
      case "OUTDATED" -> SimulationStatus.OUTDATED;
      default -> SimulationStatus.CURRENT;
    };
  }
}
