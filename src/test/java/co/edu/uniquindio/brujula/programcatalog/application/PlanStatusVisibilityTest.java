package co.edu.uniquindio.brujula.programcatalog.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import co.edu.uniquindio.brujula.programcatalog.application.dto.StudyPlanView;
import co.edu.uniquindio.brujula.programcatalog.application.port.in.ListProgramPlansQuery;
import co.edu.uniquindio.brujula.programcatalog.application.port.out.LoadStudyPlanPort;
import co.edu.uniquindio.brujula.programcatalog.application.service.ListProgramPlansService;
import co.edu.uniquindio.brujula.programcatalog.domain.PlanId;
import co.edu.uniquindio.brujula.programcatalog.domain.PlanStatus;
import co.edu.uniquindio.brujula.programcatalog.domain.StudyPlan;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * WU2-1.5 RED: Verifies ListProgramPlansService excludes DRAFT and ARCHIVED plans when
 * callerRole=STUDENT but includes all statuses for STAFF and ADMIN (CAT-002).
 */
class PlanStatusVisibilityTest {

  private LoadStudyPlanPort loadStudyPlanPort;
  private ListProgramPlansQuery service;
  private UUID programId;

  @BeforeEach
  void setUp() {
    loadStudyPlanPort = mock(LoadStudyPlanPort.class);
    service = new ListProgramPlansService(loadStudyPlanPort);
    programId = UUID.randomUUID();
  }

  @Test
  void studentSeesOnlyActivePlans() {
    StudyPlan activePlan = StudyPlan.reconstitute(
        PlanId.generate(), programId, "Plan 21", PlanStatus.ACTIVE, 163, 2021, 0L);
    StudyPlan draftPlan = StudyPlan.reconstitute(
        PlanId.generate(), programId, "Draft Plan", PlanStatus.DRAFT, 100, null, 0L);
    StudyPlan archivedPlan = StudyPlan.reconstitute(
        PlanId.generate(), programId, "Old Plan", PlanStatus.ARCHIVED, 120, 2010, 1L);

    when(loadStudyPlanPort.loadByProgramId(programId))
        .thenReturn(List.of(activePlan, draftPlan, archivedPlan));

    List<StudyPlanView> result = service.listProgramPlans(programId, "STUDENT");

    assertThat(result).hasSize(1);
    assertThat(result.get(0).status()).isEqualTo("ACTIVE");
  }

  @Test
  void draftPlanIsHiddenFromStudent() {
    StudyPlan draftPlan = StudyPlan.reconstitute(
        PlanId.generate(), programId, "Draft Plan", PlanStatus.DRAFT, 100, null, 0L);

    when(loadStudyPlanPort.loadByProgramId(programId)).thenReturn(List.of(draftPlan));

    List<StudyPlanView> result = service.listProgramPlans(programId, "STUDENT");

    assertThat(result).isEmpty();
  }

  @Test
  void archivedPlanIsHiddenFromStudent() {
    StudyPlan archivedPlan = StudyPlan.reconstitute(
        PlanId.generate(), programId, "Old Plan", PlanStatus.ARCHIVED, 120, 2010, 1L);

    when(loadStudyPlanPort.loadByProgramId(programId)).thenReturn(List.of(archivedPlan));

    List<StudyPlanView> result = service.listProgramPlans(programId, "STUDENT");

    assertThat(result).isEmpty();
  }

  @Test
  void staffSeesAllStatuses() {
    StudyPlan activePlan = StudyPlan.reconstitute(
        PlanId.generate(), programId, "Plan 21", PlanStatus.ACTIVE, 163, 2021, 0L);
    StudyPlan draftPlan = StudyPlan.reconstitute(
        PlanId.generate(), programId, "Draft Plan", PlanStatus.DRAFT, 100, null, 0L);
    StudyPlan archivedPlan = StudyPlan.reconstitute(
        PlanId.generate(), programId, "Old Plan", PlanStatus.ARCHIVED, 120, 2010, 1L);

    when(loadStudyPlanPort.loadByProgramId(programId))
        .thenReturn(List.of(activePlan, draftPlan, archivedPlan));

    List<StudyPlanView> result = service.listProgramPlans(programId, "STAFF");

    assertThat(result).hasSize(3);
  }

  @Test
  void adminSeesAllStatuses() {
    StudyPlan draftPlan = StudyPlan.reconstitute(
        PlanId.generate(), programId, "Draft Plan", PlanStatus.DRAFT, 100, null, 0L);

    when(loadStudyPlanPort.loadByProgramId(programId)).thenReturn(List.of(draftPlan));

    List<StudyPlanView> result = service.listProgramPlans(programId, "ADMIN");

    assertThat(result).hasSize(1);
  }

  @Test
  void returnsEmptyListWhenNoPlans() {
    when(loadStudyPlanPort.loadByProgramId(programId)).thenReturn(List.of());

    List<StudyPlanView> result = service.listProgramPlans(programId, "STUDENT");

    assertThat(result).isEmpty();
  }

  // FIX-2 (W-1): null / unknown callerRole must behave like STUDENT (see ACTIVE only)

  @Test
  void nullCallerRoleSeesOnlyActivePlans() {
    StudyPlan activePlan = StudyPlan.reconstitute(
        PlanId.generate(), programId, "Plan 21", PlanStatus.ACTIVE, 163, 2021, 0L);
    StudyPlan draftPlan = StudyPlan.reconstitute(
        PlanId.generate(), programId, "Draft Plan", PlanStatus.DRAFT, 100, null, 0L);
    StudyPlan archivedPlan = StudyPlan.reconstitute(
        PlanId.generate(), programId, "Old Plan", PlanStatus.ARCHIVED, 120, 2010, 1L);

    when(loadStudyPlanPort.loadByProgramId(programId))
        .thenReturn(List.of(activePlan, draftPlan, archivedPlan));

    List<StudyPlanView> result = service.listProgramPlans(programId, null);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).status()).isEqualTo("ACTIVE");
  }

  @Test
  void unknownCallerRoleSeesOnlyActivePlans() {
    StudyPlan activePlan = StudyPlan.reconstitute(
        PlanId.generate(), programId, "Plan 21", PlanStatus.ACTIVE, 163, 2021, 0L);
    StudyPlan draftPlan = StudyPlan.reconstitute(
        PlanId.generate(), programId, "Draft Plan", PlanStatus.DRAFT, 100, null, 0L);

    when(loadStudyPlanPort.loadByProgramId(programId))
        .thenReturn(List.of(activePlan, draftPlan));

    List<StudyPlanView> result = service.listProgramPlans(programId, "UNKNOWN");

    assertThat(result).hasSize(1);
    assertThat(result.get(0).status()).isEqualTo("ACTIVE");
  }
}
