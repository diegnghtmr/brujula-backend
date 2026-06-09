package co.edu.uniquindio.brujula.simulation.infrastructure.configuration;

import co.edu.uniquindio.brujula.sharedkernel.application.port.out.IdempotencyStorePort;
import co.edu.uniquindio.brujula.sharedkernel.application.port.out.JsonCodecPort;
import co.edu.uniquindio.brujula.simulation.application.port.in.AddSimulationSelectionUseCase;
import co.edu.uniquindio.brujula.simulation.application.port.in.CreateSimulationUseCase;
import co.edu.uniquindio.brujula.simulation.application.port.in.DeleteSimulationUseCase;
import co.edu.uniquindio.brujula.simulation.application.port.in.DuplicateSimulationUseCase;
import co.edu.uniquindio.brujula.simulation.application.port.in.GetSimulationQuery;
import co.edu.uniquindio.brujula.simulation.application.port.in.ListMySimulationsQuery;
import co.edu.uniquindio.brujula.simulation.application.port.in.ListSimulationSelectionsQuery;
import co.edu.uniquindio.brujula.simulation.application.port.in.RemoveSimulationSelectionUseCase;
import co.edu.uniquindio.brujula.simulation.application.port.in.ReviewSimulationUseCase;
import co.edu.uniquindio.brujula.simulation.application.port.in.UpdateSimulationUseCase;
import co.edu.uniquindio.brujula.simulation.application.port.out.CreditLimitPort;
import co.edu.uniquindio.brujula.simulation.application.port.out.CurriculumReadPort;
import co.edu.uniquindio.brujula.simulation.application.port.out.LoadSimulationPort;
import co.edu.uniquindio.brujula.simulation.application.port.out.LoadSimulationSelectionPort;
import co.edu.uniquindio.brujula.simulation.application.port.out.OfferingReadPort;
import co.edu.uniquindio.brujula.simulation.application.port.out.ProgramNameReadPort;
import co.edu.uniquindio.brujula.simulation.application.port.out.ProgressReadPort;
import co.edu.uniquindio.brujula.simulation.application.port.out.SaveSimulationPort;
import co.edu.uniquindio.brujula.simulation.application.port.out.SaveSimulationSelectionPort;
import co.edu.uniquindio.brujula.simulation.application.service.AddSimulationSelectionService;
import co.edu.uniquindio.brujula.simulation.application.service.CreateSimulationService;
import co.edu.uniquindio.brujula.simulation.application.service.DeleteSimulationService;
import co.edu.uniquindio.brujula.simulation.application.service.DuplicateSimulationService;
import co.edu.uniquindio.brujula.simulation.application.service.GetSimulationService;
import co.edu.uniquindio.brujula.simulation.application.service.ListMySimulationsService;
import co.edu.uniquindio.brujula.simulation.application.service.ListSimulationSelectionsService;
import co.edu.uniquindio.brujula.simulation.application.service.RemoveSimulationSelectionService;
import co.edu.uniquindio.brujula.simulation.application.service.ReviewSimulationService;
import co.edu.uniquindio.brujula.simulation.application.service.UpdateSimulationService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring bean wiring for the simulation module's application services.
 *
 * <p>WU3: CreateSimulationService, ListMySimulationsService, GetSimulationService,
 * UpdateSimulationService, DeleteSimulationService, DuplicateSimulationService.
 *
 * <p>WU4a: AddSimulationSelectionService, RemoveSimulationSelectionService,
 * ListSimulationSelectionsService.
 *
 * <p>Framework-free application services registered here for explicit constructor injection.
 * Persistence adapters and cross-module adapters are auto-detected via {@code @Component}.
 */
@Configuration
class SimulationConfiguration {

  // -------------------------------------------------------------------------
  // WU3 services
  // -------------------------------------------------------------------------

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
      LoadSimulationPort loadSimulationPort, OfferingReadPort offeringReadPort) {
    return new ListMySimulationsService(loadSimulationPort, offeringReadPort);
  }

  @Bean
  GetSimulationQuery getSimulationService(
      LoadSimulationPort loadSimulationPort,
      LoadSimulationSelectionPort loadSimulationSelectionPort,
      OfferingReadPort offeringReadPort,
      CreditLimitPort creditLimitPort,
      ProgressReadPort progressReadPort,
      CurriculumReadPort curriculumReadPort,
      ProgramNameReadPort programNameReadPort) {
    return new GetSimulationService(
        loadSimulationPort,
        loadSimulationSelectionPort,
        offeringReadPort,
        creditLimitPort,
        progressReadPort,
        curriculumReadPort,
        programNameReadPort);
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
      LoadSimulationPort loadSimulationPort, SaveSimulationPort saveSimulationPort) {
    return new DeleteSimulationService(loadSimulationPort, saveSimulationPort);
  }

  @Bean
  DuplicateSimulationUseCase duplicateSimulationService(
      LoadSimulationPort loadSimulationPort,
      SaveSimulationPort saveSimulationPort,
      OfferingReadPort offeringReadPort,
      LoadSimulationSelectionPort loadSimulationSelectionPort,
      SaveSimulationSelectionPort saveSimulationSelectionPort,
      IdempotencyStorePort idempotencyStorePort,
      JsonCodecPort jsonCodecPort) {
    return new DuplicateSimulationService(
        loadSimulationPort,
        saveSimulationPort,
        offeringReadPort,
        loadSimulationSelectionPort,
        saveSimulationSelectionPort,
        idempotencyStorePort,
        jsonCodecPort);
  }

  // -------------------------------------------------------------------------
  // WU4a services
  // -------------------------------------------------------------------------

  @Bean
  AddSimulationSelectionUseCase addSimulationSelectionService(
      LoadSimulationPort loadSimulationPort,
      LoadSimulationSelectionPort loadSimulationSelectionPort,
      SaveSimulationSelectionPort saveSimulationSelectionPort,
      OfferingReadPort offeringReadPort,
      CreditLimitPort creditLimitPort,
      ProgressReadPort progressReadPort,
      CurriculumReadPort curriculumReadPort,
      ProgramNameReadPort programNameReadPort,
      IdempotencyStorePort idempotencyStorePort,
      JsonCodecPort jsonCodecPort) {
    return new AddSimulationSelectionService(
        loadSimulationPort,
        loadSimulationSelectionPort,
        saveSimulationSelectionPort,
        offeringReadPort,
        creditLimitPort,
        progressReadPort,
        curriculumReadPort,
        programNameReadPort,
        idempotencyStorePort,
        jsonCodecPort);
  }

  @Bean
  RemoveSimulationSelectionUseCase removeSimulationSelectionService(
      LoadSimulationPort loadSimulationPort,
      LoadSimulationSelectionPort loadSimulationSelectionPort,
      SaveSimulationSelectionPort saveSimulationSelectionPort) {
    return new RemoveSimulationSelectionService(
        loadSimulationPort, loadSimulationSelectionPort, saveSimulationSelectionPort);
  }

  @Bean
  ListSimulationSelectionsQuery listSimulationSelectionsService(
      LoadSimulationPort loadSimulationPort,
      LoadSimulationSelectionPort loadSimulationSelectionPort) {
    return new ListSimulationSelectionsService(loadSimulationPort, loadSimulationSelectionPort);
  }

  // -------------------------------------------------------------------------
  // WU5 services
  // -------------------------------------------------------------------------

  @Bean
  ReviewSimulationUseCase reviewSimulationService(
      LoadSimulationPort loadSimulationPort,
      LoadSimulationSelectionPort loadSimulationSelectionPort,
      SaveSimulationSelectionPort saveSimulationSelectionPort,
      OfferingReadPort offeringReadPort,
      CreditLimitPort creditLimitPort,
      ProgressReadPort progressReadPort,
      CurriculumReadPort curriculumReadPort,
      ProgramNameReadPort programNameReadPort,
      GetSimulationQuery getSimulationQuery) {
    return new ReviewSimulationService(
        loadSimulationPort,
        loadSimulationSelectionPort,
        saveSimulationSelectionPort,
        offeringReadPort,
        creditLimitPort,
        progressReadPort,
        curriculumReadPort,
        programNameReadPort,
        getSimulationQuery);
  }
}
