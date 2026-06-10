package co.edu.uniquindio.brujula.simulation.domain;

import java.util.Objects;
import java.util.UUID;

/**
 * Credit limit policy for a program in a period.
 *
 * <p>Sourced from {@code CreditLimitPort} (→ administration::api). Pure value record (ARQ-002).
 *
 * @param programId the program UUID
 * @param programName human-readable program name (for metadata in notices)
 * @param limit maximum allowed credits (from administration::api, default 23)
 */
public record ProgramCreditLimit(UUID programId, String programName, int limit) {

  public ProgramCreditLimit {
    Objects.requireNonNull(programId, "programId required");
    Objects.requireNonNull(programName, "programName required");
    if (limit < 1) {
      throw new IllegalArgumentException("Credit limit must be >= 1, got: " + limit);
    }
  }
}
