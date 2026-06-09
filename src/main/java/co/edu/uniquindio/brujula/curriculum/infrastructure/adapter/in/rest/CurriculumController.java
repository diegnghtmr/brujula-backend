package co.edu.uniquindio.brujula.curriculum.infrastructure.adapter.in.rest;

import co.edu.uniquindio.brujula.curriculum.application.dto.ArchiveCourseCommand;
import co.edu.uniquindio.brujula.curriculum.application.dto.CourseRequirementView;
import co.edu.uniquindio.brujula.curriculum.application.dto.CourseView;
import co.edu.uniquindio.brujula.curriculum.application.dto.CreateCourseCommand;
import co.edu.uniquindio.brujula.curriculum.application.dto.CreateCourseRequirementCommand;
import co.edu.uniquindio.brujula.curriculum.application.dto.DegreeWorkModalityView;
import co.edu.uniquindio.brujula.curriculum.application.dto.PlanMallaView;
import co.edu.uniquindio.brujula.curriculum.application.dto.SequentialTrackView;
import co.edu.uniquindio.brujula.curriculum.application.dto.UpdateCourseCommand;
import co.edu.uniquindio.brujula.curriculum.application.dto.UpdateCourseRequirementCommand;
import co.edu.uniquindio.brujula.curriculum.application.port.in.ArchiveCourseUseCase;
import co.edu.uniquindio.brujula.curriculum.application.port.in.AssemblePlanMallaQuery;
import co.edu.uniquindio.brujula.curriculum.application.port.in.CreateCourseRequirementUseCase;
import co.edu.uniquindio.brujula.curriculum.application.port.in.CreateCourseUseCase;
import co.edu.uniquindio.brujula.curriculum.application.port.in.GetCourseQuery;
import co.edu.uniquindio.brujula.curriculum.application.port.in.ListCourseRequirementsQuery;
import co.edu.uniquindio.brujula.curriculum.application.port.in.ListDegreeWorkModalitiesQuery;
import co.edu.uniquindio.brujula.curriculum.application.port.in.ListPlanCoursesQuery;
import co.edu.uniquindio.brujula.curriculum.application.port.in.ListSequentialTracksQuery;
import co.edu.uniquindio.brujula.curriculum.application.port.in.UpdateCourseRequirementUseCase;
import co.edu.uniquindio.brujula.curriculum.application.port.in.UpdateCourseUseCase;
import co.edu.uniquindio.brujula.identity.application.port.in.GetCurrentUserQuery;
import co.edu.uniquindio.brujula.programcatalog.application.port.in.GetStudyPlanQuery;
import co.edu.uniquindio.brujula.generated.api.CurriculumApi;
import co.edu.uniquindio.brujula.generated.model.ChangeStudyPlanStatusRequest;
import co.edu.uniquindio.brujula.generated.model.Course;
import co.edu.uniquindio.brujula.generated.model.CourseCreateRequest;
import co.edu.uniquindio.brujula.generated.model.CoursePage;
import co.edu.uniquindio.brujula.generated.model.CourseRequirement;
import co.edu.uniquindio.brujula.generated.model.CourseRequirementCreateRequest;
import co.edu.uniquindio.brujula.generated.model.CourseRequirementUpdateRequest;
import co.edu.uniquindio.brujula.generated.model.CourseStatus;
import co.edu.uniquindio.brujula.generated.model.CourseType;
import co.edu.uniquindio.brujula.generated.model.CourseUpdateRequest;
import co.edu.uniquindio.brujula.generated.model.DegreeWorkModality;
import co.edu.uniquindio.brujula.generated.model.DegreeWorkModalityCreateRequest;
import co.edu.uniquindio.brujula.generated.model.DegreeWorkModalityUpdateRequest;
import co.edu.uniquindio.brujula.generated.model.MallaCourse;
import co.edu.uniquindio.brujula.generated.model.MallaRequirement;
import co.edu.uniquindio.brujula.generated.model.PlanMalla;
import co.edu.uniquindio.brujula.generated.model.PlanMallaSemesterBucketsInner;
import co.edu.uniquindio.brujula.generated.model.RecommendedSemester;
import co.edu.uniquindio.brujula.generated.model.RequirementType;
import co.edu.uniquindio.brujula.generated.model.SequentialTrack;
import co.edu.uniquindio.brujula.generated.model.SequentialTrackCreateRequest;
import co.edu.uniquindio.brujula.generated.model.SequentialTrackKind;
import co.edu.uniquindio.brujula.generated.model.SequentialTrackLevel;
import co.edu.uniquindio.brujula.generated.model.SequentialTrackUpdateRequest;
import co.edu.uniquindio.brujula.generated.model.StudyPlan;
import co.edu.uniquindio.brujula.generated.model.StudyPlanCreateRequest;
import co.edu.uniquindio.brujula.generated.model.StudyPlanUpdateRequest;
import co.edu.uniquindio.brujula.generated.model.VersionedAction;
import java.net.URI;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.ObjectMapper;

/**
 * REST adapter implementing the generated {@link CurriculumApi} interface (contract-first, ARQ-001).
 *
 * <p>Implements read-only curriculum endpoints for Phase 4+5 WU4:
 * <ul>
 *   <li>GET /courses/{courseId} — getCourse (CUR-001)
 *   <li>GET /courses/{courseId}/requirements — listCourseRequirements (CUR-003)
 *   <li>GET /plans/{planId}/courses — listPlanCourses (CUR-001)
 *   <li>GET /plans/{planId}/sequential-tracks — listSequentialTracks (CUR-005)
 *   <li>GET /plans/{planId}/degree-work-modalities — listDegreeWorkModalities (CUR-006)
 *   <li>GET /plans/{planId}/malla — getPlanMalla (ADR-F2-001)
 * </ul>
 *
 * <p>Staff write endpoints (createCourse, updateCourse, archiveCourse, createCourseRequirement,
 * updateCourseRequirement, deleteCourseRequirement, createStudyPlan, changeStudyPlanStatus,
 * createSequentialTrack, updateSequentialTrack, createDegreeWorkModality, updateDegreeWorkModality,
 * updateStudyPlan) return HTTP 501 NOT_IMPLEMENTED (out of scope for Fase 2 WU4).
 *
 * <p>ARQ-001: no domain types imported — only application DTOs and generated model types.
 * Package-private per ARQ-006.
 */
@RestController
class CurriculumController implements CurriculumApi {

  private final GetCourseQuery getCourseQuery;
  private final ListPlanCoursesQuery listPlanCoursesQuery;
  private final ListCourseRequirementsQuery listCourseRequirementsQuery;
  private final ListSequentialTracksQuery listSequentialTracksQuery;
  private final ListDegreeWorkModalitiesQuery listDegreeWorkModalitiesQuery;
  private final AssemblePlanMallaQuery assemblePlanMallaQuery;
  // Write use cases (WU5)
  private final CreateCourseUseCase createCourseUseCase;
  private final UpdateCourseUseCase updateCourseUseCase;
  private final ArchiveCourseUseCase archiveCourseUseCase;
  private final CreateCourseRequirementUseCase createCourseRequirementUseCase;
  private final UpdateCourseRequirementUseCase updateCourseRequirementUseCase;
  private final GetCurrentUserQuery getCurrentUserQuery;
  private final GetStudyPlanQuery getStudyPlanQuery;
  private final ObjectMapper objectMapper;

  CurriculumController(
      GetCourseQuery getCourseQuery,
      ListPlanCoursesQuery listPlanCoursesQuery,
      ListCourseRequirementsQuery listCourseRequirementsQuery,
      ListSequentialTracksQuery listSequentialTracksQuery,
      ListDegreeWorkModalitiesQuery listDegreeWorkModalitiesQuery,
      AssemblePlanMallaQuery assemblePlanMallaQuery,
      CreateCourseUseCase createCourseUseCase,
      UpdateCourseUseCase updateCourseUseCase,
      ArchiveCourseUseCase archiveCourseUseCase,
      CreateCourseRequirementUseCase createCourseRequirementUseCase,
      UpdateCourseRequirementUseCase updateCourseRequirementUseCase,
      GetCurrentUserQuery getCurrentUserQuery,
      GetStudyPlanQuery getStudyPlanQuery,
      ObjectMapper objectMapper) {
    this.getCourseQuery = getCourseQuery;
    this.listPlanCoursesQuery = listPlanCoursesQuery;
    this.listCourseRequirementsQuery = listCourseRequirementsQuery;
    this.listSequentialTracksQuery = listSequentialTracksQuery;
    this.listDegreeWorkModalitiesQuery = listDegreeWorkModalitiesQuery;
    this.assemblePlanMallaQuery = assemblePlanMallaQuery;
    this.createCourseUseCase = createCourseUseCase;
    this.updateCourseUseCase = updateCourseUseCase;
    this.archiveCourseUseCase = archiveCourseUseCase;
    this.createCourseRequirementUseCase = createCourseRequirementUseCase;
    this.updateCourseRequirementUseCase = updateCourseRequirementUseCase;
    this.getCurrentUserQuery = getCurrentUserQuery;
    this.getStudyPlanQuery = getStudyPlanQuery;
    this.objectMapper = objectMapper;
  }

  // ---------------------------------------------------------------------------
  // Read endpoints (implemented)
  // ---------------------------------------------------------------------------

  /** CUR-001: get a single course by ID. */
  @Override
  public ResponseEntity<Course> getCourse(UUID courseId) {
    CourseView view = getCourseQuery.getCourse(courseId);
    return ResponseEntity.ok(toCourseDto(view));
  }

  /** CUR-001: list all courses for a plan (with role-based visibility). */
  @Override
  public ResponseEntity<CoursePage> listPlanCourses(
      UUID planId,
      @Nullable CourseType type,
      @Nullable CourseStatus status,
      Integer page,
      Integer size,
      @Nullable List<String> sort) {
    String callerRole = resolveCallerRole();
    List<CourseView> views = listPlanCoursesQuery.listPlanCourses(planId, callerRole);
    // Apply type/status filters at controller level if provided
    List<CourseView> filtered = views.stream()
        .filter(v -> type == null || v.type().equals(type.getValue()))
        .filter(v -> status == null || v.status().equals(status.getValue()))
        .toList();
    int total = filtered.size();
    int from = Math.min(page * size, total);
    int to = Math.min(from + size, total);
    List<CourseView> paged = filtered.subList(from, to);
    CoursePage coursePage = buildCoursePage(paged, total, page, size);
    return ResponseEntity.ok(coursePage);
  }

  /** CUR-003: list course requirements for a course. */
  @Override
  public ResponseEntity<List<CourseRequirement>> listCourseRequirements(UUID courseId) {
    List<CourseRequirementView> views = listCourseRequirementsQuery.listCourseRequirements(courseId);
    List<CourseRequirement> result = views.stream().map(this::toCourseRequirementDto).toList();
    return ResponseEntity.ok(result);
  }

  /** CUR-005: list sequential tracks for a plan, optionally filtered by kind. */
  @Override
  public ResponseEntity<List<SequentialTrack>> listSequentialTracks(
      UUID planId, @Nullable SequentialTrackKind kind) {
    String kindFilter = kind != null ? kind.getValue() : null;
    List<SequentialTrackView> views = listSequentialTracksQuery.listSequentialTracks(planId, kindFilter);
    List<SequentialTrack> result = views.stream().map(this::toSequentialTrackDto).toList();
    return ResponseEntity.ok(result);
  }

  /** CUR-006: list degree work modalities for a plan. */
  @Override
  public ResponseEntity<List<DegreeWorkModality>> listDegreeWorkModalities(UUID planId) {
    List<DegreeWorkModalityView> views = listDegreeWorkModalitiesQuery.listDegreeWorkModalities(planId);
    List<DegreeWorkModality> result = views.stream().map(this::toDegreeWorkModalityDto).toList();
    return ResponseEntity.ok(result);
  }

  /** ADR-F2-001: assemble the plan malla (curricular grid) for a given journey. */
  @Override
  public ResponseEntity<PlanMalla> getPlanMalla(String journeyCode, UUID planId) {
    PlanMallaView view = assemblePlanMallaQuery.assemblePlanMalla(planId, journeyCode);
    return ResponseEntity.ok(toPlanMallaDto(view));
  }

  // ---------------------------------------------------------------------------
  // Write endpoints (WU5 — implemented)
  // ---------------------------------------------------------------------------

  /** CUR-001: create a new course (idempotent). STAFF/ADMIN only. */
  @Override
  @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
  public ResponseEntity<Course> createCourse(
      UUID idempotencyKey,
      String xXsrfToken,
      UUID planId,
      CourseCreateRequest courseCreateRequest) {
    UUID actorId = resolveActorId();
    String requestHash = hashRequest(courseCreateRequest);
    List<CourseView.RecommendedSemesterView> semesters =
        courseCreateRequest.getRecommendedSemesters() == null ? List.of()
            : courseCreateRequest.getRecommendedSemesters().stream()
                .map(rs -> new CourseView.RecommendedSemesterView(rs.getJourneyCode(), rs.getSemester()))
                .toList();
    // programId is not in CourseCreateRequest — resolve from plan (CAT-002 GetStudyPlanQuery).
    UUID programId = getStudyPlanQuery.getStudyPlan(planId, resolveCallerRole()).programId();
    CreateCourseCommand command = new CreateCourseCommand(
        idempotencyKey,
        requestHash,
        actorId,
        planId,
        programId,
        courseCreateRequest.getName(),
        courseCreateRequest.getOfficialCode(),
        courseCreateRequest.getType() != null ? courseCreateRequest.getType().getValue() : "REQUIRED",
        courseCreateRequest.getCredits() != null ? courseCreateRequest.getCredits().intValue() : 0,
        courseCreateRequest.getCurricularWeeklyHours() != null
            ? courseCreateRequest.getCurricularWeeklyHours().intValue() : 0,
        semesters);
    CourseView view = createCourseUseCase.createCourse(command);
    URI location = URI.create("/courses/" + view.id());
    return ResponseEntity.status(HttpStatus.CREATED).location(location).body(toCourseDto(view));
  }

  /** CUR-002: update a course's mutable fields (optimistic lock). STAFF/ADMIN only. */
  @Override
  @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
  public ResponseEntity<Course> updateCourse(
      String xXsrfToken,
      UUID courseId,
      CourseUpdateRequest courseUpdateRequest) {
    UUID actorId = resolveActorId();
    List<CourseView.RecommendedSemesterView> semesters =
        courseUpdateRequest.getRecommendedSemesters() == null ? List.of()
            : courseUpdateRequest.getRecommendedSemesters().stream()
                .map(rs -> new CourseView.RecommendedSemesterView(rs.getJourneyCode(), rs.getSemester()))
                .toList();
    UpdateCourseCommand command = new UpdateCourseCommand(
        actorId,
        courseId,
        courseUpdateRequest.getVersion() != null ? courseUpdateRequest.getVersion() : 0L,
        courseUpdateRequest.getName(),
        courseUpdateRequest.getOfficialCode(),
        courseUpdateRequest.getType() != null ? courseUpdateRequest.getType().getValue() : null,
        courseUpdateRequest.getCredits() != null ? courseUpdateRequest.getCredits().intValue() : 0,
        courseUpdateRequest.getCurricularWeeklyHours() != null
            ? courseUpdateRequest.getCurricularWeeklyHours().intValue() : 0,
        semesters);
    CourseView view = updateCourseUseCase.updateCourse(command);
    return ResponseEntity.ok(toCourseDto(view));
  }

  /** CUR-002: archive (soft-delete) a course (optimistic lock). STAFF/ADMIN only. */
  @Override
  @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
  public ResponseEntity<Course> archiveCourse(
      String xXsrfToken,
      UUID courseId,
      VersionedAction versionedAction) {
    UUID actorId = resolveActorId();
    ArchiveCourseCommand command = new ArchiveCourseCommand(
        actorId,
        courseId,
        versionedAction.getVersion() != null ? versionedAction.getVersion() : 0L);
    CourseView view = archiveCourseUseCase.archiveCourse(command);
    return ResponseEntity.ok(toCourseDto(view));
  }

  /** CUR-003: create a course requirement (idempotent, cycle-detection). STAFF/ADMIN only. */
  @Override
  @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
  public ResponseEntity<CourseRequirement> createCourseRequirement(
      UUID idempotencyKey,
      String xXsrfToken,
      UUID courseId,
      CourseRequirementCreateRequest courseRequirementCreateRequest) {
    UUID actorId = resolveActorId();
    String requestHash = hashRequest(courseRequirementCreateRequest);
    // planId needed for cycle detection — load via course's plan; for now pass courseId as sentinel
    // (controller callers must include planId in the request body per ADR, but contract only has courseId path)
    // The service needs planId to build the graph; since the contract only has courseId, we load the
    // course to get planId — but that requires an additional query. For contract compliance, use
    // a workaround: store planId in the command as null and let the service skip cycle detection
    // when planId is null (unsafe). BETTER: use a planId from the request body if available.
    // Per the generated API, courseId is the targetCourseId. planId must be resolved from course.
    // We resolve it inline here from the GetCourseQuery (available in this controller).
    CourseView courseView = getCourseQuery.getCourse(courseId);
    UUID planId = courseView.planId();
    CreateCourseRequirementCommand command = new CreateCourseRequirementCommand(
        idempotencyKey,
        requestHash,
        actorId,
        courseId,
        planId,
        courseRequirementCreateRequest.getType() != null
            ? courseRequirementCreateRequest.getType().getValue() : "COURSE_PASSED",
        courseRequirementCreateRequest.getRequiredCourseId(),
        courseRequirementCreateRequest.getRequiredCredits(),
        courseRequirementCreateRequest.getSimultaneityAllowed());
    CourseRequirementView view = createCourseRequirementUseCase.createCourseRequirement(command);
    URI location = URI.create("/courses/" + courseId + "/requirements/" + view.id());
    return ResponseEntity.status(HttpStatus.CREATED).location(location)
        .body(toCourseRequirementDto(view));
  }

  /** CUR-003: update a course requirement (optimistic lock). STAFF/ADMIN only. */
  @Override
  @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
  public ResponseEntity<CourseRequirement> updateCourseRequirement(
      String xXsrfToken,
      UUID requirementId,
      CourseRequirementUpdateRequest courseRequirementUpdateRequest) {
    UUID actorId = resolveActorId();
    UpdateCourseRequirementCommand command = new UpdateCourseRequirementCommand(
        actorId,
        requirementId,
        courseRequirementUpdateRequest.getVersion() != null
            ? courseRequirementUpdateRequest.getVersion() : 0L,
        courseRequirementUpdateRequest.getType() != null
            ? courseRequirementUpdateRequest.getType().getValue() : null,
        courseRequirementUpdateRequest.getRequiredCourseId(),
        courseRequirementUpdateRequest.getRequiredCredits(),
        courseRequirementUpdateRequest.getSimultaneityAllowed());
    CourseRequirementView view = updateCourseRequirementUseCase.updateCourseRequirement(command);
    return ResponseEntity.ok(toCourseRequirementDto(view));
  }

  // ---------------------------------------------------------------------------
  // Write endpoints — still NOT IMPLEMENTED (return 501)
  // ---------------------------------------------------------------------------

  @Override
  public ResponseEntity<StudyPlan> changeStudyPlanStatus(UUID idempotencyKey, String xXsrfToken, UUID planId, ChangeStudyPlanStatusRequest changeStudyPlanStatusRequest) {
    return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
  }

  @Override
  public ResponseEntity<DegreeWorkModality> createDegreeWorkModality(UUID idempotencyKey, String xXsrfToken, UUID planId, DegreeWorkModalityCreateRequest degreeWorkModalityCreateRequest) {
    return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
  }

  @Override
  public ResponseEntity<SequentialTrack> createSequentialTrack(UUID idempotencyKey, String xXsrfToken, UUID planId, SequentialTrackCreateRequest sequentialTrackCreateRequest) {
    return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
  }

  @Override
  public ResponseEntity<StudyPlan> createStudyPlan(UUID idempotencyKey, String xXsrfToken, UUID programId, StudyPlanCreateRequest studyPlanCreateRequest) {
    return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
  }

  @Override
  public ResponseEntity<Void> deleteCourseRequirement(String xXsrfToken, UUID requirementId) {
    return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
  }

  @Override
  public ResponseEntity<DegreeWorkModality> updateDegreeWorkModality(String xXsrfToken, UUID modalityId, DegreeWorkModalityUpdateRequest degreeWorkModalityUpdateRequest) {
    return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
  }

  @Override
  public ResponseEntity<SequentialTrack> updateSequentialTrack(String xXsrfToken, UUID trackId, SequentialTrackUpdateRequest sequentialTrackUpdateRequest) {
    return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
  }

  @Override
  public ResponseEntity<StudyPlan> updateStudyPlan(String xXsrfToken, UUID planId, StudyPlanUpdateRequest studyPlanUpdateRequest) {
    return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
  }

  // ---------------------------------------------------------------------------
  // DTO mappers: application DTOs → generated model (ARQ-005)
  // ---------------------------------------------------------------------------

  private Course toCourseDto(CourseView view) {
    Course course = new Course();
    course.setId(view.id());
    course.setPlanId(view.planId());
    course.setProgramId(view.programId());
    course.setOfficialCode(view.officialCode());
    course.setName(view.name());
    course.setType(CourseType.fromValue(view.type()));
    course.setCredits((double) view.credits());
    course.setCurricularWeeklyHours((double) view.curricularWeeklyHours());
    course.setStatus(CourseStatus.fromValue(view.status()));
    course.setVersion(view.version());
    course.setRecommendedSemesters(
        view.recommendedSemesters().stream()
            .map(rs -> new RecommendedSemester().journeyCode(rs.journeyCode()).semester(rs.semester()))
            .toList());
    return course;
  }

  private CoursePage buildCoursePage(List<CourseView> views, int totalElements, int page, int size) {
    CoursePage coursePage = new CoursePage();
    coursePage.setPage(page);
    coursePage.setSize(size);
    coursePage.setTotalElements((long) totalElements);
    coursePage.setTotalPages(size > 0 ? (int) Math.ceil((double) totalElements / size) : 0);
    coursePage.setContent(views.stream().map(this::toCourseDto).toList());
    return coursePage;
  }

  private CourseRequirement toCourseRequirementDto(CourseRequirementView view) {
    CourseRequirement req = new CourseRequirement(
        view.id(),
        view.targetCourseId(),
        RequirementType.fromValue(view.type()),
        CourseStatus.fromValue(view.status()),
        view.version());
    req.setRequiredCourseId(view.requiredCourseId());
    req.setRequiredCredits(view.requiredCredits());
    req.setSimultaneityAllowed(view.simultaneityAllowed());
    req.setConfidence(view.confidence() != null
        ? CourseRequirement.ConfidenceEnum.fromValue(view.confidence())
        : null);
    req.setInferred(view.inferred());
    return req;
  }

  private SequentialTrack toSequentialTrackDto(SequentialTrackView view) {
    SequentialTrack track = new SequentialTrack();
    track.setId(view.id());
    track.setPlanId(view.planId());
    track.setKind(SequentialTrackKind.fromValue(view.kind()));
    track.setLabel(view.label());
    track.setConsecutiveSimultaneityAllowed(view.consecutiveSimultaneityAllowed());
    track.setGatekeeperCourseId(view.gatekeeperCourseId());
    track.setVersion(view.version());
    track.setLevels(
        view.levels().stream()
            .map(l -> new SequentialTrackLevel()
                .level(l.level())
                .optionCourseIds(List.copyOf(l.optionCourseIds())))
            .toList());
    return track;
  }

  private PlanMalla toPlanMallaDto(PlanMallaView view) {
    List<PlanMallaSemesterBucketsInner> buckets = view.semesterBuckets().stream()
        .map(b -> {
          List<MallaCourse> courses = b.courses().stream()
              .map(cs -> {
                List<MallaRequirement> requirements = cs.requirements().stream()
                    .map(this::toMallaRequirementDto)
                    .toList();
                MallaCourse mc = new MallaCourse();
                mc.setId(cs.id());
                mc.setOfficialCode(cs.officialCode());
                mc.setName(cs.name());
                mc.setType(CourseType.fromValue(cs.type()));
                mc.setCredits((double) cs.credits());
                mc.setRequirements(requirements);
                return mc;
              })
              .toList();
          PlanMallaSemesterBucketsInner bucket = new PlanMallaSemesterBucketsInner();
          bucket.setRecommendedSemester(b.recommendedSemester());
          bucket.setCourses(courses);
          return bucket;
        })
        .toList();
    PlanMalla planMalla = new PlanMalla();
    planMalla.setPlanId(view.planId());
    planMalla.setJourneyCode(view.journeyCode());
    planMalla.setSemesterBuckets(buckets);
    planMalla.setGraduationRequirements(view.graduationRequirements());
    return planMalla;
  }

  private MallaRequirement toMallaRequirementDto(PlanMallaView.RequirementSummary rs) {
    MallaRequirement mr = new MallaRequirement(RequirementType.fromValue(rs.type()));
    mr.setRequiredCourseId(rs.requiredCourseId());
    mr.setConfidence(rs.confidence() != null
        ? MallaRequirement.ConfidenceEnum.fromValue(rs.confidence())
        : null);
    mr.setInferred(rs.inferred());
    mr.setSimultaneityAllowed(rs.simultaneityAllowed());
    return mr;
  }

  private DegreeWorkModality toDegreeWorkModalityDto(DegreeWorkModalityView view) {
    DegreeWorkModality modality = new DegreeWorkModality();
    modality.setId(view.id());
    modality.setPlanId(view.planId());
    modality.setName(view.name());
    modality.setCredits(view.credits() != null ? view.credits().doubleValue() : null);
    modality.setCurricularWeeklyHours(view.curricularWeeklyHours() != null
        ? view.curricularWeeklyHours().doubleValue()
        : null);
    modality.setStatus(CourseStatus.fromValue(view.status()));
    modality.setVersion(view.version());
    return modality;
  }

  // ---------------------------------------------------------------------------
  // Security helpers
  // ---------------------------------------------------------------------------

  private String resolveCallerRole() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !auth.isAuthenticated()) {
      return null;
    }
    for (GrantedAuthority authority : auth.getAuthorities()) {
      String a = authority.getAuthority();
      if ("ROLE_ADMIN".equals(a)) return "ADMIN";
      if ("ROLE_STAFF".equals(a)) return "STAFF";
    }
    return "STUDENT";
  }

  /** Resolve actor UUID from session principal. BOLA safe: never from request body. */
  private UUID resolveActorId() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || auth.getName() == null) {
      throw new IllegalStateException("No authenticated principal found in SecurityContext");
    }
    return getCurrentUserQuery.getCurrentUserBySubject(auth.getName()).id();
  }

  /** SHA-256 hex digest of the serialized request body for idempotency conflict detection. */
  private String hashRequest(Object request) {
    try {
      byte[] json = objectMapper.writeValueAsBytes(request);
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(json);
      return HexFormat.of().formatHex(hash);
    } catch (Exception e) {
      throw new IllegalStateException("Failed to hash request body", e);
    }
  }
}
