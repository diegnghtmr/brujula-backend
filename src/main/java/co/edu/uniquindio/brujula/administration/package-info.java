/**
 * Administration module — staff management, credit limits, configurable domains, and periods.
 *
 * <p>Allowed outbound dependencies: {@code sharedkernel}, {@code audit}, {@code identity}.
 */
@org.springframework.modulith.ApplicationModule(
    allowedDependencies = {"sharedkernel", "audit", "identity"})
package co.edu.uniquindio.brujula.administration;
