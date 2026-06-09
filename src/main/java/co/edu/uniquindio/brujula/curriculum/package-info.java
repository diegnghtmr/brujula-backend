/**
 * Curriculum module — courses, prerequisites, electives, and degree requirements.
 *
 * <p>Allowed outbound dependencies:
 * <ul>
 *   <li>{@code sharedkernel} — shared kernel interfaces
 *   <li>{@code audit::spi} — AuditRecorder SPI interface (write services)
 *   <li>{@code audit::spi-dto} — audit event DTOs (write services)
 *   <li>{@code programcatalog} — program catalog data
 *   <li>{@code identity::api} — current user resolution for write controller endpoints
 *   <li>{@code identity::api-dto} — user profile DTOs
 *   <li>{@code generated} — OpenAPI-generated interfaces and model DTOs in REST adapters (ARQ-005)
 * </ul>
 */
@org.springframework.modulith.ApplicationModule(
    allowedDependencies = {"sharedkernel", "audit::spi", "audit::spi-dto", "programcatalog", "programcatalog::api", "programcatalog::api-dto", "identity::api", "identity::api-dto", "generated"})
package co.edu.uniquindio.brujula.curriculum;
