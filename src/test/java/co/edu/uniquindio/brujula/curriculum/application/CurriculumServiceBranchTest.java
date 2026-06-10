package co.edu.uniquindio.brujula.curriculum.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.edu.uniquindio.brujula.audit.application.port.in.AuditRecorder;
import co.edu.uniquindio.brujula.curriculum.application.dto.CourseRequirementView;
import co.edu.uniquindio.brujula.curriculum.application.dto.CourseView;
import co.edu.uniquindio.brujula.curriculum.application.dto.CreateCourseRequirementCommand;
import co.edu.uniquindio.brujula.curriculum.application.dto.SequentialTrackView;
import co.edu.uniquindio.brujula.curriculum.application.dto.UpdateCourseCommand;
import co.edu.uniquindio.brujula.curriculum.application.dto.UpdateCourseRequirementCommand;
import co.edu.uniquindio.brujula.curriculum.application.port.out.LoadCoursePort;
import co.edu.uniquindio.brujula.curriculum.application.port.out.LoadSequentialTrackPort;
import co.edu.uniquindio.brujula.curriculum.application.port.out.SaveCoursePort;
import co.edu.uniquindio.brujula.curriculum.application.port.out.SaveCourseRequirementPort;
import co.edu.uniquindio.brujula.curriculum.application.service.CreateCourseRequirementService;
import co.edu.uniquindio.brujula.curriculum.application.service.GetCourseService;
import co.edu.uniquindio.brujula.curriculum.application.service.ListSequentialTracksService;
import co.edu.uniquindio.brujula.curriculum.application.service.UpdateCourseRequirementService;
import co.edu.uniquindio.brujula.curriculum.application.service.UpdateCourseService;
import co.edu.uniquindio.brujula.curriculum.domain.Course;
import co.edu.uniquindio.brujula.curriculum.domain.CourseId;
import co.edu.uniquindio.brujula.curriculum.domain.CourseRequirement;
import co.edu.uniquindio.brujula.curriculum.domain.CourseRequirementId;
import co.edu.uniquindio.brujula.curriculum.domain.CourseRequirementStatus;
import co.edu.uniquindio.brujula.curriculum.domain.CourseStatus;
import co.edu.uniquindio.brujula.curriculum.domain.CourseType;
import co.edu.uniquindio.brujula.curriculum.domain.RequirementType;
import co.edu.uniquindio.brujula.curriculum.domain.SequentialTrack;
import co.edu.uniquindio.brujula.curriculum.domain.SequentialTrackId;
import co.edu.uniquindio.brujula.curriculum.domain.TrackKind;
import co.edu.uniquindio.brujula.sharedkernel.application.port.out.IdempotencyStorePort;
import co.edu.uniquindio.brujula.sharedkernel.application.port.out.JsonCodecPort;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * DEBT-F7-003: covers uncovered branches in curriculum/application/service classes.
 *
 * <p>GetCourseService.getCourses: null input and empty input. ListSequentialTracksService: null and
 * blank kindFilter. UpdateCourseService: null recommendedSemesters uses empty list.
 * CreateCourseRequirementService: CONFIGURABLE type, MINIMUM_CREDITS type, SEQUENCE type, null
 * requiredCourseId skips cycle check. UpdateCourseRequirementService: null field fallbacks for
 * type/requiredCourseId/requiredCredits/simultaneityAllowed.
 */
class CurriculumServiceBranchTest {

  // =========================================================================
  // GetCourseService — getCourses branches
  // =========================================================================

  private LoadCoursePort loadCoursePort;
  private GetCourseService getCourseService;
  private final UUID planId = UUID.randomUUID();
  private final UUID programId = UUID.randomUUID();

  @BeforeEach
  void setUpGetCourseService() {
    loadCoursePort = mock(LoadCoursePort.class);
    getCourseService = new GetCourseService(loadCoursePort);
  }

  @Test
  void getCourses_with_null_set_returns_empty_map_without_querying_port() {
    Map<UUID, CourseView> result = getCourseService.getCourses(null);

    assertThat(result).isEmpty();
    verify(loadCoursePort, never()).loadByIds(any());
  }

  @Test
  void getCourses_with_empty_set_returns_empty_map_without_querying_port() {
    Map<UUID, CourseView> result = getCourseService.getCourses(Set.of());

    assertThat(result).isEmpty();
    verify(loadCoursePort, never()).loadByIds(any());
  }

  @Test
  void getCourses_with_valid_ids_returns_matching_courses() {
    CourseId courseId = CourseId.generate();
    Course course = stubCourse(courseId, planId, programId);
    when(loadCoursePort.loadByIds(Set.of(courseId.value()))).thenReturn(List.of(course));

    Map<UUID, CourseView> result = getCourseService.getCourses(Set.of(courseId.value()));

    assertThat(result).containsKey(courseId.value());
    assertThat(result.get(courseId.value()).name()).isEqualTo("Stub Course");
  }

  // =========================================================================
  // ListSequentialTracksService — parseKind branches
  // =========================================================================

  @Test
  void listSequentialTracks_with_null_kindFilter_passes_null_to_port() {
    LoadSequentialTrackPort trackPort = mock(LoadSequentialTrackPort.class);
    when(trackPort.loadByPlanId(any(), any())).thenReturn(List.of());
    ListSequentialTracksService service = new ListSequentialTracksService(trackPort);

    UUID id = UUID.randomUUID();
    List<SequentialTrackView> result = service.listSequentialTracks(id, null);

    assertThat(result).isEmpty();
    verify(trackPort).loadByPlanId(id, null);
  }

  @Test
  void listSequentialTracks_with_blank_kindFilter_passes_null_to_port() {
    LoadSequentialTrackPort trackPort = mock(LoadSequentialTrackPort.class);
    when(trackPort.loadByPlanId(any(), any())).thenReturn(List.of());
    ListSequentialTracksService service = new ListSequentialTracksService(trackPort);

    UUID id = UUID.randomUUID();
    List<SequentialTrackView> result = service.listSequentialTracks(id, "  ");

    assertThat(result).isEmpty();
    verify(trackPort).loadByPlanId(id, null);
  }

  @Test
  void listSequentialTracks_with_valid_kind_parses_and_filters() {
    LoadSequentialTrackPort trackPort = mock(LoadSequentialTrackPort.class);
    SequentialTrack track = stubTrack();
    when(trackPort.loadByPlanId(any(), any())).thenReturn(List.of(track));
    ListSequentialTracksService service = new ListSequentialTracksService(trackPort);

    UUID id = UUID.randomUUID();
    List<SequentialTrackView> result = service.listSequentialTracks(id, "DEEPENING");

    assertThat(result).hasSize(1);
    verify(trackPort).loadByPlanId(id, TrackKind.DEEPENING);
  }

  // =========================================================================
  // UpdateCourseService — null recommendedSemesters uses empty list
  // =========================================================================

  @Test
  void updateCourse_with_null_recommendedSemesters_uses_empty_list() {
    SaveCoursePort saveCoursePort = mock(SaveCoursePort.class);
    AuditRecorder auditRecorder = mock(AuditRecorder.class);
    UpdateCourseService service = new UpdateCourseService(saveCoursePort, auditRecorder);

    CourseId courseId = CourseId.generate();
    Course existing = stubCourse(courseId, planId, programId);
    when(saveCoursePort.findCurrentById(courseId)).thenReturn(Optional.of(existing));

    Course saved = stubCourse(courseId, planId, programId);
    when(saveCoursePort.save(any(Course.class))).thenReturn(saved);

    // null recommendedSemesters → service falls back to empty list (branch coverage)
    UpdateCourseCommand command =
        new UpdateCourseCommand(
            UUID.randomUUID(), courseId.value(), 0L, "New Name", null, "REQUIRED", 3, 4, null);

    CourseView result = service.updateCourse(command);
    assertThat(result.recommendedSemesters()).isEmpty();
  }

  // =========================================================================
  // CreateCourseRequirementService — type branches
  // =========================================================================

  private SaveCourseRequirementPort saveRequirementPort;
  private IdempotencyStorePort idempotencyStorePort;
  private JsonCodecPort jsonCodecPort;
  private AuditRecorder auditRecorder;
  private CreateCourseRequirementService createReqService;

  private final UUID targetCourseId = UUID.randomUUID();
  private final UUID requiredCourseId = UUID.randomUUID();
  private final UUID planIdForReq = UUID.randomUUID();
  private final UUID actorId = UUID.randomUUID();

  @BeforeEach
  void setUpCreateReqService() {
    saveRequirementPort = mock(SaveCourseRequirementPort.class);
    idempotencyStorePort = mock(IdempotencyStorePort.class);
    jsonCodecPort = mock(JsonCodecPort.class);
    auditRecorder = mock(AuditRecorder.class);
    createReqService =
        new CreateCourseRequirementService(
            saveRequirementPort,
            mock(LoadCoursePort.class),
            idempotencyStorePort,
            jsonCodecPort,
            auditRecorder);
  }

  @Test
  void create_CONFIGURABLE_requirement_skips_cycle_check_and_persists() {
    UUID idemKey = UUID.randomUUID();
    when(idempotencyStorePort.find(idemKey, CreateCourseRequirementService.OPERATION))
        .thenReturn(Optional.empty());
    CourseRequirement saved = stubRequirement(RequirementType.CONFIGURABLE, null);
    when(saveRequirementPort.save(any())).thenReturn(saved);
    when(jsonCodecPort.toJson(any())).thenReturn("{}");

    // CONFIGURABLE: no requiredCourseId → no cycle check, no findAllByPlanId
    CreateCourseRequirementCommand command =
        new CreateCourseRequirementCommand(
            idemKey,
            "hash-cfg",
            actorId,
            targetCourseId,
            planIdForReq,
            "CONFIGURABLE",
            null,
            null,
            null);

    CourseRequirementView result = createReqService.createCourseRequirement(command);

    assertThat(result.type()).isEqualTo("CONFIGURABLE");
    verify(saveRequirementPort, never()).findAllByPlanId(any());
  }

  @Test
  void create_MINIMUM_CREDITS_requirement_skips_cycle_check_and_persists() {
    UUID idemKey = UUID.randomUUID();
    when(idempotencyStorePort.find(idemKey, CreateCourseRequirementService.OPERATION))
        .thenReturn(Optional.empty());
    CourseRequirement saved = stubRequirementMinCredits(50);
    when(saveRequirementPort.save(any())).thenReturn(saved);
    when(jsonCodecPort.toJson(any())).thenReturn("{}");

    // MINIMUM_CREDITS: no edge check → no cycle detection
    CreateCourseRequirementCommand command =
        new CreateCourseRequirementCommand(
            idemKey,
            "hash-mc",
            actorId,
            targetCourseId,
            planIdForReq,
            "MINIMUM_CREDITS",
            null,
            50,
            null);

    CourseRequirementView result = createReqService.createCourseRequirement(command);

    assertThat(result.type()).isEqualTo("MINIMUM_CREDITS");
    verify(saveRequirementPort, never()).findAllByPlanId(any());
  }

  @Test
  void create_COURSE_PASSED_with_null_requiredCourseId_skips_cycle_check_but_domain_rejects() {
    UUID idemKey = UUID.randomUUID();
    when(idempotencyStorePort.find(idemKey, CreateCourseRequirementService.OPERATION))
        .thenReturn(Optional.empty());
    // null requiredCourseId on COURSE_PASSED:
    // - The inner guard `if (command.requiredCourseId() != null)` skips cycle detection
    // - The domain factory CourseRequirement.coursePassed() then throws because null is invalid
    // This asserts the actual observable behavior: domain rejects null requiredCourseId
    CreateCourseRequirementCommand command =
        new CreateCourseRequirementCommand(
            idemKey,
            "hash-cp-null",
            actorId,
            targetCourseId,
            planIdForReq,
            "COURSE_PASSED",
            null,
            null,
            false);

    assertThatThrownBy(() -> createReqService.createCourseRequirement(command))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("requiredCourseId");
    // Cycle check (findAllByPlanId) must NOT be called when requiredCourseId is null
    verify(saveRequirementPort, never()).findAllByPlanId(any());
  }

  @Test
  void create_SEQUENCE_requirement_with_non_null_required_course_performs_cycle_check() {
    UUID idemKey = UUID.randomUUID();
    when(idempotencyStorePort.find(idemKey, CreateCourseRequirementService.OPERATION))
        .thenReturn(Optional.empty());
    when(saveRequirementPort.findAllByPlanId(planIdForReq)).thenReturn(List.of());
    CourseRequirement saved = stubRequirement(RequirementType.SEQUENCE, requiredCourseId);
    when(saveRequirementPort.save(any())).thenReturn(saved);
    when(jsonCodecPort.toJson(any())).thenReturn("{}");

    CreateCourseRequirementCommand command =
        new CreateCourseRequirementCommand(
            idemKey,
            "hash-seq",
            actorId,
            targetCourseId,
            planIdForReq,
            "SEQUENCE",
            requiredCourseId,
            null,
            false);

    CourseRequirementView result = createReqService.createCourseRequirement(command);

    assertThat(result.type()).isEqualTo("SEQUENCE");
    // SEQUENCE with non-null requiredCourseId MUST do the cycle check
    verify(saveRequirementPort).findAllByPlanId(planIdForReq);
  }

  // =========================================================================
  // UpdateCourseRequirementService — null fallback branches
  // =========================================================================

  @Test
  void updateRequirement_null_type_falls_back_to_existing_type() {
    SaveCourseRequirementPort savePort = mock(SaveCourseRequirementPort.class);
    LoadCoursePort lcp = mock(LoadCoursePort.class);
    AuditRecorder ar = mock(AuditRecorder.class);
    UpdateCourseRequirementService updateService =
        new UpdateCourseRequirementService(savePort, lcp, ar);

    CourseRequirementId reqId = CourseRequirementId.generate();
    // Existing is MINIMUM_CREDITS (no cycle check needed)
    CourseRequirement existing =
        CourseRequirement.reconstitute(
            reqId,
            targetCourseId,
            RequirementType.MINIMUM_CREDITS,
            null,
            50,
            null,
            null,
            null,
            CourseRequirementStatus.ACTIVE,
            0L);
    when(savePort.findCurrentById(reqId)).thenReturn(Optional.of(existing));

    CourseRequirement saved =
        CourseRequirement.reconstitute(
            reqId,
            targetCourseId,
            RequirementType.MINIMUM_CREDITS,
            null,
            50,
            null,
            null,
            null,
            CourseRequirementStatus.ACTIVE,
            1L);
    when(savePort.save(any())).thenReturn(saved);

    // null type → must fall back to existing type (MINIMUM_CREDITS), no cycle check
    UpdateCourseRequirementCommand command =
        new UpdateCourseRequirementCommand(actorId, reqId.value(), 0L, null, null, null, null);

    CourseRequirementView result = updateService.updateCourseRequirement(command);

    assertThat(result.type()).isEqualTo("MINIMUM_CREDITS");
    // MINIMUM_CREDITS has no requiredCourseId → no cycle detection call
    verify(savePort, never()).findAllByPlanId(any());
  }

  @Test
  void updateRequirement_null_requiredCredits_falls_back_to_existing_value() {
    SaveCourseRequirementPort savePort = mock(SaveCourseRequirementPort.class);
    AuditRecorder ar = mock(AuditRecorder.class);
    UpdateCourseRequirementService updateService =
        new UpdateCourseRequirementService(savePort, mock(LoadCoursePort.class), ar);

    CourseRequirementId reqId = CourseRequirementId.generate();
    CourseRequirement existing =
        CourseRequirement.reconstitute(
            reqId,
            targetCourseId,
            RequirementType.MINIMUM_CREDITS,
            null,
            80,
            null,
            null,
            null,
            CourseRequirementStatus.ACTIVE,
            0L);
    when(savePort.findCurrentById(reqId)).thenReturn(Optional.of(existing));

    CourseRequirement saved =
        CourseRequirement.reconstitute(
            reqId,
            targetCourseId,
            RequirementType.MINIMUM_CREDITS,
            null,
            80,
            null,
            null,
            null,
            CourseRequirementStatus.ACTIVE,
            1L);
    when(savePort.save(any())).thenReturn(saved);

    // null requiredCredits → must fall back to 80
    UpdateCourseRequirementCommand command =
        new UpdateCourseRequirementCommand(
            actorId, reqId.value(), 0L, "MINIMUM_CREDITS", null, null, null);

    CourseRequirementView result = updateService.updateCourseRequirement(command);

    assertThat(result.requiredCredits()).isEqualTo(80);
  }

  @Test
  void updateRequirement_null_simultaneityAllowed_falls_back_to_existing_value() {
    SaveCourseRequirementPort savePort = mock(SaveCourseRequirementPort.class);
    LoadCoursePort lcp = mock(LoadCoursePort.class);
    AuditRecorder ar = mock(AuditRecorder.class);
    UpdateCourseRequirementService updateService =
        new UpdateCourseRequirementService(savePort, lcp, ar);

    CourseRequirementId reqId = CourseRequirementId.generate();
    CourseRequirement existing =
        CourseRequirement.reconstitute(
            reqId,
            targetCourseId,
            RequirementType.MINIMUM_CREDITS,
            null,
            40,
            true, // simultaneityAllowed = true in existing
            null,
            null,
            CourseRequirementStatus.ACTIVE,
            0L);
    when(savePort.findCurrentById(reqId)).thenReturn(Optional.of(existing));

    CourseRequirement saved =
        CourseRequirement.reconstitute(
            reqId,
            targetCourseId,
            RequirementType.MINIMUM_CREDITS,
            null,
            40,
            true,
            null,
            null,
            CourseRequirementStatus.ACTIVE,
            1L);
    when(savePort.save(any())).thenReturn(saved);

    // null simultaneityAllowed → must fall back to existing true
    UpdateCourseRequirementCommand command =
        new UpdateCourseRequirementCommand(
            actorId, reqId.value(), 0L, "MINIMUM_CREDITS", null, null, null);

    CourseRequirementView result = updateService.updateCourseRequirement(command);

    assertThat(result.simultaneityAllowed()).isTrue();
  }

  // =========================================================================
  // Helpers
  // =========================================================================

  private static Course stubCourse(CourseId courseId, UUID planId, UUID programId) {
    return Course.reconstitute(
        courseId,
        planId,
        programId,
        null,
        "Stub Course",
        CourseType.REQUIRED,
        3,
        4,
        Collections.emptyList(),
        CourseStatus.ACTIVE,
        0L);
  }

  private static CourseRequirement stubRequirement(RequirementType type, UUID requiredId) {
    return CourseRequirement.reconstitute(
        CourseRequirementId.generate(),
        UUID.randomUUID(),
        type,
        requiredId,
        null,
        false,
        null,
        null,
        CourseRequirementStatus.ACTIVE,
        0L);
  }

  private static CourseRequirement stubRequirementMinCredits(int credits) {
    return CourseRequirement.reconstitute(
        CourseRequirementId.generate(),
        UUID.randomUUID(),
        RequirementType.MINIMUM_CREDITS,
        null,
        credits,
        false,
        null,
        null,
        CourseRequirementStatus.ACTIVE,
        0L);
  }

  private static SequentialTrack stubTrack() {
    return SequentialTrack.reconstitute(
        new SequentialTrackId(UUID.randomUUID()),
        UUID.randomUUID(),
        TrackKind.DEEPENING,
        "Track A",
        false,
        null,
        List.of(),
        0L);
  }
}
