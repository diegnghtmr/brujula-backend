/**
 * Curriculum module — courses, prerequisites, electives, and degree requirements.
 *
 * <p>Allowed outbound dependencies: {@code sharedkernel}, {@code audit}, {@code programcatalog},
 * {@code generated} (contract-first REST adapters use the generated API interfaces and models).
 */
@org.springframework.modulith.ApplicationModule(
    allowedDependencies = {"sharedkernel", "audit", "programcatalog", "generated"})
package co.edu.uniquindio.brujula.curriculum;
