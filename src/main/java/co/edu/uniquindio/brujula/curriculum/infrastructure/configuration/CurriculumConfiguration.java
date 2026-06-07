package co.edu.uniquindio.brujula.curriculum.infrastructure.configuration;

import co.edu.uniquindio.brujula.curriculum.application.port.out.LoadCoursePort;
import co.edu.uniquindio.brujula.curriculum.application.port.out.LoadCourseRequirementPort;
import co.edu.uniquindio.brujula.curriculum.application.port.out.LoadDegreeWorkModalityPort;
import co.edu.uniquindio.brujula.curriculum.application.port.out.LoadSequentialTrackPort;
import co.edu.uniquindio.brujula.curriculum.application.service.GetCourseService;
import co.edu.uniquindio.brujula.curriculum.application.service.ListCourseRequirementsService;
import co.edu.uniquindio.brujula.curriculum.application.service.ListDegreeWorkModalitiesService;
import co.edu.uniquindio.brujula.curriculum.application.service.ListPlanCoursesService;
import co.edu.uniquindio.brujula.curriculum.application.service.ListSequentialTracksService;
import co.edu.uniquindio.brujula.curriculum.application.service.PlanMallaAssemblyService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring bean wiring for the curriculum module's application services.
 *
 * <p>Bridges domain/application (framework-free) into the Spring container. Infrastructure adapters
 * are auto-detected via {@code @Component}; application services are registered here to maintain
 * explicit wiring (ARQ-002).
 *
 * <p>Wired services (read-only curriculum query use cases — Phase 2/4/5 WU4):
 * <ul>
 *   <li>{@link GetCourseService} — CUR-001 get single course by ID
 *   <li>{@link ListPlanCoursesService} — CUR-001 list courses for a plan
 *   <li>{@link ListCourseRequirementsService} — CUR-003 list requirements for a course
 *   <li>{@link ListSequentialTracksService} — CUR-005 list sequential tracks for a plan
 *   <li>{@link ListDegreeWorkModalitiesService} — CUR-006 list degree work modalities for a plan
 *   <li>{@link PlanMallaAssemblyService} — ADR-F2-001 plan malla assembly
 * </ul>
 */
@Configuration
class CurriculumConfiguration {

  @Bean
  GetCourseService getCourseService(LoadCoursePort loadCoursePort) {
    return new GetCourseService(loadCoursePort);
  }

  @Bean
  ListPlanCoursesService listPlanCoursesService(LoadCoursePort loadCoursePort) {
    return new ListPlanCoursesService(loadCoursePort);
  }

  @Bean
  ListCourseRequirementsService listCourseRequirementsService(
      LoadCourseRequirementPort loadCourseRequirementPort) {
    return new ListCourseRequirementsService(loadCourseRequirementPort);
  }

  @Bean
  ListSequentialTracksService listSequentialTracksService(
      LoadSequentialTrackPort loadSequentialTrackPort) {
    return new ListSequentialTracksService(loadSequentialTrackPort);
  }

  @Bean
  ListDegreeWorkModalitiesService listDegreeWorkModalitiesService(
      LoadDegreeWorkModalityPort loadDegreeWorkModalityPort) {
    return new ListDegreeWorkModalitiesService(loadDegreeWorkModalityPort);
  }

  @Bean
  PlanMallaAssemblyService planMallaAssemblyService(
      LoadCoursePort loadCoursePort,
      LoadCourseRequirementPort loadCourseRequirementPort) {
    return new PlanMallaAssemblyService(loadCoursePort, loadCourseRequirementPort);
  }
}
