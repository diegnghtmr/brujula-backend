/**
 * Audit module — functional audit events visible to admin and staff.
 *
 * <p>The audit module is a sink that may be called by all other modules. It depends only on {@code
 * sharedkernel}.
 */
@org.springframework.modulith.ApplicationModule(allowedDependencies = {"sharedkernel"})
package co.edu.uniquindio.brujula.audit;
