package co.edu.uniquindio.brujula.programcatalog.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * DEBT-F7-003: covers uncovered branches in programcatalog/domain classes.
 *
 * <p>Targets: AcademicProgram.validate null-id, equals identity and wrong-type branches; Journey
 * equals identity and wrong-type branches; StudyPlan null programId, null name, equals branches.
 */
class ProgramCatalogDomainBranchTest {

  // =========================================================================
  // AcademicProgram
  // =========================================================================

  @Test
  void academicProgram_create_rejects_null_id() {
    assertThatThrownBy(() -> AcademicProgram.create(null, "Sistemas", "FIET"))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void academicProgram_reconstitute_rejects_null_status() {
    assertThatThrownBy(
            () -> AcademicProgram.reconstitute(ProgramId.generate(), "Sistemas", "FIET", null, 0L))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void academicProgram_equals_same_instance_is_true() {
    AcademicProgram p = AcademicProgram.create(ProgramId.generate(), "Sistemas", "FIET");
    assertThat(p.equals(p)).isTrue();
  }

  @Test
  void academicProgram_equals_wrong_type_is_false() {
    AcademicProgram p = AcademicProgram.create(ProgramId.generate(), "Sistemas", "FIET");
    assertThat(p.equals("not a program")).isFalse();
  }

  @Test
  void academicProgram_equals_different_id_is_false() {
    AcademicProgram p1 = AcademicProgram.create(ProgramId.generate(), "Sistemas", "FIET");
    AcademicProgram p2 = AcademicProgram.create(ProgramId.generate(), "Sistemas", "FIET");
    assertThat(p1.equals(p2)).isFalse();
  }

  @Test
  void academicProgram_equals_same_id_is_true() {
    ProgramId id = ProgramId.generate();
    AcademicProgram p1 = AcademicProgram.create(id, "Sistemas", "FIET");
    AcademicProgram p2 =
        AcademicProgram.reconstitute(id, "Sistemas", "FIET", ProgramStatus.ACTIVE, 0L);
    assertThat(p1.equals(p2)).isTrue();
  }

  @Test
  void academicProgram_toString_contains_expected_fields() {
    AcademicProgram p = AcademicProgram.create(ProgramId.generate(), "Sistemas", "FIET");
    assertThat(p.toString()).contains("Sistemas").contains("FIET").contains("ACTIVE");
  }

  // =========================================================================
  // Journey
  // =========================================================================

  @Test
  void journey_equals_same_instance_is_true() {
    Journey j = Journey.create(JourneyId.generate(), "DIURNA", "Jornada Diurna");
    assertThat(j.equals(j)).isTrue();
  }

  @Test
  void journey_equals_wrong_type_is_false() {
    Journey j = Journey.create(JourneyId.generate(), "DIURNA", "Jornada Diurna");
    assertThat(j.equals("not a journey")).isFalse();
  }

  @Test
  void journey_equals_different_id_is_false() {
    Journey j1 = Journey.create(JourneyId.generate(), "DIURNA", "Jornada Diurna");
    Journey j2 = Journey.create(JourneyId.generate(), "DIURNA", "Jornada Diurna");
    assertThat(j1.equals(j2)).isFalse();
  }

  @Test
  void journey_equals_same_id_is_true() {
    JourneyId id = JourneyId.generate();
    Journey j1 = Journey.create(id, "DIURNA", "Jornada Diurna");
    Journey j2 = Journey.reconstitute(id, "DIURNA", "Jornada Diurna", false);
    assertThat(j1.equals(j2)).isTrue();
  }

  @Test
  void journey_toString_contains_expected_fields() {
    Journey j = Journey.create(JourneyId.generate(), "NOCTURNA", "Jornada Nocturna");
    assertThat(j.toString()).contains("NOCTURNA").contains("Jornada Nocturna");
  }

  // =========================================================================
  // StudyPlan
  // =========================================================================

  @Test
  void studyPlan_create_rejects_null_programId() {
    assertThatThrownBy(() -> StudyPlan.create(PlanId.generate(), null, "Plan 21", 163, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("programId");
  }

  @Test
  void studyPlan_create_rejects_null_name() {
    assertThatThrownBy(
            () -> StudyPlan.create(PlanId.generate(), UUID.randomUUID(), null, 163, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("name");
  }

  @Test
  void studyPlan_reconstitute_rejects_null_status() {
    assertThatThrownBy(
            () ->
                StudyPlan.reconstitute(
                    PlanId.generate(), UUID.randomUUID(), "Plan", null, 163, null, 0L))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void studyPlan_equals_same_instance_is_true() {
    StudyPlan p = StudyPlan.create(PlanId.generate(), UUID.randomUUID(), "Plan 21", 163, null);
    assertThat(p.equals(p)).isTrue();
  }

  @Test
  void studyPlan_equals_wrong_type_is_false() {
    StudyPlan p = StudyPlan.create(PlanId.generate(), UUID.randomUUID(), "Plan 21", 163, null);
    assertThat(p.equals("not a plan")).isFalse();
  }

  @Test
  void studyPlan_equals_different_id_is_false() {
    UUID programId = UUID.randomUUID();
    StudyPlan p1 = StudyPlan.create(PlanId.generate(), programId, "Plan 21", 163, null);
    StudyPlan p2 = StudyPlan.create(PlanId.generate(), programId, "Plan 21", 163, null);
    assertThat(p1.equals(p2)).isFalse();
  }

  @Test
  void studyPlan_equals_same_id_is_true() {
    PlanId id = PlanId.generate();
    UUID programId = UUID.randomUUID();
    StudyPlan p1 = StudyPlan.create(id, programId, "Plan 21", 163, null);
    StudyPlan p2 =
        StudyPlan.reconstitute(id, programId, "Plan 21", PlanStatus.ACTIVE, 163, null, 1L);
    assertThat(p1.equals(p2)).isTrue();
  }

  @Test
  void studyPlan_toString_contains_expected_fields() {
    StudyPlan p = StudyPlan.create(PlanId.generate(), UUID.randomUUID(), "Plan 21", 163, null);
    assertThat(p.toString()).contains("Plan 21").contains("DRAFT").contains("163");
  }
}
