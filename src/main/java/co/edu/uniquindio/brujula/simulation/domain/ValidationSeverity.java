package co.edu.uniquindio.brujula.simulation.domain;

/**
 * Severity of a {@link ValidationNotice}.
 *
 * <p>BLOCKING: must not allow the selection to proceed. WARNING: advisory — the selection may
 * proceed; notice rides in the response body.
 */
public enum ValidationSeverity {
  BLOCKING,
  WARNING
}
