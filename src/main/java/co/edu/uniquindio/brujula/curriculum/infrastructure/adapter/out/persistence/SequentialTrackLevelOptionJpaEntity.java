package co.edu.uniquindio.brujula.curriculum.infrastructure.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * JPA entity mapping to {@code curriculum.sequential_track_level_option} (created by Flyway V011).
 *
 * <p>PK is composite: {@code (level_id, course_id)}. Mapped via {@code @EmbeddedId}.
 * Package-private per ARQ-006.
 */
@Entity
@Table(name = "sequential_track_level_option", schema = "curriculum")
class SequentialTrackLevelOptionJpaEntity {

  @EmbeddedId private SequentialTrackLevelOptionId id;

  @MapsId("levelId")
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "level_id", nullable = false)
  private SequentialTrackLevelJpaEntity level;

  // JPA requires a no-arg constructor
  protected SequentialTrackLevelOptionJpaEntity() {}

  SequentialTrackLevelOptionId getId() {
    return id;
  }

  UUID getCourseId() {
    return id != null ? id.courseId() : null;
  }

  // ---------------------------------------------------------------------------
  // Composite PK
  // ---------------------------------------------------------------------------

  @Embeddable
  static class SequentialTrackLevelOptionId implements Serializable {

    @Column(name = "level_id", nullable = false)
    private UUID levelId;

    @Column(name = "course_id", nullable = false)
    private UUID courseId;

    protected SequentialTrackLevelOptionId() {}

    UUID levelId() {
      return levelId;
    }

    UUID courseId() {
      return courseId;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof SequentialTrackLevelOptionId other)) return false;
      return Objects.equals(levelId, other.levelId) && Objects.equals(courseId, other.courseId);
    }

    @Override
    public int hashCode() {
      return Objects.hash(levelId, courseId);
    }
  }
}
