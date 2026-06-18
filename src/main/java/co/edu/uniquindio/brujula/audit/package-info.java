/**
 * Audit module — functional audit events visible to admin and staff.
 *
 * <p>The audit module is a sink that may be called by all other modules. It exposes the {@code
 * AuditRecorder} SPI (port.in) for cross-module event recording via the named interface {@code
 * spi}. The REST adapter depends on the {@code generated} module for OpenAPI-generated interfaces
 * and model DTOs.
 */
@org.springframework.modulith.ApplicationModule(allowedDependencies = {"sharedkernel", "generated"})
package co.edu.uniquindio.brujula.audit;
