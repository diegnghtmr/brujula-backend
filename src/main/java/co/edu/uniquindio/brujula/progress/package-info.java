/**
 * Progress module — student curricular progress, course attempts, and estimated averages.
 *
 * <p>Allowed outbound dependencies: {@code sharedkernel}, {@code audit}, {@code curriculum}, {@code
 * programcatalog}, {@code identity}.
 */
@org.springframework.modulith.ApplicationModule(
    allowedDependencies = {"sharedkernel", "audit", "curriculum", "programcatalog", "identity"})
package co.edu.uniquindio.brujula.progress;
