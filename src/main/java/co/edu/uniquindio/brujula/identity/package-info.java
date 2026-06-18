/**
 * Identity module — student account lifecycle, email verification, and authentication seam.
 *
 * <p>Allowed outbound dependencies: {@code sharedkernel}, {@code audit}, {@code generated}
 * (OpenAPI-generated API interfaces and model DTOs; confined to infrastructure adapters).
 *
 * <p>Sub-packages {@code application.port.in}, {@code application.dto}, and {@code domain} are
 * exposed via {@code @NamedInterface} so that other modules (e.g. administration) may inject {@code
 * GetCurrentUserQuery} and consume {@code UserProfile} and domain policy types.
 */
@org.springframework.modulith.ApplicationModule(
    allowedDependencies = {"sharedkernel", "audit::spi", "audit::spi-dto", "generated"})
package co.edu.uniquindio.brujula.identity;
