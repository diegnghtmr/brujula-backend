package co.edu.uniquindio.brujula.curriculum.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** WU1-1.1 RED: Verifies all 6 CourseType enum values exist and are distinct. */
class CourseTypeTest {

  @Test
  void allSixValuesExist() {
    CourseType[] values = CourseType.values();
    assertThat(values).hasSize(6);
  }

  @Test
  void requiredValueExists() {
    assertThat(CourseType.valueOf("REQUIRED")).isEqualTo(CourseType.REQUIRED);
  }

  @Test
  void personalElectiveValueExists() {
    assertThat(CourseType.valueOf("PERSONAL_ELECTIVE")).isEqualTo(CourseType.PERSONAL_ELECTIVE);
  }

  @Test
  void deepeningValueExists() {
    assertThat(CourseType.valueOf("DEEPENING")).isEqualTo(CourseType.DEEPENING);
  }

  @Test
  void degreeWorkValueExists() {
    assertThat(CourseType.valueOf("DEGREE_WORK")).isEqualTo(CourseType.DEGREE_WORK);
  }

  @Test
  void institutionalValueExists() {
    assertThat(CourseType.valueOf("INSTITUTIONAL")).isEqualTo(CourseType.INSTITUTIONAL);
  }

  @Test
  void optionalValueExists() {
    assertThat(CourseType.valueOf("OPTIONAL")).isEqualTo(CourseType.OPTIONAL);
  }

  @Test
  void allValuesAreDistinct() {
    CourseType[] values = CourseType.values();
    assertThat(values).doesNotHaveDuplicates();
  }
}
