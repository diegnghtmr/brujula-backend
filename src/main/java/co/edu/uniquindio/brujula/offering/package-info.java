/**
 * Offering module — academic periods, course offerings, schedule options, and sessions.
 *
 * <p>Allowed outbound dependencies: {@code sharedkernel}, {@code audit}, {@code curriculum},
 * {@code programcatalog}.
 */
@org.springframework.modulith.ApplicationModule(
    allowedDependencies = {"sharedkernel", "audit", "curriculum", "programcatalog"})
package co.edu.uniquindio.brujula.offering;
