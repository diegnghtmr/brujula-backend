package co.edu.uniquindio.brujula.imports.domain.exception;

import java.util.UUID;

/**
 * Thrown when an ImportBatch with the requested id does not exist.
 *
 * <p>Maps to HTTP 404 Not Found at the controller layer.
 */
public class ImportBatchNotFoundException extends RuntimeException {

  private final UUID importId;

  public ImportBatchNotFoundException(UUID importId) {
    super("ImportBatch not found: %s".formatted(importId));
    this.importId = importId;
  }

  public UUID getImportId() {
    return importId;
  }
}
