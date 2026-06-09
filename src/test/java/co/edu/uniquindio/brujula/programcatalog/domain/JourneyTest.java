package co.edu.uniquindio.brujula.programcatalog.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/**
 * FIX-3 (W-2) RED: Journey aggregate domain invariant tests (CAT-003).
 *
 * <p>Asserts: blank/null code rejected; blank/null name rejected; null id rejected; {@code
 * create()} defaults {@code active} to {@code true}.
 */
class JourneyTest {

  @Test
  void createWithValidFieldsSucceeds() {
    JourneyId id = JourneyId.generate();
    Journey journey = Journey.create(id, "DIURNA", "Jornada Diurna");

    assertThat(journey.id()).isEqualTo(id);
    assertThat(journey.code()).isEqualTo("DIURNA");
    assertThat(journey.name()).isEqualTo("Jornada Diurna");
  }

  @Test
  void createDefaultsActiveToTrue() {
    Journey journey = Journey.create(JourneyId.generate(), "NOCTURNA", "Jornada Nocturna");

    assertThat(journey.active()).isTrue();
  }

  @Test
  void rejectsNullId() {
    assertThatThrownBy(() -> Journey.create(null, "DIURNA", "Jornada Diurna"))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void rejectsBlankCode() {
    assertThatThrownBy(() -> Journey.create(JourneyId.generate(), "   ", "Jornada Diurna"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("code");
  }

  @Test
  void rejectsNullCode() {
    assertThatThrownBy(() -> Journey.create(JourneyId.generate(), null, "Jornada Diurna"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("code");
  }

  @Test
  void rejectsBlankName() {
    assertThatThrownBy(() -> Journey.create(JourneyId.generate(), "DIURNA", "  "))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("name");
  }

  @Test
  void rejectsNullName() {
    assertThatThrownBy(() -> Journey.create(JourneyId.generate(), "DIURNA", null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("name");
  }

  @Test
  void reconstitutePreservesAllFields() {
    JourneyId id = JourneyId.generate();
    Journey journey = Journey.reconstitute(id, "NOCTURNA", "Jornada Nocturna", false);

    assertThat(journey.id()).isEqualTo(id);
    assertThat(journey.code()).isEqualTo("NOCTURNA");
    assertThat(journey.name()).isEqualTo("Jornada Nocturna");
    assertThat(journey.active()).isFalse();
  }
}
