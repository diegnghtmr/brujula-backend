package co.edu.uniquindio.brujula.curriculum.infrastructure.adapter.out.persistence;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * JPA entity mapping to {@code curriculum.sequential_track} (V011) plus {@code
 * gatekeeper_course_id} column (V014).
 *
 * <p>Package-private per ARQ-006. Levels are eagerly loaded and ordered by level number. Optimistic
 * locking via {@code @Version} (DB-006/DT-014).
 */
@Entity
@Table(name = "sequential_track", schema = "curriculum")
class SequentialTrackJpaEntity {

  @Id
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @Column(name = "plan_id", nullable = false)
  private UUID planId;

  @Column(name = "kind", nullable = false)
  private String kind;

  @Column(name = "label", nullable = false)
  private String label;

  @Column(name = "consecutive_simultaneity_allowed", nullable = false)
  private boolean consecutiveSimultaneityAllowed;

  @Column(name = "gatekeeper_course_id")
  private UUID gatekeeperCourseId; // nullable; added by V014

  @Version
  @Column(name = "version", nullable = false)
  private long version;

  @OneToMany(
      mappedBy = "track",
      cascade = CascadeType.ALL,
      orphanRemoval = true,
      fetch = FetchType.EAGER)
  @OrderBy("level ASC")
  private List<SequentialTrackLevelJpaEntity> levels = new ArrayList<>();

  // JPA requires a no-arg constructor
  protected SequentialTrackJpaEntity() {}

  UUID getId() {
    return id;
  }

  UUID getPlanId() {
    return planId;
  }

  String getKind() {
    return kind;
  }

  String getLabel() {
    return label;
  }

  boolean isConsecutiveSimultaneityAllowed() {
    return consecutiveSimultaneityAllowed;
  }

  UUID getGatekeeperCourseId() {
    return gatekeeperCourseId;
  }

  long getVersion() {
    return version;
  }

  List<SequentialTrackLevelJpaEntity> getLevels() {
    return levels;
  }
}
