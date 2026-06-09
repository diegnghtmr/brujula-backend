/**
 * Imports module — bulk data import via XLSX/JSON with pre-validation (staff only).
 *
 * <p>Publishes no named interfaces (leaf consumer module).
 *
 * <p>Allowed outbound dependencies:
 * <ul>
 *   <li>{@code sharedkernel} — JsonCodecPort, IdempotencyStorePort
 *   <li>{@code audit::spi} — AuditRecorder SPI (WU3+)
 *   <li>{@code audit::spi-dto} — audit event DTOs (WU3+)
 *   <li>{@code curriculum::api} — curriculum write use-cases for confirm (WU5+)
 *   <li>{@code curriculum::api-dto} — CreateCourseCommand, CreateCourseRequirementCommand, CourseView
 *       DTOs consumed by ConfirmImportService during row replay (WU6)
 *   <li>{@code offering::api} — offering write use-case for confirm OFFERINGS (WU7)
 *   <li>{@code programcatalog::api} — program/plan resolution for validation (WU3)
 *   <li>{@code administration::spi} — staff assignment check + actor→StaffAccount resolution
 *       (WU2, {@code StaffAssignmentAdapter}). This edge is NOW backed by a real
 *       {@code @NamedInterface("spi")} in {@code administration.application.port.in.spi},
 *       so Spring Modulith genuinely validates this boundary.
 *   <li>{@code identity::api} — GetCurrentUserQuery for actor resolution in REST controller (WU4)
 *   <li>{@code identity::api-dto} — UserProfile DTO consumed by REST controller (WU4)
 *   <li>{@code generated} — OpenAPI-generated ImportsApi interface and model DTOs in REST
 *       adapters (ARQ-005, WU4). Mirrors the offering module pattern.
 * </ul>
 *
 * <p>Note: bare module names (e.g. {@code "audit"}) are migrated to NamedInterface paths
 * (e.g. {@code "audit::spi"}) per Spring Modulith convention and the design ADR for this change.
 */
@org.springframework.modulith.ApplicationModule(
    allowedDependencies = {
        "sharedkernel",
        "audit::spi",
        "audit::spi-dto",
        "curriculum::api",
        "curriculum::api-dto",
        "offering::api",
        "programcatalog::api",
        "administration::spi",
        "identity::api",
        "identity::api-dto",
        "generated"
    })
package co.edu.uniquindio.brujula.imports;
