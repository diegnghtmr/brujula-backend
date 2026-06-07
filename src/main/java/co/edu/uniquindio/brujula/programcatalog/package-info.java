/**
 * Program catalog module — academic programs, curricula, and schedules.
 *
 * <p>Maps to PostgreSQL schema {@code catalog} (programcatalog → catalog). Allowed outbound
 * dependencies: {@code sharedkernel}, {@code audit}, {@code generated} (contract-first REST
 * adapters use the generated API interfaces and models).
 */
@org.springframework.modulith.ApplicationModule(
    allowedDependencies = {"sharedkernel", "audit", "generated"})
package co.edu.uniquindio.brujula.programcatalog;
