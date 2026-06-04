/**
 * Imports module — bulk data import via XLSX/JSON with pre-validation (staff only).
 *
 * <p>Allowed outbound dependencies: {@code sharedkernel}, {@code audit}, {@code curriculum},
 * {@code programcatalog}, {@code offering}.
 */
@org.springframework.modulith.ApplicationModule(
    allowedDependencies = {
      "sharedkernel", "audit", "curriculum", "programcatalog", "offering"
    })
package co.edu.uniquindio.brujula.imports;
