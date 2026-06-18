/**
 * Simulation module — academic planning simulations and validation engine.
 *
 * <p>Leaf consumer: no published named interface. All cross-module access goes through the named
 * interfaces of the target modules.
 *
 * <p>Allowed outbound dependencies:
 *
 * <ul>
 *   <li>{@code sharedkernel} — shared kernel (idempotency, codec, exceptions)
 *   <li>{@code audit} — audit recorder SPI
 *   <li>{@code offering::api} — resolve period/schedule-option
 *   <li>{@code offering::api-dto} — offering view DTOs
 *   <li>{@code administration::api} — credit limit query
 *   <li>{@code progress::api} — student course history (no api-dto in progress)
 *   <li>{@code curriculum::api} — prerequisite edges, course credits, sequential tracks
 *   <li>{@code curriculum::api-dto} — curriculum view DTOs
 *   <li>{@code programcatalog::api} — program name resolution
 *   <li>{@code programcatalog::api-dto} — program view DTOs
 *   <li>{@code identity::api} — current user resolution
 *   <li>{@code identity::api-dto} — user profile DTOs
 *   <li>{@code generated} — OpenAPI-generated interfaces and model DTOs
 * </ul>
 */
@org.springframework.modulith.ApplicationModule(
    allowedDependencies = {
      "sharedkernel",
      "audit",
      "offering::api",
      "offering::api-dto",
      "administration::api",
      "progress::api",
      "curriculum::api",
      "curriculum::api-dto",
      "programcatalog::api",
      "programcatalog::api-dto",
      "identity::api",
      "identity::api-dto",
      "generated"
    })
package co.edu.uniquindio.brujula.simulation;
