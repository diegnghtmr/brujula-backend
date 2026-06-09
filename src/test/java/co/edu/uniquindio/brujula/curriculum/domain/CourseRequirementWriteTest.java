package co.edu.uniquindio.brujula.curriculum.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * TDD RED → GREEN: domain invariants for CourseRequirement write operations.
 *
 * <p>Covers: factory methods produce correct status/version defaults; type-specific
 * field requirements enforced.
 *
 * <p>WU5 – T5.2 (domain portion).
 */
class CourseRequirementWriteTest {

  private final UUID targetCourseId = UUID.randomUUID();
  private final UUID requiredCourseId = UUID.randomUUID();

  @Test
  void course_passed_creates_with_active_status_and_version_zero() {
    CourseRequirement req = CourseRequirement.coursePassed(
        CourseRequirementId.generate(), targetCourseId,
        requiredCourseId, false, null, null);

    assertThat(req.status()).isEqualTo(CourseRequirementStatus.ACTIVE);
    assertThat(req.version()).isEqualTo(0L);
    assertThat(req.type()).isEqualTo(RequirementType.COURSE_PASSED);
    assertThat(req.requiredCourseId()).isEqualTo(requiredCourseId);
  }

  @Test
  void sequence_requires_required_course_id() {
    assertThatThrownBy(() -> CourseRequirement.sequence(
        CourseRequirementId.generate(), targetCourseId, null, false))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("requiredCourseId");
  }

  @Test
  void minimum_credits_requires_non_null_credits() {
    assertThatThrownBy(() -> CourseRequirement.minimumCredits(
        CourseRequirementId.generate(), targetCourseId, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("requiredCredits");
  }

  @Test
  void minimum_credits_requires_non_negative_credits() {
    assertThatThrownBy(() -> CourseRequirement.minimumCredits(
        CourseRequirementId.generate(), targetCourseId, -1))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining(">=");
  }

  @Test
  void configurable_creates_with_no_required_fields() {
    CourseRequirement req = CourseRequirement.configurable(
        CourseRequirementId.generate(), targetCourseId);

    assertThat(req.type()).isEqualTo(RequirementType.CONFIGURABLE);
    assertThat(req.requiredCourseId()).isNull();
    assertThat(req.requiredCredits()).isNull();
  }
}
