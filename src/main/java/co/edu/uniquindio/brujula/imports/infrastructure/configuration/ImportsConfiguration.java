package co.edu.uniquindio.brujula.imports.infrastructure.configuration;

import co.edu.uniquindio.brujula.audit.application.port.in.AuditRecorder;
import co.edu.uniquindio.brujula.curriculum.application.port.in.CreateCourseRequirementUseCase;
import co.edu.uniquindio.brujula.curriculum.application.port.in.CreateCourseUseCase;
import co.edu.uniquindio.brujula.imports.application.port.in.ConfirmImportUseCase;
import co.edu.uniquindio.brujula.imports.application.port.in.GetImportUseCase;
import co.edu.uniquindio.brujula.imports.application.port.in.ListImportsUseCase;
import co.edu.uniquindio.brujula.imports.application.port.in.UploadImportUseCase;
import co.edu.uniquindio.brujula.imports.application.port.out.FileParserPort;
import co.edu.uniquindio.brujula.imports.application.port.out.LoadImportBatchPort;
import co.edu.uniquindio.brujula.imports.application.port.out.SaveImportBatchPort;
import co.edu.uniquindio.brujula.imports.application.port.out.StaffAssignmentPort;
import co.edu.uniquindio.brujula.imports.application.service.CoursesValidator;
import co.edu.uniquindio.brujula.imports.application.service.ConfirmImportService;
import co.edu.uniquindio.brujula.imports.application.service.GetImportService;
import co.edu.uniquindio.brujula.imports.application.service.ImportValidator;
import co.edu.uniquindio.brujula.imports.application.service.ListImportsService;
import co.edu.uniquindio.brujula.imports.application.service.OfferingsValidator;
import co.edu.uniquindio.brujula.imports.application.service.RequirementsValidator;
import co.edu.uniquindio.brujula.imports.application.service.UploadImportService;
import co.edu.uniquindio.brujula.sharedkernel.application.port.out.IdempotencyStorePort;
import co.edu.uniquindio.brujula.sharedkernel.application.port.out.JsonCodecPort;
import java.util.List;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring bean wiring for the imports module (WU3).
 *
 * <p>Bridges framework-free application services into the Spring container. Parser adapters
 * (XlsxParserAdapter, JsonParserAdapter) are auto-detected via {@code @Component}; services
 * are registered here for explicit wiring.
 *
 * <p>ARQ-006: all port.out adapters are injected as interfaces — never concrete adapter classes.
 */
@Configuration
@EnableConfigurationProperties(ImportValidationProperties.class)
class ImportsConfiguration {

  @Bean
  CoursesValidator coursesValidator() {
    return new CoursesValidator();
  }

  @Bean
  RequirementsValidator requirementsValidator() {
    return new RequirementsValidator();
  }

  @Bean
  OfferingsValidator offeringsValidator() {
    return new OfferingsValidator();
  }

  @Bean
  ImportValidator importValidator(
      CoursesValidator coursesValidator,
      RequirementsValidator requirementsValidator,
      OfferingsValidator offeringsValidator) {
    return new ImportValidator(coursesValidator, requirementsValidator, offeringsValidator);
  }

  @Bean
  UploadImportUseCase uploadImportUseCase(
      List<FileParserPort> parsers,
      ImportValidator importValidator,
      SaveImportBatchPort saveImportBatchPort,
      StaffAssignmentPort staffAssignmentPort,
      IdempotencyStorePort idempotencyStorePort,
      JsonCodecPort jsonCodecPort,
      AuditRecorder auditRecorder,
      ImportValidationProperties importValidationProperties) {
    return new UploadImportService(
        parsers,
        importValidator,
        saveImportBatchPort,
        staffAssignmentPort,
        idempotencyStorePort,
        jsonCodecPort,
        auditRecorder,
        importValidationProperties.getMaxFileSizeBytes());
  }

  @Bean
  ListImportsUseCase listImportsUseCase(
      LoadImportBatchPort loadImportBatchPort,
      StaffAssignmentPort staffAssignmentPort) {
    return new ListImportsService(loadImportBatchPort, staffAssignmentPort);
  }

  @Bean
  GetImportUseCase getImportUseCase(
      LoadImportBatchPort loadImportBatchPort,
      StaffAssignmentPort staffAssignmentPort) {
    return new GetImportService(loadImportBatchPort, staffAssignmentPort);
  }

  @Bean
  ConfirmImportUseCase confirmImportUseCase(
      LoadImportBatchPort loadImportBatchPort,
      SaveImportBatchPort saveImportBatchPort,
      StaffAssignmentPort staffAssignmentPort,
      IdempotencyStorePort idempotencyStorePort,
      JsonCodecPort jsonCodecPort,
      AuditRecorder auditRecorder,
      CreateCourseUseCase createCourseUseCase,
      CreateCourseRequirementUseCase createCourseRequirementUseCase) {
    return new ConfirmImportService(
        loadImportBatchPort,
        saveImportBatchPort,
        staffAssignmentPort,
        idempotencyStorePort,
        jsonCodecPort,
        auditRecorder,
        createCourseUseCase,
        createCourseRequirementUseCase);
  }
}
