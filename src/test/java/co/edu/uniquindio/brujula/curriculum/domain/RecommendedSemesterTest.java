package co.edu.uniquindio.brujula.curriculum.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/**
 * Verifies RecommendedSemester is immutable, rejects null journeyCode, and rejects semester ≤ 0.
 */
class RecommendedSemesterTest {

  @Test
  void createsValidRecommendedSemester() {
    RecommendedSemester rs = new RecommendedSemester("DIURNA", 3);
    assertThat(rs.journeyCode()).isEqualTo("DIURNA");
    assertThat(rs.semester()).isEqualTo(3);
  }

  @Test
  void rejectsNullJourneyCode() {
    assertThatThrownBy(() -> new RecommendedSemester(null, 1))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("journeyCode");
  }

  @Test
  void rejectsBlankJourneyCode() {
    assertThatThrownBy(() -> new RecommendedSemester("  ", 1))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("journeyCode");
  }

  @Test
  void rejectsSemesterZero() {
    assertThatThrownBy(() -> new RecommendedSemester("DIURNA", 0))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("semester");
  }

  @Test
  void rejectsNegativeSemester() {
    assertThatThrownBy(() -> new RecommendedSemester("DIURNA", -1))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("semester");
  }

  @Test
  void isImmutableViaRecord() {
    // Records are inherently immutable — verify equals/hashCode contract
    RecommendedSemester a = new RecommendedSemester("DIURNA", 2);
    RecommendedSemester b = new RecommendedSemester("DIURNA", 2);
    assertThat(a).isEqualTo(b);
    assertThat(a.hashCode()).isEqualTo(b.hashCode());
  }

  @Test
  void twoDistinctJourneysAreNotEqual() {
    RecommendedSemester diurna = new RecommendedSemester("DIURNA", 2);
    RecommendedSemester nocturna = new RecommendedSemester("NOCTURNA", 2);
    assertThat(diurna).isNotEqualTo(nocturna);
  }
}
