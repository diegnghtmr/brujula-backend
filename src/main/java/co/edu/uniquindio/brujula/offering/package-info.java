/**
 * Offering module — academic periods, course offerings, schedule options, and sessions.
 *
 * <p>Publishes:
 * <ul>
 *   <li>{@code offering::api} — read-only ports for students and simulation module
 *   <li>{@code offering::api-dto} — view records consumed by simulation and REST controllers
 * </ul>
 *
 * <p>Allowed outbound dependencies:
 * <ul>
 *   <li>{@code sharedkernel} — shared kernel interfaces
 *   <li>{@code audit::spi} — AuditRecorder SPI interface
 *   <li>{@code audit::spi-dto} — audit event DTOs
 *   <li>{@code curriculum::api} — curriculum data (course names, credits) via named interface
 *   <li>{@code curriculum::api-dto} — curriculum DTOs
 *   <li>{@code programcatalog::api} — program catalog data via named interface
 *   <li>{@code programcatalog::api-dto} — program catalog DTOs
 *   <li>{@code identity::api} — current user resolution via named interface
 *   <li>{@code identity::api-dto} — user profile DTOs
 *   <li>{@code generated} — OpenAPI-generated interfaces and model DTOs in REST adapters (ARQ-005)
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
package co.edu.uniquindio.brujula.offering;
