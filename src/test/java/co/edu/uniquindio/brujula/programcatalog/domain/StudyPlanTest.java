package co.edu.uniquindio.brujula.programcatalog.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * WU2-1.3 RED: StudyPlan invariant tests.
 *
 * <p>Asserts: totalCredits > 0; status defaults to DRAFT; version field exists. Also covers Journey
 * aggregate basic properties.
 */
class StudyPlanTest {

  private final UUID programId = UUID.randomUUID();

  @Test
  void createWithValidFieldsSucceeds() {
    PlanId id = PlanId.generate();
    StudyPlan plan = StudyPlan.create(id, programId, "Plan 21", 163, null);

    assertThat(plan.id()).isEqualTo(id);
    assertThat(plan.programId()).isEqualTo(programId);
    assertThat(plan.name()).isEqualTo("Plan 21");
    assertThat(plan.totalCredits()).isEqualTo(163);
    assertThat(plan.referenceYear()).isNull();
  }

  @Test
  void statusDefaultsToDraft() {
    StudyPlan plan = StudyPlan.create(PlanId.generate(), programId, "Plan 21", 163, null);

    assertThat(plan.status()).isEqualTo(PlanStatus.DRAFT);
  }

  @Test
  void versionInitializesToZero() {
    StudyPlan plan = StudyPlan.create(PlanId.generate(), programId, "Plan 21", 163, null);

    assertThat(plan.version()).isEqualTo(0L);
  }

  @Test
  void rejectsTotalCreditsOfZero() {
    assertThatThrownBy(() -> StudyPlan.create(PlanId.generate(), programId, "Plan", 0, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("totalCredits");
  }

  @Test
  void rejectsNegativeTotalCredits() {
    assertThatThrownBy(() -> StudyPlan.create(PlanId.generate(), programId, "Plan", -1, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("totalCredits");
  }

  @Test
  void acceptsReferenceYear() {
    StudyPlan plan = StudyPlan.create(PlanId.generate(), programId, "Plan 21", 163, 2021);

    assertThat(plan.referenceYear()).isEqualTo(2021);
  }

  @Test
  void rejectsBlankName() {
    assertThatThrownBy(() -> StudyPlan.create(PlanId.generate(), programId, "  ", 163, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("name");
  }

  @Test
  void reconstitutesFromPersistenceWithAllStatuses() {
    PlanId id = PlanId.generate();
    StudyPlan plan =
        StudyPlan.reconstitute(id, programId, "Plan 21", PlanStatus.ACTIVE, 163, 2021, 5L);

    assertThat(plan.status()).isEqualTo(PlanStatus.ACTIVE);
    assertThat(plan.version()).isEqualTo(5L);
    assertThat(plan.referenceYear()).isEqualTo(2021);
  }

  @Test
  void reconstitutesArchivedPlan() {
    StudyPlan plan =
        StudyPlan.reconstitute(
            PlanId.generate(), programId, "Old Plan", PlanStatus.ARCHIVED, 100, null, 2L);

    assertThat(plan.status()).isEqualTo(PlanStatus.ARCHIVED);
  }
}
