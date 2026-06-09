package co.edu.uniquindio.brujula.simulation.domain;

import java.util.List;
import java.util.Objects;

/**
 * Thrown when the validation engine produces one or more blocking notices (→ HTTP 409
 * problem+json).
 *
 * <p>The exception carries the full list of blocking notices so the exception handler can render a
 * structured problem+json body with {@code code}, {@code title}, {@code detail}, and {@code
 * metadata} (spec VAL-001, VAL-002..005).
 */
public class SimulationValidationBlockedException extends RuntimeException {

  private final List<ValidationNotice> blocking;

  public SimulationValidationBlockedException(List<ValidationNotice> blocking) {
    super(buildMessage(blocking));
    Objects.requireNonNull(blocking, "blocking must not be null");
    if (blocking.isEmpty()) {
      throw new IllegalArgumentException("At least one blocking notice is required");
    }
    this.blocking = List.copyOf(blocking);
  }

  /** The primary (first) blocking notice — used by exception handler for code + title. */
  public ValidationNotice primaryNotice() {
    return blocking.getFirst();
  }

  public List<ValidationNotice> getBlocking() {
    return blocking;
  }

  private static String buildMessage(List<ValidationNotice> blocking) {
    if (blocking == null || blocking.isEmpty()) {
      return "Validation blocked";
    }
    return "Selection blocked by validation: " + blocking.getFirst().code();
  }
}
