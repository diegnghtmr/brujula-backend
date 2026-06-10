package co.edu.uniquindio.brujula.simulation.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Simulation aggregate root.
 *
 * <p>Invariants:
 *
 * <ul>
 *   <li>programIds must have 1 or 2 elements (INV-002).
 *   <li>programIds must be unique (contract {@code uniqueItems}).
 *   <li>name must be non-blank and <= 120 chars.
 *   <li>Lifecycle status is ACTIVE or DELETED (INV-004: CURRENT/OUTDATED never persisted).
 * </ul>
 *
 * <p>Framework-free — no Spring/JPA annotations (ARQ-002).
 */
public class Simulation {

  private final SimulationId id;
  private final UUID ownerId;
  private String name;
  private final UUID periodId;
  private final List<UUID> programIds;
  private SimulationLifecycle lifecycleStatus;
  private final Instant createdAt;
  private Instant updatedAt;
  private long version;

  /** Package-private: used ONLY by reconstitute() / mapper. */
  Simulation(
      SimulationId id,
      UUID ownerId,
      String name,
      UUID periodId,
      List<UUID> programIds,
      SimulationLifecycle lifecycleStatus,
      Instant createdAt,
      Instant updatedAt,
      long version) {
    this.id = id;
    this.ownerId = ownerId;
    this.name = name;
    this.periodId = periodId;
    this.programIds = new ArrayList<>(programIds);
    this.lifecycleStatus = lifecycleStatus;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
    this.version = version;
  }

  /**
   * Factory: create a new simulation.
   *
   * <p>Validates invariants: name non-blank, 1..2 unique programIds.
   */
  public static Simulation create(
      SimulationId id, UUID ownerId, String name, UUID periodId, List<UUID> programIds) {
    Objects.requireNonNull(id, "id required");
    Objects.requireNonNull(ownerId, "ownerId required");
    Objects.requireNonNull(periodId, "periodId required");
    validateName(name);
    validateProgramIds(programIds);
    Instant now = Instant.now();
    return new Simulation(
        id, ownerId, name, periodId, programIds, SimulationLifecycle.ACTIVE, now, now, 0L);
  }

  /**
   * Reconstitute from persistence. Public factory invoked by the persistence mapper, which lives in
   * a different (infrastructure) package and therefore requires public visibility.
   */
  public static Simulation reconstitute(
      SimulationId id,
      UUID ownerId,
      String name,
      UUID periodId,
      List<UUID> programIds,
      SimulationLifecycle lifecycleStatus,
      Instant createdAt,
      Instant updatedAt,
      long version) {
    return new Simulation(
        id, ownerId, name, periodId, programIds, lifecycleStatus, createdAt, updatedAt, version);
  }

  // -------------------------------------------------------------------------
  // Business methods
  // -------------------------------------------------------------------------

  /**
   * Rename the simulation. Validates name invariant.
   *
   * @param newName the new name (1..120 chars, non-blank)
   */
  public void rename(String newName) {
    validateName(newName);
    this.name = newName;
    this.updatedAt = Instant.now();
  }

  /** Soft-delete the simulation. */
  public void markDeleted() {
    this.lifecycleStatus = SimulationLifecycle.DELETED;
    this.updatedAt = Instant.now();
  }

  // -------------------------------------------------------------------------
  // Invariant validators
  // -------------------------------------------------------------------------

  private static void validateName(String name) {
    Objects.requireNonNull(name, "name required");
    if (name.isBlank()) {
      throw new IllegalArgumentException("Simulation name must not be blank");
    }
    if (name.length() > 120) {
      throw new IllegalArgumentException(
          "Simulation name must be <= 120 characters, got: " + name.length());
    }
  }

  private static void validateProgramIds(List<UUID> programIds) {
    Objects.requireNonNull(programIds, "programIds required");
    if (programIds.isEmpty() || programIds.size() > 2) {
      throw new IllegalArgumentException(
          "programIds must have 1 or 2 elements, got: " + programIds.size());
    }
    long distinct = programIds.stream().distinct().count();
    if (distinct != programIds.size()) {
      throw new IllegalArgumentException("programIds must be unique");
    }
  }

  // -------------------------------------------------------------------------
  // Accessors
  // -------------------------------------------------------------------------

  public SimulationId getId() {
    return id;
  }

  public UUID getOwnerId() {
    return ownerId;
  }

  public String getName() {
    return name;
  }

  public UUID getPeriodId() {
    return periodId;
  }

  public List<UUID> getProgramIds() {
    return List.copyOf(programIds);
  }

  public SimulationLifecycle getLifecycleStatus() {
    return lifecycleStatus;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public long getVersion() {
    return version;
  }

  public void setVersion(long version) {
    this.version = version;
  }

  public void setUpdatedAt(Instant updatedAt) {
    this.updatedAt = updatedAt;
  }
}
