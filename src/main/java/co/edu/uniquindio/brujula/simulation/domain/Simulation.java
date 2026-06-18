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
 *   <li>scopeEntries: 1..4 (programId, journeyCode) pairs; pairs must be unique (INV-002-v2).
 *   <li>At most 2 distinct programIds across all scope entries (INV-002-v2).
 *   <li>name must be non-blank and <= 120 chars.
 *   <li>Lifecycle status is ACTIVE or DELETED (CURRENT/OUTDATED are never persisted).
 * </ul>
 *
 * <p>Framework-free — no Spring/JPA annotations.
 */
public class Simulation {

  private final SimulationId id;
  private final UUID ownerId;
  private String name;
  private final UUID periodId;
  private final List<UUID> programIds;
  private final List<SimulationScopeEntry> scopeEntries;
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
      List<SimulationScopeEntry> scopeEntries,
      SimulationLifecycle lifecycleStatus,
      Instant createdAt,
      Instant updatedAt,
      long version) {
    this.id = id;
    this.ownerId = ownerId;
    this.name = name;
    this.periodId = periodId;
    this.programIds = new ArrayList<>(programIds);
    this.scopeEntries = new ArrayList<>(scopeEntries);
    this.lifecycleStatus = lifecycleStatus;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
    this.version = version;
  }

  /**
   * Factory: create a new simulation from scope entries.
   *
   * <p>Validates invariants: name non-blank, 1..4 unique (programId, journeyCode) pairs, at most 2
   * distinct programIds.
   */
  public static Simulation create(
      SimulationId id,
      UUID ownerId,
      String name,
      UUID periodId,
      List<SimulationScopeEntry> scopeEntries) {
    Objects.requireNonNull(id, "id required");
    Objects.requireNonNull(ownerId, "ownerId required");
    Objects.requireNonNull(periodId, "periodId required");
    validateName(name);
    validateScopeEntries(scopeEntries);
    List<UUID> derivedProgramIds =
        scopeEntries.stream().map(SimulationScopeEntry::programId).distinct().toList();
    Instant now = Instant.now();
    return new Simulation(
        id,
        ownerId,
        name,
        periodId,
        derivedProgramIds,
        scopeEntries,
        SimulationLifecycle.ACTIVE,
        now,
        now,
        0L);
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
      List<SimulationScopeEntry> scopeEntries,
      SimulationLifecycle lifecycleStatus,
      Instant createdAt,
      Instant updatedAt,
      long version) {
    return new Simulation(
        id,
        ownerId,
        name,
        periodId,
        programIds,
        scopeEntries,
        lifecycleStatus,
        createdAt,
        updatedAt,
        version);
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

  private static void validateScopeEntries(List<SimulationScopeEntry> entries) {
    Objects.requireNonNull(entries, "scopeEntries required");
    if (entries.isEmpty()) {
      throw new IllegalArgumentException(
          "scopeEntries must have at least 1 (programId, journeyCode) pair");
    }
    if (entries.size() > 4) {
      throw new IllegalArgumentException(
          "scopeEntries must have at most 4 (programId, journeyCode) pairs, got: "
              + entries.size());
    }
    long distinctPairs = entries.stream().distinct().count();
    if (distinctPairs != entries.size()) {
      throw new IllegalArgumentException(
          "scopeEntries (programId, journeyCode) pairs must be unique");
    }
    long distinctPrograms =
        entries.stream().map(SimulationScopeEntry::programId).distinct().count();
    if (distinctPrograms > 2) {
      throw new IllegalArgumentException(
          "scopeEntries must reference at most 2 distinct programs, got: " + distinctPrograms);
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

  /** Returns the distinct programIds derived from scope entries. Backward-compatible accessor. */
  public List<UUID> getProgramIds() {
    return List.copyOf(programIds);
  }

  /**
   * Returns the distinct programIds derived from scope entries (canonical name aligned with the
   * record accessor convention used in tests and views).
   */
  public List<UUID> programIds() {
    return List.copyOf(programIds);
  }

  /** Returns an immutable copy of the (programId, journeyCode) scope entries. */
  public List<SimulationScopeEntry> getScopeEntries() {
    return List.copyOf(scopeEntries);
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
