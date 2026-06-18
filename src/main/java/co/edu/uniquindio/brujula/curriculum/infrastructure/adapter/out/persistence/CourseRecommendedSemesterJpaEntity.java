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
 * JPA entity mapping to {@code curriculum.course_recommended_semester} (created by Flyway V011).
 *
 * <p>PK is composite: {@code (course_id, journey_code)}. Mapped via {@code @EmbeddedId}.
 * Package-private.
 */
@Entity
@Table(name = "course_recommended_semester", schema = "curriculum")
class CourseRecommendedSemesterJpaEntity {

  @EmbeddedId private CourseRecommendedSemesterId id;

  @MapsId("courseId")
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "course_id", nullable = false)
  private CourseJpaEntity course;

  @Column(name = "semester", nullable = false)
  private int semester;

  // JPA requires a no-arg constructor
  protected CourseRecommendedSemesterJpaEntity() {}

  CourseRecommendedSemesterId getId() {
    return id;
  }

  String getJourneyCode() {
    return id != null ? id.journeyCode() : null;
  }

  int getSemester() {
    return semester;
  }

  // Setters for the write path (CUR-001)
  void setId(CourseRecommendedSemesterId id) {
    this.id = id;
  }

  void setCourse(CourseJpaEntity course) {
    this.course = course;
  }

  void setSemester(int semester) {
    this.semester = semester;
  }

  // ---------------------------------------------------------------------------
  // Composite PK
  // ---------------------------------------------------------------------------

  @Embeddable
  static class CourseRecommendedSemesterId implements Serializable {

    @Column(name = "course_id", nullable = false)
    private UUID courseId;

    @Column(name = "journey_code", nullable = false)
    private String journeyCode;

    protected CourseRecommendedSemesterId() {}

    // Factory for the write path
    static CourseRecommendedSemesterId of(UUID courseId, String journeyCode) {
      CourseRecommendedSemesterId pk = new CourseRecommendedSemesterId();
      pk.courseId = courseId;
      pk.journeyCode = journeyCode;
      return pk;
    }

    UUID courseId() {
      return courseId;
    }

    String journeyCode() {
      return journeyCode;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof CourseRecommendedSemesterId other)) return false;
      return Objects.equals(courseId, other.courseId)
          && Objects.equals(journeyCode, other.journeyCode);
    }

    @Override
    public int hashCode() {
      return Objects.hash(courseId, journeyCode);
    }
  }
}
