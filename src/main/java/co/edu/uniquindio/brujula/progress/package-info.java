/**
 * Progress module — student curricular progress, course attempts, and estimated averages.
 *
 * <p>Allowed outbound dependencies:
 *
 * <ul>
 *   <li>{@code sharedkernel} — idempotency and JSON codec ports.
 *   <li>{@code audit::spi} — {@code AuditRecorder} SPI interface.
 *   <li>{@code audit::spi-dto} — {@code RecordAuditEventCommand} DTO.
 *   <li>{@code curriculum} — course/plan data for progress analysis.
 *   <li>{@code programcatalog::api} — {@code GetStudyPlanQuery} and other inbound ports.
 *   <li>{@code programcatalog::api-dto} — {@code StudyPlanView} and other DTOs.
 *   <li>{@code identity::api} — {@code GetCurrentUserQuery} to resolve caller identity.
 *   <li>{@code identity::api-dto} — {@code UserProfile} DTO.
 *   <li>{@code generated} — OpenAPI-generated interfaces and model DTOs in REST adapters.
 * </ul>
 */
@org.springframework.modulith.ApplicationModule(
    allowedDependencies = {
      "sharedkernel",
      "audit::spi",
      "audit::spi-dto",
      "curriculum::api",
      "curriculum::api-dto",
      "programcatalog::api",
      "programcatalog::api-dto",
      "identity::api",
      "identity::api-dto",
      "generated"
    })
package co.edu.uniquindio.brujula.progress;
