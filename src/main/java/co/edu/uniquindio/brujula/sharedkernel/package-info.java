/**
 * Shared kernel — cross-cutting value types, base exceptions, and domain primitives.
 *
 * <p>Open module: exposes all its contents to all other modules. Has no dependency on any other
 * module. Maps to PostgreSQL schema {@code platform}.
 */
@org.springframework.modulith.ApplicationModule(
    type = org.springframework.modulith.ApplicationModule.Type.OPEN,
    allowedDependencies = {})
package co.edu.uniquindio.brujula.sharedkernel;
