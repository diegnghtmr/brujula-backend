package co.edu.uniquindio.brujula.curriculum.domain;

/**
 * Lifecycle status enum for a {@link DegreeWorkModality}.
 *
 * <p>Maps to the {@code status} column CHECK constraint in V011: {@code 'ACTIVE' | 'ARCHIVED'}.
 *
 * <p>No Spring/JPA imports.
 */
public enum DegreeWorkModalityStatus {
  ACTIVE,
  ARCHIVED
}
