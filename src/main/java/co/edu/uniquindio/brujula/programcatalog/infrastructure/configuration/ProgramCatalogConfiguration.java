package co.edu.uniquindio.brujula.programcatalog.infrastructure.configuration;

import co.edu.uniquindio.brujula.programcatalog.application.port.out.LoadJourneyPort;
import co.edu.uniquindio.brujula.programcatalog.application.port.out.LoadProgramPort;
import co.edu.uniquindio.brujula.programcatalog.application.port.out.LoadStudyPlanPort;
import co.edu.uniquindio.brujula.programcatalog.application.service.GetProgramService;
import co.edu.uniquindio.brujula.programcatalog.application.service.GetStudyPlanService;
import co.edu.uniquindio.brujula.programcatalog.application.service.ListJourneysService;
import co.edu.uniquindio.brujula.programcatalog.application.service.ListProgramPlansService;
import co.edu.uniquindio.brujula.programcatalog.application.service.ListProgramsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring bean wiring for the programcatalog module's application services.
 *
 * <p>Bridges domain/application (framework-free) into the Spring container. Infrastructure adapters
 * are auto-detected via {@code @Component}; application services are registered here to maintain
 * explicit wiring (ARQ-002).
 *
 * <p>Wired services (read-only catalog query use cases — Phase 3 WU4):
 *
 * <ul>
 *   <li>{@link ListProgramsService} — CAT-001 list programs with status filter + pagination
 *   <li>{@link GetProgramService} — CAT-001 get single program by ID
 *   <li>{@link ListProgramPlansService} — CAT-002 list study plans for a program
 *   <li>{@link GetStudyPlanService} — CAT-002 get single study plan by ID
 *   <li>{@link ListJourneysService} — CAT-003 list active journeys
 * </ul>
 */
@Configuration
class ProgramCatalogConfiguration {

  @Bean
  ListProgramsService listProgramsService(LoadProgramPort loadProgramPort) {
    return new ListProgramsService(loadProgramPort);
  }

  @Bean
  GetProgramService getProgramService(LoadProgramPort loadProgramPort) {
    return new GetProgramService(loadProgramPort);
  }

  @Bean
  ListProgramPlansService listProgramPlansService(LoadStudyPlanPort loadStudyPlanPort) {
    return new ListProgramPlansService(loadStudyPlanPort);
  }

  @Bean
  GetStudyPlanService getStudyPlanService(LoadStudyPlanPort loadStudyPlanPort) {
    return new GetStudyPlanService(loadStudyPlanPort);
  }

  @Bean
  ListJourneysService listJourneysService(LoadJourneyPort loadJourneyPort) {
    return new ListJourneysService(loadJourneyPort);
  }
}
