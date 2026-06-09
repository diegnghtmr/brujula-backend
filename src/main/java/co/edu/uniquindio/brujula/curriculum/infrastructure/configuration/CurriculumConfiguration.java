package co.edu.uniquindio.brujula.curriculum.infrastructure.configuration;

import co.edu.uniquindio.brujula.audit.application.port.in.AuditRecorder;
import co.edu.uniquindio.brujula.curriculum.application.port.out.LoadCoursePort;
import co.edu.uniquindio.brujula.curriculum.application.port.out.LoadCourseRequirementPort;
import co.edu.uniquindio.brujula.curriculum.application.port.out.LoadDegreeWorkModalityPort;
import co.edu.uniquindio.brujula.curriculum.application.port.out.LoadSequentialTrackPort;
import co.edu.uniquindio.brujula.curriculum.application.port.out.SaveCoursePort;
import co.edu.uniquindio.brujula.curriculum.application.port.out.SaveCourseRequirementPort;
import co.edu.uniquindio.brujula.curriculum.application.service.ArchiveCourseService;
import co.edu.uniquindio.brujula.curriculum.application.service.CreateCourseRequirementService;
import co.edu.uniquindio.brujula.curriculum.application.service.CreateCourseService;
import co.edu.uniquindio.brujula.curriculum.application.service.GetCourseService;
import co.edu.uniquindio.brujula.curriculum.application.service.ListCourseRequirementsService;
import co.edu.uniquindio.brujula.curriculum.application.service.ListDegreeWorkModalitiesService;
import co.edu.uniquindio.brujula.curriculum.application.service.ListPlanCoursesService;
import co.edu.uniquindio.brujula.curriculum.application.service.ListSequentialTracksService;
import co.edu.uniquindio.brujula.curriculum.application.service.PlanMallaAssemblyService;
import co.edu.uniquindio.brujula.curriculum.application.service.UpdateCourseRequirementService;
import co.edu.uniquindio.brujula.curriculum.application.service.UpdateCourseService;
import co.edu.uniquindio.brujula.sharedkernel.application.port.out.IdempotencyStorePort;
import co.edu.uniquindio.brujula.sharedkernel.application.port.out.JsonCodecPort;
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
 *
 * <ul>
 *   <li>{@link GetCourseService} — CUR-001 get single course by ID
 *   <li>{@link ListPlanCoursesService} — CUR-001 list courses for a plan
 *   <li>{@link ListCourseRequirementsService} — CUR-003 list requirements for a course
 *   <li>{@link ListSequentialTracksService} — CUR-005 list sequential tracks for a plan
 *   <li>{@link ListDegreeWorkModalitiesService} — CUR-006 list degree work modalities for a plan
 *   <li>{@link PlanMallaAssemblyService} — ADR-F2-001 plan malla assembly
 * </ul>
 *
 * <p>Wired services (write curriculum command use cases — Phase 6 WU5):
 *
 * <ul>
 *   <li>{@link CreateCourseService} — CUR-001 create course (idempotent)
 *   <li>{@link UpdateCourseService} — CUR-002 update course (optimistic lock)
 *   <li>{@link ArchiveCourseService} — CUR-002 archive course (optimistic lock)
 *   <li>{@link CreateCourseRequirementService} — CUR-003 create requirement (idempotent, cycle
 *       check)
 *   <li>{@link UpdateCourseRequirementService} — CUR-003 update requirement (optimistic lock)
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
      LoadCoursePort loadCoursePort, LoadCourseRequirementPort loadCourseRequirementPort) {
    return new PlanMallaAssemblyService(loadCoursePort, loadCourseRequirementPort);
  }

  // ---- Write use cases (WU5) ----

  @Bean
  CreateCourseService createCourseService(
      SaveCoursePort saveCoursePort,
      IdempotencyStorePort idempotencyStorePort,
      JsonCodecPort jsonCodecPort,
      AuditRecorder auditRecorder) {
    return new CreateCourseService(
        saveCoursePort, idempotencyStorePort, jsonCodecPort, auditRecorder);
  }

  @Bean
  UpdateCourseService updateCourseService(
      SaveCoursePort saveCoursePort, AuditRecorder auditRecorder) {
    return new UpdateCourseService(saveCoursePort, auditRecorder);
  }

  @Bean
  ArchiveCourseService archiveCourseService(
      SaveCoursePort saveCoursePort, AuditRecorder auditRecorder) {
    return new ArchiveCourseService(saveCoursePort, auditRecorder);
  }

  @Bean
  CreateCourseRequirementService createCourseRequirementService(
      SaveCourseRequirementPort saveRequirementPort,
      LoadCoursePort loadCoursePort,
      IdempotencyStorePort idempotencyStorePort,
      JsonCodecPort jsonCodecPort,
      AuditRecorder auditRecorder) {
    return new CreateCourseRequirementService(
        saveRequirementPort, loadCoursePort, idempotencyStorePort, jsonCodecPort, auditRecorder);
  }

  @Bean
  UpdateCourseRequirementService updateCourseRequirementService(
      SaveCourseRequirementPort saveRequirementPort,
      LoadCoursePort loadCoursePort,
      AuditRecorder auditRecorder) {
    return new UpdateCourseRequirementService(saveRequirementPort, loadCoursePort, auditRecorder);
  }
}
