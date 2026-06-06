/**
 * Administration module — staff management, credit limits, configurable domains, and periods.
 *
 * <p>Allowed outbound dependencies: {@code sharedkernel}, {@code audit} (for AuditRecorder SPI),
 * {@code identity} (for AllowedEmailDomainPolicy and GetCurrentUserQuery), {@code generated} (for
 * OpenAPI-generated interfaces and model DTOs in REST adapters, ARQ-005).
 */
@org.springframework.modulith.ApplicationModule(
    allowedDependencies = {
      "sharedkernel",
      "audit::spi",
      "audit::spi-dto",
      "identity::api",
      "identity::api-dto",
      "identity::domain",
      "generated"
    })
package co.edu.uniquindio.brujula.administration;
