/**
 * Published SPI for the administration module — read-only query interfaces for cross-module use.
 *
 * <p>This package is published as {@code @NamedInterface("spi")} so that consuming modules (e.g.
 * {@code imports}) can depend on {@code administration::spi} without accessing internal
 * administration packages directly. Spring Modulith enforces this boundary.
 *
 * <p>Exposed interfaces:
 *
 * <ul>
 *   <li>{@link
 *       co.edu.uniquindio.brujula.administration.application.port.in.spi.IsStaffAssignedToProgramQuery}
 *       — check whether a staff member has an active assignment to a program
 *   <li>{@link
 *       co.edu.uniquindio.brujula.administration.application.port.in.spi.ResolveStaffByEmailQuery}
 *       — resolve a staff UUID from an institutional email address (actor→StaffAccount seam)
 * </ul>
 *
 * <p>These interfaces use only UUID/primitive/Optional signatures — no domain types cross published
 * boundaries.
 *
 * <p>Mirrors {@code audit::spi} pattern. Consumer modules must declare {@code
 * "administration::spi"} in their {@code @ApplicationModule(allowedDependencies)}.
 */
@org.springframework.modulith.NamedInterface("spi")
package co.edu.uniquindio.brujula.administration.application.port.in.spi;
