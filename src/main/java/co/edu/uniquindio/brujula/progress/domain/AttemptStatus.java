package co.edu.uniquindio.brujula.progress.domain;

/**
 * Declared status of a single course attempt (ADR-F3-002).
 *
 * <p>Contract enum is [PASSED, FAILED, CANCELLED] — no PENDING. PENDING was removed (FIX-W2)
 * because it was a sentinel value used only by {@code EffectiveAttemptResolver} for the empty-list
 * case, diverging from the OpenAPI contract. The resolver now returns {@code Optional.empty()}
 * instead.
 *
 * <p>PASSED — student passed with a grade ≥ 3.0. FAILED — student failed with a grade ≥ 0.0 and
 * &lt; 3.0. CANCELLED — student withdrew; no grade.
 */
public enum AttemptStatus {
  PASSED,
  FAILED,
  CANCELLED
}
