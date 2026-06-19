package co.edu.uniquindio.brujula.progress.infrastructure.configuration;

import co.edu.uniquindio.brujula.audit.application.port.in.AuditRecorder;
import co.edu.uniquindio.brujula.curriculum.application.port.in.AssemblePlanMallaQuery;
import co.edu.uniquindio.brujula.offering.application.port.in.ListPeriodsQuery;
import co.edu.uniquindio.brujula.programcatalog.application.port.in.GetProgramQuery;
import co.edu.uniquindio.brujula.programcatalog.application.port.in.GetStudyPlanQuery;
import co.edu.uniquindio.brujula.programcatalog.application.port.in.ListJourneysQuery;
import co.edu.uniquindio.brujula.progress.application.port.in.CreateCourseAttemptUseCase;
import co.edu.uniquindio.brujula.progress.application.port.in.CreateSubscriptionUseCase;
import co.edu.uniquindio.brujula.progress.application.port.in.DeleteCourseAttemptUseCase;
import co.edu.uniquindio.brujula.progress.application.port.in.DeleteSubscriptionUseCase;
import co.edu.uniquindio.brujula.progress.application.port.in.GetSubscriptionCurriculumQuery;
import co.edu.uniquindio.brujula.progress.application.port.in.GetSubscriptionProgressQuery;
import co.edu.uniquindio.brujula.progress.application.port.in.UpdateCourseAttemptUseCase;
import co.edu.uniquindio.brujula.progress.application.port.in.UpdateSubscriptionUseCase;
import co.edu.uniquindio.brujula.progress.application.port.in.api.GetStudentCourseHistoryQuery;
import co.edu.uniquindio.brujula.progress.application.port.out.LoadCourseAttemptPort;
import co.edu.uniquindio.brujula.progress.application.port.out.LoadSubscriptionPort;
import co.edu.uniquindio.brujula.progress.application.port.out.SaveCourseAttemptPort;
import co.edu.uniquindio.brujula.progress.application.port.out.SaveSubscriptionPort;
import co.edu.uniquindio.brujula.progress.application.port.out.ValidatePlanStatusPort;
import co.edu.uniquindio.brujula.progress.application.service.CreateCourseAttemptService;
import co.edu.uniquindio.brujula.progress.application.service.CreateSubscriptionService;
import co.edu.uniquindio.brujula.progress.application.service.DeleteCourseAttemptService;
import co.edu.uniquindio.brujula.progress.application.service.DeleteSubscriptionService;
import co.edu.uniquindio.brujula.progress.application.service.GetCourseAttemptService;
import co.edu.uniquindio.brujula.progress.application.service.GetStudentCourseHistoryService;
import co.edu.uniquindio.brujula.progress.application.service.GetSubscriptionCurriculumService;
import co.edu.uniquindio.brujula.progress.application.service.GetSubscriptionProgressService;
import co.edu.uniquindio.brujula.progress.application.service.GetSubscriptionService;
import co.edu.uniquindio.brujula.progress.application.service.UpdateCourseAttemptService;
import co.edu.uniquindio.brujula.progress.application.service.UpdateSubscriptionService;
import co.edu.uniquindio.brujula.sharedkernel.application.port.out.IdempotencyStorePort;
import co.edu.uniquindio.brujula.sharedkernel.application.port.out.JsonCodecPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring bean wiring for the progress module's application services.
 *
 * <p>Framework-free application services are registered here for explicit constructor injection.
 * Persistence adapters are auto-detected via {@code @Component}. All port.out adapters are injected
 * as interfaces — never concrete adapter classes.
 *
 * <p>Wires:
 *
 * <ul>
 *   <li>{@link CreateSubscriptionService}
 *   <li>{@link GetSubscriptionService} — implements both {@code GetSubscriptionQuery} and {@code
 *       ListMySubscriptionsQuery}
 *   <li>{@link UpdateSubscriptionService}
 *   <li>{@link DeleteSubscriptionService}
 *   <li>{@link CreateCourseAttemptService}
 *   <li>{@link GetCourseAttemptService} — implements both {@code GetCourseAttemptQuery} and {@code
 *       ListMyCourseAttemptsQuery}
 *   <li>{@link UpdateCourseAttemptService}
 *   <li>{@link DeleteCourseAttemptService}
 *   <li>{@link GetSubscriptionCurriculumService}
 *   <li>{@link GetSubscriptionProgressService}
 * </ul>
 *
 * <p>{@code GetCurrentUserQuery} is injected by Spring's auto-wiring into {@link
 * co.edu.uniquindio.brujula.progress.infrastructure.adapter.in.rest.SubscriptionController}
 * directly — no explicit bean needed here.
 */
@Configuration
class ProgressConfiguration {

  @Bean
  CreateSubscriptionUseCase createSubscriptionService(
      LoadSubscriptionPort loadSubscriptionPort,
      SaveSubscriptionPort saveSubscriptionPort,
      IdempotencyStorePort idempotencyStorePort,
      JsonCodecPort jsonCodecPort,
      AuditRecorder auditRecorder,
      ValidatePlanStatusPort validatePlanStatusPort) {
    return new CreateSubscriptionService(
        loadSubscriptionPort,
        saveSubscriptionPort,
        idempotencyStorePort,
        jsonCodecPort,
        auditRecorder,
        validatePlanStatusPort);
  }

  /**
   * {@link GetSubscriptionService} implements both {@link
   * co.edu.uniquindio.brujula.progress.application.port.in.GetSubscriptionQuery} and {@link
   * co.edu.uniquindio.brujula.progress.application.port.in.ListMySubscriptionsQuery}.
   *
   * <p>Registering the concrete class lets Spring satisfy both interface autowiring points from a
   * single bean instance.
   */
  @Bean
  GetSubscriptionService getSubscriptionService(
      LoadSubscriptionPort loadSubscriptionPort,
      GetProgramQuery getProgramQuery,
      GetStudyPlanQuery getStudyPlanQuery,
      ListJourneysQuery listJourneysQuery) {
    return new GetSubscriptionService(
        loadSubscriptionPort, getProgramQuery, getStudyPlanQuery, listJourneysQuery);
  }

  @Bean
  UpdateSubscriptionUseCase updateSubscriptionService(
      LoadSubscriptionPort loadSubscriptionPort,
      SaveSubscriptionPort saveSubscriptionPort,
      AuditRecorder auditRecorder) {
    return new UpdateSubscriptionService(loadSubscriptionPort, saveSubscriptionPort, auditRecorder);
  }

  @Bean
  DeleteSubscriptionUseCase deleteSubscriptionService(
      LoadSubscriptionPort loadSubscriptionPort,
      SaveSubscriptionPort saveSubscriptionPort,
      AuditRecorder auditRecorder) {
    return new DeleteSubscriptionService(loadSubscriptionPort, saveSubscriptionPort, auditRecorder);
  }

  // -------------------------------------------------------------------------
  // CourseAttempt beans
  // -------------------------------------------------------------------------

  @Bean
  CreateCourseAttemptUseCase createCourseAttemptService(
      LoadSubscriptionPort loadSubscriptionPort,
      SaveCourseAttemptPort saveCourseAttemptPort,
      LoadCourseAttemptPort loadCourseAttemptPort,
      IdempotencyStorePort idempotencyStorePort,
      JsonCodecPort jsonCodecPort,
      AuditRecorder auditRecorder) {
    return new CreateCourseAttemptService(
        loadSubscriptionPort,
        saveCourseAttemptPort,
        loadCourseAttemptPort,
        idempotencyStorePort,
        jsonCodecPort,
        auditRecorder);
  }

  /**
   * {@link GetCourseAttemptService} implements both {@link
   * co.edu.uniquindio.brujula.progress.application.port.in.GetCourseAttemptQuery} and {@link
   * co.edu.uniquindio.brujula.progress.application.port.in.ListMyCourseAttemptsQuery}.
   *
   * <p>Registering the concrete class lets Spring satisfy both interface autowiring points from a
   * single bean instance.
   */
  @Bean
  GetCourseAttemptService getCourseAttemptService(
      LoadCourseAttemptPort loadCourseAttemptPort,
      LoadSubscriptionPort loadSubscriptionPort,
      AssemblePlanMallaQuery assemblePlanMallaQuery,
      ListPeriodsQuery listPeriodsQuery) {
    return new GetCourseAttemptService(
        loadCourseAttemptPort, loadSubscriptionPort, assemblePlanMallaQuery, listPeriodsQuery);
  }

  @Bean
  UpdateCourseAttemptUseCase updateCourseAttemptService(
      LoadCourseAttemptPort loadCourseAttemptPort,
      LoadSubscriptionPort loadSubscriptionPort,
      SaveCourseAttemptPort saveCourseAttemptPort,
      AuditRecorder auditRecorder) {
    return new UpdateCourseAttemptService(
        loadCourseAttemptPort, loadSubscriptionPort, saveCourseAttemptPort, auditRecorder);
  }

  @Bean
  DeleteCourseAttemptUseCase deleteCourseAttemptService(
      LoadCourseAttemptPort loadCourseAttemptPort,
      LoadSubscriptionPort loadSubscriptionPort,
      SaveCourseAttemptPort saveCourseAttemptPort,
      AuditRecorder auditRecorder) {
    return new DeleteCourseAttemptService(
        loadCourseAttemptPort, loadSubscriptionPort, saveCourseAttemptPort, auditRecorder);
  }

  // -------------------------------------------------------------------------
  // Curriculum, progress summary, and cross-module read API beans
  // -------------------------------------------------------------------------

  @Bean
  GetStudentCourseHistoryQuery getStudentCourseHistoryService(
      LoadSubscriptionPort loadSubscriptionPort, LoadCourseAttemptPort loadCourseAttemptPort) {
    return new GetStudentCourseHistoryService(loadSubscriptionPort, loadCourseAttemptPort);
  }

  @Bean
  GetSubscriptionCurriculumQuery getSubscriptionCurriculumService(
      LoadSubscriptionPort loadSubscriptionPort,
      LoadCourseAttemptPort loadCourseAttemptPort,
      AssemblePlanMallaQuery assemblePlanMallaQuery,
      ListJourneysQuery listJourneysQuery) {
    return new GetSubscriptionCurriculumService(
        loadSubscriptionPort, loadCourseAttemptPort, assemblePlanMallaQuery, listJourneysQuery);
  }

  @Bean
  GetSubscriptionProgressQuery getSubscriptionProgressService(
      LoadSubscriptionPort loadSubscriptionPort,
      LoadCourseAttemptPort loadCourseAttemptPort,
      AssemblePlanMallaQuery assemblePlanMallaQuery,
      GetProgramQuery getProgramQuery,
      ListPeriodsQuery listPeriodsQuery) {
    return new GetSubscriptionProgressService(
        loadSubscriptionPort,
        loadCourseAttemptPort,
        assemblePlanMallaQuery,
        getProgramQuery,
        listPeriodsQuery);
  }
}
