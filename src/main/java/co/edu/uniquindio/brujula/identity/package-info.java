/**
 * Identity module — student account lifecycle, email verification, and authentication seam.
 *
 * <p>Allowed outbound dependencies: {@code sharedkernel}, {@code audit}.
 */
@org.springframework.modulith.ApplicationModule(
    allowedDependencies = {"sharedkernel", "audit"})
package co.edu.uniquindio.brujula.identity;
