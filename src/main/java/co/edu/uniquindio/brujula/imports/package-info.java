/**
 * Imports module — bulk data import via XLSX/JSON with pre-validation (staff only).
 *
 * <p>Publishes no named interfaces (leaf consumer module).
 *
 * <p>Allowed outbound dependencies:
 *
 * <ul>
 *   <li>{@code sharedkernel} — JsonCodecPort, IdempotencyStorePort
 *   <li>{@code audit::spi} — AuditRecorder SPI
 *   <li>{@code audit::spi-dto} — audit event DTOs
 *   <li>{@code curriculum::api} — curriculum write use-cases for confirm
 *   <li>{@code curriculum::api-dto} — CreateCourseCommand, CreateCourseRequirementCommand,
 *       CourseView DTOs consumed by ConfirmImportService during row replay
 *   <li>{@code offering::api} — offering write use-case for confirm OFFERINGS
 *   <li>{@code offering::api-dto} — CreateOfferingCommand DTO consumed by ConfirmImportService
 *       during OFFERINGS row replay (mirrors curriculum::api-dto pattern)
 *   <li>{@code programcatalog::api} — program/plan resolution for validation
 *   <li>{@code administration::spi} — staff assignment check + actor→StaffAccount resolution
 *       ({@code StaffAssignmentAdapter}). Backed by a real {@code @NamedInterface("spi")} in {@code
 *       administration.application.port.in.spi}, so Spring Modulith genuinely validates this
 *       boundary.
 *   <li>{@code identity::api} — GetCurrentUserQuery for actor resolution in REST controller
 *   <li>{@code identity::api-dto} — UserProfile DTO consumed by REST controller
 *   <li>{@code generated} — OpenAPI-generated ImportsApi interface and model DTOs in REST adapters.
 *       Mirrors the offering module pattern.
 * </ul>
 *
 * <p>Note: bare module names (e.g. {@code "audit"}) are migrated to NamedInterface paths (e.g.
 * {@code "audit::spi"}) per Spring Modulith convention.
 */
@org.springframework.modulith.ApplicationModule(
    allowedDependencies = {
      "sharedkernel",
      "audit::spi",
      "audit::spi-dto",
      "curriculum::api",
      "curriculum::api-dto",
      "offering::api",
      "offering::api-dto",
      "programcatalog::api",
      "administration::spi",
      "identity::api",
      "identity::api-dto",
      "generated"
    })
package co.edu.uniquindio.brujula.imports;
