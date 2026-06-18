package co.edu.uniquindio.brujula.imports.domain.exception;

import co.edu.uniquindio.brujula.imports.domain.model.ImportBatchId;
import co.edu.uniquindio.brujula.imports.domain.model.ImportStatus;

/**
 * Thrown when an ImportBatch confirmation is attempted on a batch that is not in a confirmable
 * state.
 *
 * <p>Covers:
 *
 * <ul>
 *   <li>Cannot re-confirm an already CONFIRMED batch
 *   <li>Cannot confirm an INVALID batch
 * </ul>
 *
 * <p>Pure domain exception — zero framework annotations.
 */
public class ImportBatchNotConfirmableException extends RuntimeException {

  private final ImportBatchId batchId;
  private final ImportStatus currentStatus;

  public ImportBatchNotConfirmableException(ImportBatchId batchId, ImportStatus currentStatus) {
    super(
        "ImportBatch %s cannot be confirmed: current status is %s. Only VALIDATED batches are confirmable."
            .formatted(batchId.value(), currentStatus));
    this.batchId = batchId;
    this.currentStatus = currentStatus;
  }

  public ImportBatchId getBatchId() {
    return batchId;
  }

  public ImportStatus getCurrentStatus() {
    return currentStatus;
  }
}
