/**
 * Curriculum module — courses, prerequisites, electives, and degree requirements.
 *
 * <p>Allowed outbound dependencies: {@code sharedkernel}, {@code audit}, {@code programcatalog}.
 */
@org.springframework.modulith.ApplicationModule(
    allowedDependencies = {"sharedkernel", "audit", "programcatalog"})
package co.edu.uniquindio.brujula.curriculum;
