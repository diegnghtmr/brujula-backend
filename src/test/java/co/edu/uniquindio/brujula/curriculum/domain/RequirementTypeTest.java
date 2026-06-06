package co.edu.uniquindio.brujula.curriculum.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * WU1-1.3 RED: Verifies all 4 RequirementType enum values exist.
 */
class RequirementTypeTest {

  @Test
  void allFourValuesExist() {
    assertThat(RequirementType.values()).hasSize(4);
  }

  @Test
  void coursePassedExists() {
    assertThat(RequirementType.valueOf("COURSE_PASSED")).isEqualTo(RequirementType.COURSE_PASSED);
  }

  @Test
  void sequenceExists() {
    assertThat(RequirementType.valueOf("SEQUENCE")).isEqualTo(RequirementType.SEQUENCE);
  }

  @Test
  void minimumCreditsExists() {
    assertThat(RequirementType.valueOf("MINIMUM_CREDITS")).isEqualTo(RequirementType.MINIMUM_CREDITS);
  }

  @Test
  void configurableExists() {
    assertThat(RequirementType.valueOf("CONFIGURABLE")).isEqualTo(RequirementType.CONFIGURABLE);
  }

  @Test
  void allValuesAreDistinct() {
    assertThat(RequirementType.values()).doesNotHaveDuplicates();
  }
}
