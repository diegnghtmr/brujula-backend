package co.edu.uniquindio.brujula.simulation.infrastructure.configuration;

import co.edu.uniquindio.brujula.sharedkernel.application.port.out.IdempotencyStorePort;
import co.edu.uniquindio.brujula.sharedkernel.application.port.out.JsonCodecPort;
import co.edu.uniquindio.brujula.simulation.application.port.in.CreateSimulationUseCase;
import co.edu.uniquindio.brujula.simulation.application.port.in.DeleteSimulationUseCase;
import co.edu.uniquindio.brujula.simulation.application.port.in.DuplicateSimulationUseCase;
import co.edu.uniquindio.brujula.simulation.application.port.in.GetSimulationQuery;
import co.edu.uniquindio.brujula.simulation.application.port.in.ListMySimulationsQuery;
import co.edu.uniquindio.brujula.simulation.application.port.in.UpdateSimulationUseCase;
import co.edu.uniquindio.brujula.simulation.application.port.out.LoadSimulationPort;
import co.edu.uniquindio.brujula.simulation.application.port.out.OfferingReadPort;
import co.edu.uniquindio.brujula.simulation.application.port.out.SaveSimulationPort;
import co.edu.uniquindio.brujula.simulation.application.service.CreateSimulationService;
import co.edu.uniquindio.brujula.simulation.application.service.DeleteSimulationService;
import co.edu.uniquindio.brujula.simulation.application.service.DuplicateSimulationService;
import co.edu.uniquindio.brujula.simulation.application.service.GetSimulationService;
import co.edu.uniquindio.brujula.simulation.application.service.ListMySimulationsService;
import co.edu.uniquindio.brujula.simulation.application.service.UpdateSimulationService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring bean wiring for the simulation module's application services (WU3).
 *
 * <p>Framework-free application services registered here for explicit constructor injection.
 * Persistence adapters and the offering read adapter are auto-detected via {@code @Component}.
 * All port.out dependencies injected as interfaces (ARQ-006).
 *
 * <p>WU3 wires: CreateSimulationService, ListMySimulationsService, GetSimulationService (skeleton),
 * UpdateSimulationService, DeleteSimulationService, DuplicateSimulationService.
 *
 * <p>WU4/WU5 will extend this configuration with selection and detail-assembly services.
 */
@Configuration
class SimulationConfiguration {

  @Bean
  CreateSimulationUseCase createSimulationService(
      LoadSimulationPort loadSimulationPort,
      SaveSimulationPort saveSimulationPort,
      OfferingReadPort offeringReadPort,
      IdempotencyStorePort idempotencyStorePort,
      JsonCodecPort jsonCodecPort) {
    return new CreateSimulationService(
        loadSimulationPort,
        saveSimulationPort,
        offeringReadPort,
        idempotencyStorePort,
        jsonCodecPort);
  }

  @Bean
  ListMySimulationsQuery listMySimulationsService(
      LoadSimulationPort loadSimulationPort,
      OfferingReadPort offeringReadPort) {
    return new ListMySimulationsService(loadSimulationPort, offeringReadPort);
  }

  @Bean
  GetSimulationQuery getSimulationService(
      LoadSimulationPort loadSimulationPort,
      OfferingReadPort offeringReadPort) {
    return new GetSimulationService(loadSimulationPort, offeringReadPort);
  }

  @Bean
  UpdateSimulationUseCase updateSimulationService(
      LoadSimulationPort loadSimulationPort,
      SaveSimulationPort saveSimulationPort,
      OfferingReadPort offeringReadPort) {
    return new UpdateSimulationService(loadSimulationPort, saveSimulationPort, offeringReadPort);
  }

  @Bean
  DeleteSimulationUseCase deleteSimulationService(
      LoadSimulationPort loadSimulationPort,
      SaveSimulationPort saveSimulationPort) {
    return new DeleteSimulationService(loadSimulationPort, saveSimulationPort);
  }

  @Bean
  DuplicateSimulationUseCase duplicateSimulationService(
      LoadSimulationPort loadSimulationPort,
      SaveSimulationPort saveSimulationPort,
      OfferingReadPort offeringReadPort,
      IdempotencyStorePort idempotencyStorePort,
      JsonCodecPort jsonCodecPort) {
    return new DuplicateSimulationService(
        loadSimulationPort,
        saveSimulationPort,
        offeringReadPort,
        idempotencyStorePort,
        jsonCodecPort);
  }
}
