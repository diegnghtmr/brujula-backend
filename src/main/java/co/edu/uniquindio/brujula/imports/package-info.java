/**
 * Imports module — bulk data import via XLSX/JSON with pre-validation (staff only).
 *
 * <p>Publishes no named interfaces in WU1 (the module is a leaf consumer for now).
 *
 * <p>Allowed outbound dependencies (WU1 scope — only what WU1 actually imports):
 * <ul>
 *   <li>{@code sharedkernel} — JsonCodecPort, IdempotencyStorePort (added in later WUs)
 *   <li>{@code audit::spi} — AuditRecorder SPI (added in WU3+ but pre-declared per design)
 *   <li>{@code audit::spi-dto} — audit event DTOs (WU3+)
 *   <li>{@code curriculum::api} — curriculum write use-cases for confirm (WU5+)
 *   <li>{@code offering::api} — offering write use-case for confirm OFFERINGS (WU7)
 *   <li>{@code programcatalog::api} — program/plan resolution for validation (WU3)
 * </ul>
 *
 * <p>{@code administration::spi} is intentionally NOT declared here: that named interface
 * does not exist yet (WU2 creates it). Spring Modulith only validates an allowed dependency
 * when real code references the target, so declaring it now would be an unenforced
 * forward-commitment. WU2 adds the {@code @NamedInterface("spi")} and this edge together.
 *
 * <p>Note: bare module names (e.g. {@code "audit"}) are migrated to NamedInterface paths
 * (e.g. {@code "audit::spi"}) per Spring Modulith convention and the design ADR for this change.
 * WU1 itself only touches {@code sharedkernel} (persistence-level, no cross-module application
 * call); the remaining edges are declared now so later WUs don't need to revisit this file.
 */
@org.springframework.modulith.ApplicationModule(
    allowedDependencies = {
        "sharedkernel",
        "audit::spi",
        "audit::spi-dto",
        "curriculum::api",
        "offering::api",
        "programcatalog::api"
    })
package co.edu.uniquindio.brujula.imports;
