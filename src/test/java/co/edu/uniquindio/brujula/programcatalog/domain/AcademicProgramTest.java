package co.edu.uniquindio.brujula.programcatalog.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/**
 * WU2-1.1 RED: AcademicProgram invariant tests.
 *
 * <p>Asserts: name non-blank; faculty non-blank; status defaults to ACTIVE; version initializes to 0.
 */
class AcademicProgramTest {

  @Test
  void createWithValidFieldsSucceeds() {
    ProgramId id = ProgramId.generate();
    AcademicProgram program = AcademicProgram.create(id, "Ingeniería de Sistemas", "FIET");

    assertThat(program.id()).isEqualTo(id);
    assertThat(program.name()).isEqualTo("Ingeniería de Sistemas");
    assertThat(program.faculty()).isEqualTo("FIET");
  }

  @Test
  void statusDefaultsToActive() {
    AcademicProgram program = AcademicProgram.create(ProgramId.generate(), "Ingeniería de Sistemas", "FIET");

    assertThat(program.status()).isEqualTo(ProgramStatus.ACTIVE);
  }

  @Test
  void versionInitializesToZero() {
    AcademicProgram program = AcademicProgram.create(ProgramId.generate(), "Ingeniería de Sistemas", "FIET");

    assertThat(program.version()).isEqualTo(0L);
  }

  @Test
  void rejectsBlankName() {
    assertThatThrownBy(() -> AcademicProgram.create(ProgramId.generate(), "  ", "FIET"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("name");
  }

  @Test
  void rejectsNullName() {
    assertThatThrownBy(() -> AcademicProgram.create(ProgramId.generate(), null, "FIET"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("name");
  }

  @Test
  void rejectsBlankFaculty() {
    assertThatThrownBy(() -> AcademicProgram.create(ProgramId.generate(), "Sistemas", "   "))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("faculty");
  }

  @Test
  void rejectsNullFaculty() {
    assertThatThrownBy(() -> AcademicProgram.create(ProgramId.generate(), "Sistemas", null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("faculty");
  }

  @Test
  void reconstructsFromPersistenceWithAllFields() {
    ProgramId id = ProgramId.generate();
    AcademicProgram program = AcademicProgram.reconstitute(id, "Lenguas Modernas", "FCHSH", ProgramStatus.INACTIVE, 3L);

    assertThat(program.id()).isEqualTo(id);
    assertThat(program.name()).isEqualTo("Lenguas Modernas");
    assertThat(program.faculty()).isEqualTo("FCHSH");
    assertThat(program.status()).isEqualTo(ProgramStatus.INACTIVE);
    assertThat(program.version()).isEqualTo(3L);
  }
}
