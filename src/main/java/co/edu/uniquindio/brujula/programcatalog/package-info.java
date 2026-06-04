/**
 * Program catalog module — academic programs, curricula, and schedules.
 *
 * <p>Maps to PostgreSQL schema {@code catalog} (programcatalog → catalog). Allowed outbound
 * dependencies: {@code sharedkernel}, {@code audit}.
 */
@org.springframework.modulith.ApplicationModule(
    allowedDependencies = {"sharedkernel", "audit"})
package co.edu.uniquindio.brujula.programcatalog;
