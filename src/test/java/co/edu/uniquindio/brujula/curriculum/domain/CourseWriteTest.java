package co.edu.uniquindio.brujula.curriculum.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * TDD RED → GREEN: domain invariants for Course write operations.
 *
 * <p>Covers: Course.create defaults, archive transition, update fields, and invariant enforcement
 * (blank name, null planId, negative credits).
 */
class CourseWriteTest {

  private final UUID planId = UUID.randomUUID();
  private final UUID programId = UUID.randomUUID();

  // ---- create ----

  @Test
  void create_sets_active_status_and_version_zero() {
    Course course =
        Course.create(
            CourseId.generate(),
            planId,
            programId,
            "Cálculo I",
            CourseType.REQUIRED,
            4,
            4,
            Collections.emptyList());

    // Course.create starts ACTIVE. The service passes status=ACTIVE per the existing domain model
    // (OpenAPI CourseStatus = ACTIVE/INACTIVE/ARCHIVED).
    assertThat(course.status()).isEqualTo(CourseStatus.ACTIVE);
    assertThat(course.version()).isEqualTo(0L);
  }

  @Test
  void create_stores_provided_fields() {
    List<RecommendedSemester> semesters = List.of(new RecommendedSemester("DIURNA", 2));
    Course course =
        Course.create(
            CourseId.generate(),
            planId,
            programId,
            "Álgebra",
            CourseType.OPTIONAL,
            3,
            3,
            semesters);

    assertThat(course.name()).isEqualTo("Álgebra");
    assertThat(course.type()).isEqualTo(CourseType.OPTIONAL);
    assertThat(course.credits()).isEqualTo(3);
    assertThat(course.curricularWeeklyHours()).isEqualTo(3);
    assertThat(course.planId()).isEqualTo(planId);
    assertThat(course.programId()).isEqualTo(programId);
    assertThat(course.recommendedSemesters()).hasSize(1);
  }

  @Test
  void create_blank_name_throws() {
    assertThatThrownBy(
            () ->
                Course.create(
                    CourseId.generate(),
                    planId,
                    programId,
                    "  ",
                    CourseType.REQUIRED,
                    3,
                    3,
                    Collections.emptyList()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("name");
  }

  @Test
  void create_null_planId_throws() {
    assertThatThrownBy(
            () ->
                Course.create(
                    CourseId.generate(),
                    null,
                    programId,
                    "Valid Name",
                    CourseType.REQUIRED,
                    3,
                    3,
                    Collections.emptyList()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("planId");
  }

  @Test
  void create_negative_credits_throws() {
    assertThatThrownBy(
            () ->
                Course.create(
                    CourseId.generate(),
                    planId,
                    programId,
                    "Valid Name",
                    CourseType.REQUIRED,
                    -1,
                    3,
                    Collections.emptyList()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("credits");
  }

  // ---- update ----

  @Test
  void update_returns_new_instance_with_changed_fields() {
    Course original =
        Course.create(
            CourseId.generate(),
            planId,
            programId,
            "Cálculo I",
            CourseType.REQUIRED,
            4,
            4,
            Collections.emptyList());

    List<RecommendedSemester> newSemesters = List.of(new RecommendedSemester("NOCTURNA", 3));
    Course updated = original.update("Cálculo II", CourseType.OPTIONAL, 5, 5, newSemesters);

    assertThat(updated.name()).isEqualTo("Cálculo II");
    assertThat(updated.type()).isEqualTo(CourseType.OPTIONAL);
    assertThat(updated.credits()).isEqualTo(5);
    assertThat(updated.curricularWeeklyHours()).isEqualTo(5);
    assertThat(updated.recommendedSemesters()).hasSize(1);
    // identity unchanged
    assertThat(updated.id()).isEqualTo(original.id());
    assertThat(updated.planId()).isEqualTo(original.planId());
    assertThat(updated.programId()).isEqualTo(original.programId());
    // version preserved for optimistic lock (set by caller)
    assertThat(updated.version()).isEqualTo(original.version());
  }

  @Test
  void update_blank_name_throws() {
    Course original =
        Course.create(
            CourseId.generate(),
            planId,
            programId,
            "Cálculo I",
            CourseType.REQUIRED,
            4,
            4,
            Collections.emptyList());

    assertThatThrownBy(
            () -> original.update("", CourseType.REQUIRED, 4, 4, Collections.emptyList()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("name");
  }

  // ---- archive ----

  @Test
  void archive_transitions_status_to_archived() {
    Course course =
        Course.reconstitute(
            CourseId.generate(),
            planId,
            programId,
            null,
            "Cálculo I",
            CourseType.REQUIRED,
            4,
            4,
            Collections.emptyList(),
            CourseStatus.ACTIVE,
            0L);

    Course archived = course.archive();

    assertThat(archived.status()).isEqualTo(CourseStatus.ARCHIVED);
    assertThat(archived.id()).isEqualTo(course.id());
  }

  @Test
  void archive_already_archived_throws() {
    Course course =
        Course.reconstitute(
            CourseId.generate(),
            planId,
            programId,
            null,
            "Cálculo I",
            CourseType.REQUIRED,
            4,
            4,
            Collections.emptyList(),
            CourseStatus.ARCHIVED,
            0L);

    assertThatThrownBy(course::archive)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("archived");
  }
}
