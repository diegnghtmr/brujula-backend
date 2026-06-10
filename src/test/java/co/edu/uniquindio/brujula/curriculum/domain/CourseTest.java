package co.edu.uniquindio.brujula.curriculum.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** WU1-1.7 RED: Verifies Course aggregate invariants. */
class CourseTest {

  private final CourseId courseId = CourseId.generate();
  private final UUID planId = UUID.randomUUID();
  private final UUID programId = UUID.randomUUID();

  @Test
  void createsValidCourse() {
    Course course =
        Course.create(
            courseId, planId, programId, "Programación I", CourseType.REQUIRED, 3, 4, List.of());
    assertThat(course.id()).isEqualTo(courseId);
    assertThat(course.name()).isEqualTo("Programación I");
    assertThat(course.credits()).isEqualTo(3);
    assertThat(course.curricularWeeklyHours()).isEqualTo(4);
    assertThat(course.type()).isEqualTo(CourseType.REQUIRED);
  }

  @Test
  void statusDefaultsToActive() {
    Course course =
        Course.create(
            courseId, planId, programId, "Matemáticas", CourseType.REQUIRED, 4, 4, List.of());
    assertThat(course.status()).isEqualTo(CourseStatus.ACTIVE);
  }

  @Test
  void versionInitializesToZero() {
    Course course =
        Course.create(courseId, planId, programId, "Física", CourseType.REQUIRED, 3, 3, List.of());
    assertThat(course.version()).isEqualTo(0L);
  }

  @Test
  void rejectsNegativeCredits() {
    assertThatThrownBy(
            () ->
                Course.create(
                    courseId, planId, programId, "Física", CourseType.REQUIRED, -1, 3, List.of()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("credits");
  }

  @Test
  void acceptsZeroCredits() {
    Course course =
        Course.create(
            courseId, planId, programId, "Electiva", CourseType.OPTIONAL, 0, 0, List.of());
    assertThat(course.credits()).isEqualTo(0);
  }

  @Test
  void rejectsNullName() {
    assertThatThrownBy(
            () ->
                Course.create(
                    courseId, planId, programId, null, CourseType.REQUIRED, 3, 3, List.of()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("name");
  }

  @Test
  void rejectsBlankName() {
    assertThatThrownBy(
            () ->
                Course.create(
                    courseId, planId, programId, "  ", CourseType.REQUIRED, 3, 3, List.of()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("name");
  }

  @Test
  void rejectsNullRecommendedSemesters() {
    assertThatThrownBy(
            () ->
                Course.create(
                    courseId, planId, programId, "Física", CourseType.REQUIRED, 3, 3, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("recommendedSemesters");
  }

  @Test
  void acceptsRecommendedSemestersList() {
    List<RecommendedSemester> semesters =
        List.of(new RecommendedSemester("DIURNA", 3), new RecommendedSemester("NOCTURNA", 4));
    Course course =
        Course.create(
            courseId, planId, programId, "Programación II", CourseType.REQUIRED, 3, 4, semesters);
    assertThat(course.recommendedSemesters()).hasSize(2);
  }

  @Test
  void officialCodeIsNullByDefault() {
    Course course =
        Course.create(
            courseId, planId, programId, "Base Course", CourseType.REQUIRED, 3, 3, List.of());
    assertThat(course.officialCode()).isNull();
  }

  @Test
  void courseIdGenerateProducesNonNullUniqueId() {
    CourseId id1 = CourseId.generate();
    CourseId id2 = CourseId.generate();
    assertThat(id1.value()).isNotNull();
    assertThat(id1).isNotEqualTo(id2);
  }

  @Test
  void rejectsNullPlanId() {
    assertThatThrownBy(
            () ->
                Course.create(
                    courseId, null, programId, "Course", CourseType.REQUIRED, 3, 3, List.of()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("planId");
  }

  @Test
  void rejectsNullProgramId() {
    assertThatThrownBy(
            () ->
                Course.create(
                    courseId, planId, null, "Course", CourseType.REQUIRED, 3, 3, List.of()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("programId");
  }
}
