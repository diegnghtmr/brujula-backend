package co.edu.uniquindio.brujula.imports.domain.model;

import java.util.Objects;
import java.util.UUID;

/**
 * Value object: strongly-typed identity for an ImportBatch aggregate.
 *
 * <p>Pure domain type — zero framework annotations (ARQ-002).
 */
public record ImportBatchId(UUID value) {

  public ImportBatchId {
    Objects.requireNonNull(value, "ImportBatchId value must not be null");
  }

  public static ImportBatchId generate() {
    return new ImportBatchId(UUID.randomUUID());
  }

  public static ImportBatchId of(UUID value) {
    return new ImportBatchId(value);
  }
}
