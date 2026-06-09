package co.edu.uniquindio.brujula.curriculum.infrastructure.adapter.out.persistence;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * JPA entity mapping to {@code curriculum.sequential_track_level} (created by Flyway V011).
 *
 * <p>Package-private per ARQ-006. Contains ordered level options via {@link
 * SequentialTrackLevelOptionJpaEntity}.
 */
@Entity
@Table(name = "sequential_track_level", schema = "curriculum")
class SequentialTrackLevelJpaEntity {

  @Id
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "track_id", nullable = false)
  private SequentialTrackJpaEntity track;

  @Column(name = "level", nullable = false)
  private int level;

  @OneToMany(
      mappedBy = "level",
      cascade = CascadeType.ALL,
      orphanRemoval = true,
      fetch = FetchType.EAGER)
  private List<SequentialTrackLevelOptionJpaEntity> options = new ArrayList<>();

  // JPA requires a no-arg constructor
  protected SequentialTrackLevelJpaEntity() {}

  UUID getId() {
    return id;
  }

  int getLevel() {
    return level;
  }

  List<SequentialTrackLevelOptionJpaEntity> getOptions() {
    return options;
  }
}
