package co.edu.uniquindio.brujula.curriculum.domain;

/**
 * Discriminator enum for the kind of sequential track.
 *
 * <p>Maps to the {@code kind} column CHECK constraint in V011:
 * {@code 'PERSONAL_ELECTIVE' | 'DEEPENING'}.
 *
 * <p>No Spring/JPA imports (ARQ-002).
 */
public enum TrackKind {
  PERSONAL_ELECTIVE,
  DEEPENING
}
