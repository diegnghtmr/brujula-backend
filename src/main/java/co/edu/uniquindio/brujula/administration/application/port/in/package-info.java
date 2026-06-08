/**
 * Published API for the administration module — inbound use-case and query interfaces.
 *
 * <p>This package is published as {@code @NamedInterface("api")} so that other modules (e.g.
 * simulation) can depend on {@code administration::api} without accessing internal administration
 * packages directly. Spring Modulith enforces this boundary constraint.
 *
 * <p>Exposed interfaces: {@link co.edu.uniquindio.brujula.administration.application.port.in.GetCreditLimitQuery},
 * {@link co.edu.uniquindio.brujula.administration.application.port.in.CreateStaffUseCase},
 * {@link co.edu.uniquindio.brujula.administration.application.port.in.ListStaffQuery},
 * {@link co.edu.uniquindio.brujula.administration.application.port.in.ManageAllowedEmailDomainsUseCase},
 * {@link co.edu.uniquindio.brujula.administration.application.port.in.UpdateStaffUseCase}.
 *
 * <p>Note: publishing the full port.in package is acceptable. Staff management use-cases are
 * read-only or protected by their own authorization layers.
 */
@org.springframework.modulith.NamedInterface("api")
package co.edu.uniquindio.brujula.administration.application.port.in;
