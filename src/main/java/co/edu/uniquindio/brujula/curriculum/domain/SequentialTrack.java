package co.edu.uniquindio.brujula.curriculum.domain;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * SequentialTrack — aggregate representing an ordered electiva or profundización track within a
 * study plan.
 *
 * <p>Contains an ordered list of {@link SequentialTrackLevel} objects, each of which holds the
 * course options selectable at that level (e.g. Electiva I, Electiva II).
 *
 * <p>The optional {@code gatekeeperCourseId} (added by V014) is the course a student must have
 * passed before enrolling in level 1 of this track (DEEPENING tracks only; nullable for
 * PERSONAL_ELECTIVE tracks and DEEPENING tracks without a defined prereq).
 *
 * <p>Status is a read-only aggregate — no write use cases for this aggregate in Fase 2.
 * {@code @Version} is persisted for future optimistic-lock support.
 *
 * <p>No Spring/JPA imports (ARQ-002).
 */
public final class SequentialTrack {

  private final SequentialTrackId id;
  private final UUID planId;
  private final TrackKind kind;
  private final String label;
  private final boolean consecutiveSimultaneityAllowed;
  private final UUID gatekeeperCourseId; // nullable; DEEPENING tracks only (V014)
  private final List<SequentialTrackLevel> levels;
  private final long version;

  private SequentialTrack(
      SequentialTrackId id,
      UUID planId,
      TrackKind kind,
      String label,
      boolean consecutiveSimultaneityAllowed,
      UUID gatekeeperCourseId,
      List<SequentialTrackLevel> levels,
      long version) {
    this.id = id;
    this.planId = planId;
    this.kind = kind;
    this.label = label;
    this.consecutiveSimultaneityAllowed = consecutiveSimultaneityAllowed;
    this.gatekeeperCourseId = gatekeeperCourseId;
    this.levels = Collections.unmodifiableList(levels);
    this.version = version;
  }

  /** Reconstitute a SequentialTrack from persistence (all fields explicit; no default logic). */
  public static SequentialTrack reconstitute(
      SequentialTrackId id,
      UUID planId,
      TrackKind kind,
      String label,
      boolean consecutiveSimultaneityAllowed,
      UUID gatekeeperCourseId,
      List<SequentialTrackLevel> levels,
      long version) {
    Objects.requireNonNull(id, "id must not be null");
    Objects.requireNonNull(planId, "planId must not be null");
    Objects.requireNonNull(kind, "kind must not be null");
    if (label == null || label.isBlank()) {
      throw new IllegalArgumentException("label must not be null or blank");
    }
    Objects.requireNonNull(levels, "levels must not be null");
    return new SequentialTrack(
        id,
        planId,
        kind,
        label,
        consecutiveSimultaneityAllowed,
        gatekeeperCourseId,
        List.copyOf(levels),
        version);
  }

  // ---------------------------------------------------------------------------
  // Accessors
  // ---------------------------------------------------------------------------

  public SequentialTrackId id() {
    return id;
  }

  public UUID planId() {
    return planId;
  }

  public TrackKind kind() {
    return kind;
  }

  public String label() {
    return label;
  }

  public boolean consecutiveSimultaneityAllowed() {
    return consecutiveSimultaneityAllowed;
  }

  /** Nullable: the gatekeeper course UUID, or {@code null} if no gatekeeper is defined. */
  public UUID gatekeeperCourseId() {
    return gatekeeperCourseId;
  }

  public List<SequentialTrackLevel> levels() {
    return levels;
  }

  public long version() {
    return version;
  }

  // ---------------------------------------------------------------------------
  // Equality (identity-based)
  // ---------------------------------------------------------------------------

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof SequentialTrack other)) return false;
    return Objects.equals(id, other.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }

  @Override
  public String toString() {
    return "SequentialTrack[id=%s, planId=%s, kind=%s, label=%s, levels=%d]"
        .formatted(id, planId, kind, label, levels.size());
  }
}
