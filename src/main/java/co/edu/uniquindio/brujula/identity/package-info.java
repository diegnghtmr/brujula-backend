/**
 * Identity module — student account lifecycle, email verification, and authentication seam.
 *
 * <p>Allowed outbound dependencies: {@code sharedkernel}, {@code audit}, {@code generated}
 * (OpenAPI-generated API interfaces and model DTOs; confined to infrastructure adapters per
 * ARQ-005).
 */
@org.springframework.modulith.ApplicationModule(
    allowedDependencies = {"sharedkernel", "audit", "generated"})
package co.edu.uniquindio.brujula.identity;
