package co.edu.uniquindio.brujula.curriculum.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/**
 * WU1-2.1 RED: Verifies OfficialCodeMinter determinism, accent-folding, non-alnum stripping, code
 * format, and uniqueness for similar-name courses.
 */
class OfficialCodeMinterTest {

  private final OfficialCodeMinter minter = new OfficialCodeMinter();

  @Test
  void sameNameAndContextProducesSameCode() {
    String code1 = minter.mint("ISC", 2, "Programación I");
    String code2 = minter.mint("ISC", 2, "Programación I");
    assertThat(code1).isEqualTo(code2);
  }

  @Test
  void codeFollowsFormat() {
    // Format: {PROG}-S{sem}-{SLUG}
    String code = minter.mint("ISC", 3, "Programación I");
    assertThat(code).startsWith("ISC-S3-");
  }

  @Test
  void accentFoldingRemovedFromSlug() {
    // "Programación I" → accent removed → slug from "Programacion I"
    String code = minter.mint("ISC", 2, "Programación I");
    assertThat(code).doesNotContain("ó");
    assertThat(code).doesNotContain("á");
    assertThat(code).doesNotContain("é");
    assertThat(code).doesNotContain("ú");
    assertThat(code).doesNotContain("ñ");
  }

  @Test
  void slugIsUppercase() {
    String code = minter.mint("ISC", 1, "Lógica de Programación");
    // After ISC-S1- the slug must be uppercase
    String slug = code.substring("ISC-S1-".length());
    assertThat(slug).isEqualTo(slug.toUpperCase());
  }

  @Test
  void nonAlnumCharsStrippedFromSlug() {
    // Spaces and special chars stripped
    String code = minter.mint("ISC", 1, "Lógica de Programación");
    String slug = code.substring("ISC-S1-".length());
    assertThat(slug).matches("[A-Z0-9]+");
  }

  @Test
  void distinctCodesForSimilarNames() {
    // "Programación I" vs "Programación II" must yield distinct codes
    String code1 = minter.mint("ISC", 2, "Programación I");
    String code2 = minter.mint("ISC", 3, "Programación II");
    assertThat(code1).isNotEqualTo(code2);
  }

  @Test
  void distinctCodesForSameProgSameSemSimilarNames() {
    // FIX-3 (WARNING-3): real collision risk — same program, same semester, suffix-differentiated
    // names
    // "ISC", 2, "Programación I"  → slug "PROGRAMACIONI"  → ISC-S2-PROGRAMACIONI
    // "ISC", 2, "Programación II" → slug "PROGRAMACIONII" → ISC-S2-PROGRAMACIONII
    // These MUST be distinct; this test locks in the behavior so a future slug change that
    // accidentally collapses both to the same code will fail loudly.
    String code1 = minter.mint("ISC", 2, "Programación I");
    String code2 = minter.mint("ISC", 2, "Programación II");
    assertThat(code1).isNotEqualTo(code2);
  }

  @Test
  void differentSemesterProducesDistinctCode() {
    String code1 = minter.mint("ISC", 1, "Matemáticas");
    String code2 = minter.mint("ISC", 2, "Matemáticas");
    assertThat(code1).isNotEqualTo(code2);
  }

  @Test
  void differentProgramCodeProducesDistinctCode() {
    String code1 = minter.mint("ISC", 1, "Matemáticas");
    String code2 = minter.mint("LM", 1, "Matemáticas");
    assertThat(code1).isNotEqualTo(code2);
  }

  @Test
  void rejectsNullProgramCode() {
    assertThatThrownBy(() -> minter.mint(null, 1, "Course"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsNullCourseName() {
    assertThatThrownBy(() -> minter.mint("ISC", 1, null))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsInvalidSemester() {
    assertThatThrownBy(() -> minter.mint("ISC", 0, "Course"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void accentedVowelsAreNormalized() {
    // All accented vowels must be folded: á→A, é→E, í→I, ó→O, ú→U, ñ→N
    String code = minter.mint("LM", 1, "Inglés Básico");
    String slug = code.substring("LM-S1-".length());
    assertThat(slug).isEqualTo("INGLESBASICO");
  }
}
