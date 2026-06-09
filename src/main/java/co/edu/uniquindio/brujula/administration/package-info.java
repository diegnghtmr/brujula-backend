/**
 * Administration module — staff management, credit limits, configurable domains, and periods.
 *
 * <p>Allowed outbound dependencies: {@code sharedkernel}, {@code audit} (for AuditRecorder SPI),
 * {@code identity} (for AllowedEmailDomainPolicy and GetCurrentUserQuery), {@code generated} (for
 * OpenAPI-generated interfaces and model DTOs in REST adapters, ARQ-005), {@code offering::api} and
 * {@code offering::api-dto} (for period write use cases — ADR-0: period writes live in offering
 * module, administration controller invokes them via offering::api, mirrors the existing
 * simulation→offering pattern).
 */
@org.springframework.modulith.ApplicationModule(
    allowedDependencies = {
      "sharedkernel",
      "audit::spi",
      "audit::spi-dto",
      "identity::api",
      "identity::api-dto",
      "identity::domain",
      "generated",
      "offering::api",
      "offering::api-dto"
    })
package co.edu.uniquindio.brujula.administration;
