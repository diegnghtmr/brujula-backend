package co.edu.uniquindio.brujula.curriculum.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Verifies CourseRequirement aggregate validation rules.
 *
 * <p>Rules:
 *
 * <ul>
 *   <li>COURSE_PASSED requires non-null requiredCourseId.
 *   <li>MINIMUM_CREDITS requires non-null requiredCredits.
 *   <li>COURSE_PASSED with null requiredCourseId is rejected.
 *   <li>reconstitute() enforces the same per-type invariants as the factory methods.
 * </ul>
 */
class CourseRequirementTest {

  private final CourseRequirementId reqId = CourseRequirementId.generate();
  private final UUID targetCourseId = UUID.randomUUID();
  private final UUID requiredCourseId = UUID.randomUUID();

  // -------------------------------------------------------------------------
  // COURSE_PASSED
  // -------------------------------------------------------------------------

  @Test
  void createsValidCoursePassedRequirement() {
    CourseRequirement req =
        CourseRequirement.coursePassed(
            reqId, targetCourseId, requiredCourseId, false, Confidence.HIGH, false);
    assertThat(req.id()).isEqualTo(reqId);
    assertThat(req.targetCourseId()).isEqualTo(targetCourseId);
    assertThat(req.type()).isEqualTo(RequirementType.COURSE_PASSED);
    assertThat(req.requiredCourseId()).isEqualTo(requiredCourseId);
    assertThat(req.confidence()).isEqualTo(Confidence.HIGH);
    assertThat(req.inferred()).isFalse();
    assertThat(req.simultaneityAllowed()).isFalse();
  }

  @Test
  void coursePassedRejectsNullRequiredCourseId() {
    assertThatThrownBy(
            () ->
                CourseRequirement.coursePassed(
                    reqId, targetCourseId, null, false, Confidence.HIGH, false))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("requiredCourseId");
  }

  // -------------------------------------------------------------------------
  // SEQUENCE
  // -------------------------------------------------------------------------

  @Test
  void createsValidSequenceRequirement() {
    CourseRequirement req =
        CourseRequirement.sequence(reqId, targetCourseId, requiredCourseId, true);
    assertThat(req.type()).isEqualTo(RequirementType.SEQUENCE);
    assertThat(req.requiredCourseId()).isEqualTo(requiredCourseId);
    assertThat(req.simultaneityAllowed()).isTrue();
  }

  @Test
  void sequenceRejectsNullRequiredCourseId() {
    assertThatThrownBy(() -> CourseRequirement.sequence(reqId, targetCourseId, null, true))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("requiredCourseId");
  }

  // -------------------------------------------------------------------------
  // MINIMUM_CREDITS
  // -------------------------------------------------------------------------

  @Test
  void createsValidMinimumCreditsRequirement() {
    CourseRequirement req = CourseRequirement.minimumCredits(reqId, targetCourseId, 60);
    assertThat(req.type()).isEqualTo(RequirementType.MINIMUM_CREDITS);
    assertThat(req.requiredCredits()).isEqualTo(60);
    assertThat(req.requiredCourseId()).isNull();
  }

  @Test
  void minimumCreditsRejectsNullCredits() {
    assertThatThrownBy(() -> CourseRequirement.minimumCredits(reqId, targetCourseId, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("requiredCredits");
  }

  @Test
  void minimumCreditsRejectsNegativeCredits() {
    assertThatThrownBy(() -> CourseRequirement.minimumCredits(reqId, targetCourseId, -1))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("requiredCredits");
  }

  // -------------------------------------------------------------------------
  // CONFIGURABLE
  // -------------------------------------------------------------------------

  @Test
  void createsValidConfigurableRequirement() {
    CourseRequirement req = CourseRequirement.configurable(reqId, targetCourseId);
    assertThat(req.type()).isEqualTo(RequirementType.CONFIGURABLE);
    assertThat(req.requiredCourseId()).isNull();
    assertThat(req.requiredCredits()).isNull();
  }

  // -------------------------------------------------------------------------
  // Common fields
  // -------------------------------------------------------------------------

  @Test
  void statusDefaultsToActive() {
    CourseRequirement req =
        CourseRequirement.coursePassed(
            reqId, targetCourseId, requiredCourseId, false, Confidence.MEDIUM, true);
    assertThat(req.status()).isEqualTo(CourseRequirementStatus.ACTIVE);
  }

  @Test
  void versionDefaultsToZero() {
    CourseRequirement req =
        CourseRequirement.coursePassed(reqId, targetCourseId, requiredCourseId, false, null, null);
    assertThat(req.version()).isEqualTo(0L);
  }

  @Test
  void rejectsNullTargetCourseId() {
    assertThatThrownBy(
            () -> CourseRequirement.coursePassed(reqId, null, requiredCourseId, false, null, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("targetCourseId");
  }

  // -------------------------------------------------------------------------
  // reconstitute() — per-type invariants must be enforced
  // -------------------------------------------------------------------------

  @Test
  void reconstituteAllowsValidCoursePassed() {
    // A persisted COURSE_PASSED requirement with a valid requiredCourseId must reconstitute fine
    CourseRequirement req =
        CourseRequirement.reconstitute(
            reqId,
            targetCourseId,
            RequirementType.COURSE_PASSED,
            requiredCourseId,
            null,
            false,
            Confidence.HIGH,
            false,
            CourseRequirementStatus.ACTIVE,
            1L);
    assertThat(req.type()).isEqualTo(RequirementType.COURSE_PASSED);
    assertThat(req.requiredCourseId()).isEqualTo(requiredCourseId);
    assertThat(req.version()).isEqualTo(1L);
  }

  @Test
  void reconstituteRejectsCoursePassedWithNullRequiredCourseId() {
    // COURSE_PASSED with null requiredCourseId violates the invariant even via reconstitute
    assertThatThrownBy(
            () ->
                CourseRequirement.reconstitute(
                    reqId,
                    targetCourseId,
                    RequirementType.COURSE_PASSED,
                    null,
                    null,
                    false,
                    Confidence.HIGH,
                    false,
                    CourseRequirementStatus.ACTIVE,
                    1L))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("requiredCourseId");
  }

  @Test
  void reconstituteRejectsSequenceWithNullRequiredCourseId() {
    // SEQUENCE with null requiredCourseId violates per-type invariant even via reconstitute
    assertThatThrownBy(
            () ->
                CourseRequirement.reconstitute(
                    reqId,
                    targetCourseId,
                    RequirementType.SEQUENCE,
                    null,
                    null,
                    true,
                    null,
                    null,
                    CourseRequirementStatus.ACTIVE,
                    2L))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("requiredCourseId");
  }

  @Test
  void reconstituteRejectsMinimumCreditsWithNullRequiredCredits() {
    // MINIMUM_CREDITS with null requiredCredits violates per-type invariant even via reconstitute
    assertThatThrownBy(
            () ->
                CourseRequirement.reconstitute(
                    reqId,
                    targetCourseId,
                    RequirementType.MINIMUM_CREDITS,
                    null,
                    null,
                    null,
                    null,
                    null,
                    CourseRequirementStatus.ACTIVE,
                    1L))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("requiredCredits");
  }

  @Test
  void reconstituteRejectsMinimumCreditsWithNegativeCredits() {
    // MINIMUM_CREDITS with negative requiredCredits violates per-type invariant even via
    // reconstitute
    assertThatThrownBy(
            () ->
                CourseRequirement.reconstitute(
                    reqId,
                    targetCourseId,
                    RequirementType.MINIMUM_CREDITS,
                    null,
                    -5,
                    null,
                    null,
                    null,
                    CourseRequirementStatus.ACTIVE,
                    1L))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("requiredCredits");
  }

  @Test
  void reconstituteAllowsValidMinimumCredits() {
    // A persisted MINIMUM_CREDITS requirement with valid requiredCredits must reconstitute fine
    CourseRequirement req =
        CourseRequirement.reconstitute(
            reqId,
            targetCourseId,
            RequirementType.MINIMUM_CREDITS,
            null,
            60,
            null,
            null,
            null,
            CourseRequirementStatus.ACTIVE,
            3L);
    assertThat(req.type()).isEqualTo(RequirementType.MINIMUM_CREDITS);
    assertThat(req.requiredCredits()).isEqualTo(60);
  }

  @Test
  void reconstituteAllowsConfigurableWithNullFields() {
    // CONFIGURABLE requires no specific fields — both null course ID and null credits are fine
    CourseRequirement req =
        CourseRequirement.reconstitute(
            reqId,
            targetCourseId,
            RequirementType.CONFIGURABLE,
            null,
            null,
            null,
            null,
            null,
            CourseRequirementStatus.ARCHIVED,
            1L);
    assertThat(req.type()).isEqualTo(RequirementType.CONFIGURABLE);
  }
}
