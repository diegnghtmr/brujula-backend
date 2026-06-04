/**
 * Simulation module — academic planning simulations and validation engine.
 *
 * <p>Allowed outbound dependencies: {@code sharedkernel}, {@code audit}, {@code progress}, {@code
 * offering}, {@code curriculum}, {@code programcatalog}.
 */
@org.springframework.modulith.ApplicationModule(
    allowedDependencies = {
      "sharedkernel",
      "audit",
      "progress",
      "offering",
      "curriculum",
      "programcatalog"
    })
package co.edu.uniquindio.brujula.simulation;
