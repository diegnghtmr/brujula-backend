package co.edu.uniquindio.brujula.progress.infrastructure.adapter.in.rest;

import co.edu.uniquindio.brujula.generated.api.ProgressApi;
import co.edu.uniquindio.brujula.generated.model.CourseAttempt;
import co.edu.uniquindio.brujula.generated.model.CourseAttemptCreateRequest;
import co.edu.uniquindio.brujula.generated.model.CourseAttemptPage;
import co.edu.uniquindio.brujula.generated.model.CourseAttemptUpdateRequest;
import co.edu.uniquindio.brujula.generated.model.CourseProgressStatus;
import co.edu.uniquindio.brujula.generated.model.CourseType;
import co.edu.uniquindio.brujula.generated.model.CurriculumCourse;
import co.edu.uniquindio.brujula.generated.model.CurriculumView;
import co.edu.uniquindio.brujula.generated.model.CurriculumViewSemestersInner;
import co.edu.uniquindio.brujula.generated.model.JourneyRef;
import co.edu.uniquindio.brujula.generated.model.PeriodAverage;
import co.edu.uniquindio.brujula.generated.model.ProgressSummary;
import co.edu.uniquindio.brujula.identity.application.dto.UserProfile;
import co.edu.uniquindio.brujula.identity.application.port.in.GetCurrentUserQuery;
import co.edu.uniquindio.brujula.progress.application.dto.CourseAttemptPageView;
import co.edu.uniquindio.brujula.progress.application.dto.CourseAttemptView;
import co.edu.uniquindio.brujula.progress.application.dto.CreateCourseAttemptCommand;
import co.edu.uniquindio.brujula.progress.application.dto.CurriculumCourseData;
import co.edu.uniquindio.brujula.progress.application.dto.CurriculumSemesterData;
import co.edu.uniquindio.brujula.progress.application.dto.CurriculumViewData;
import co.edu.uniquindio.brujula.progress.application.dto.ListCourseAttemptsQuery;
import co.edu.uniquindio.brujula.progress.application.dto.PeriodAverageView;
import co.edu.uniquindio.brujula.progress.application.dto.ProgressSummaryView;
import co.edu.uniquindio.brujula.progress.application.dto.UpdateCourseAttemptCommand;
import co.edu.uniquindio.brujula.progress.application.port.in.CreateCourseAttemptUseCase;
import co.edu.uniquindio.brujula.progress.application.port.in.DeleteCourseAttemptUseCase;
import co.edu.uniquindio.brujula.progress.application.port.in.GetCourseAttemptQuery;
import co.edu.uniquindio.brujula.progress.application.port.in.GetSubscriptionCurriculumQuery;
import co.edu.uniquindio.brujula.progress.application.port.in.GetSubscriptionProgressQuery;
import co.edu.uniquindio.brujula.progress.application.port.in.ListMyCourseAttemptsQuery;
import co.edu.uniquindio.brujula.progress.application.port.in.UpdateCourseAttemptUseCase;
import java.math.BigDecimal;
import java.net.URI;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST adapter: course attempt endpoints ({@code /me/course-attempts/**}).
 *
 * <p>Implements the generated {@link ProgressApi} contract-first interface. Package-private.
 * STUDENT-only; STAFF/ADMIN access rejected via {@code @PreAuthorize("hasRole('STUDENT')")}.
 *
 * <p>Also implements curriculum and progress summary endpoints. Delegates to {@link
 * GetSubscriptionCurriculumQuery} and {@link GetSubscriptionProgressQuery}.
 *
 * <p>{@code ProgressSummary.programName} is resolved via {@code GetSubscriptionProgressService}
 * which calls the programcatalog published port.
 *
 * <p>{@code PeriodAverage.periodName} is resolved via the offering period catalog ({@code
 * ListPeriodsQuery}), falling back to the raw period id only when the catalog has no match.
 */
@RestController
class CourseAttemptController implements ProgressApi {

  private final CreateCourseAttemptUseCase createCourseAttemptUseCase;
  private final ListMyCourseAttemptsQuery listMyCourseAttemptsQuery;
  private final GetCourseAttemptQuery getCourseAttemptQuery;
  private final UpdateCourseAttemptUseCase updateCourseAttemptUseCase;
  private final DeleteCourseAttemptUseCase deleteCourseAttemptUseCase;
  private final GetCurrentUserQuery getCurrentUserQuery;
  private final GetSubscriptionCurriculumQuery getSubscriptionCurriculumQuery;
  private final GetSubscriptionProgressQuery getSubscriptionProgressQuery;

  CourseAttemptController(
      CreateCourseAttemptUseCase createCourseAttemptUseCase,
      ListMyCourseAttemptsQuery listMyCourseAttemptsQuery,
      GetCourseAttemptQuery getCourseAttemptQuery,
      UpdateCourseAttemptUseCase updateCourseAttemptUseCase,
      DeleteCourseAttemptUseCase deleteCourseAttemptUseCase,
      GetCurrentUserQuery getCurrentUserQuery,
      GetSubscriptionCurriculumQuery getSubscriptionCurriculumQuery,
      GetSubscriptionProgressQuery getSubscriptionProgressQuery) {
    this.createCourseAttemptUseCase = createCourseAttemptUseCase;
    this.listMyCourseAttemptsQuery = listMyCourseAttemptsQuery;
    this.getCourseAttemptQuery = getCourseAttemptQuery;
    this.updateCourseAttemptUseCase = updateCourseAttemptUseCase;
    this.deleteCourseAttemptUseCase = deleteCourseAttemptUseCase;
    this.getCurrentUserQuery = getCurrentUserQuery;
    this.getSubscriptionCurriculumQuery = getSubscriptionCurriculumQuery;
    this.getSubscriptionProgressQuery = getSubscriptionProgressQuery;
  }

  // -------------------------------------------------------------------------
  // POST /me/course-attempts — create
  // -------------------------------------------------------------------------

  @Override
  @PreAuthorize("hasRole('STUDENT')")
  public ResponseEntity<CourseAttempt> createCourseAttempt(
      UUID idempotencyKey,
      String xXsrfToken,
      CourseAttemptCreateRequest courseAttemptCreateRequest) {

    UUID callerId = resolveCallerId();
    String requestHash = computeHash(courseAttemptCreateRequest);

    String statusName =
        courseAttemptCreateRequest.getStatus() != null
            ? courseAttemptCreateRequest.getStatus().getValue()
            : null;

    CreateCourseAttemptCommand command =
        new CreateCourseAttemptCommand(
            idempotencyKey,
            callerId,
            courseAttemptCreateRequest.getSubscriptionId(),
            courseAttemptCreateRequest.getCourseId(),
            courseAttemptCreateRequest.getPeriodId(),
            courseAttemptCreateRequest.getJourneyCode(),
            statusName,
            courseAttemptCreateRequest.getFinalGrade() != null
                ? BigDecimal.valueOf(courseAttemptCreateRequest.getFinalGrade())
                : null,
            requestHash);

    CourseAttemptView view = createCourseAttemptUseCase.createCourseAttempt(command);
    CourseAttempt response = toDto(view);

    return ResponseEntity.created(URI.create("/me/course-attempts/" + view.id())).body(response);
  }

  // -------------------------------------------------------------------------
  // GET /me/course-attempts — list
  // -------------------------------------------------------------------------

  @Override
  @PreAuthorize("hasRole('STUDENT')")
  public ResponseEntity<CourseAttemptPage> listMyCourseAttempts(
      @Nullable UUID subscriptionId,
      @Nullable UUID courseId,
      Integer page,
      Integer size,
      @Nullable List<String> sort) {

    UUID callerId = resolveCallerId();
    ListCourseAttemptsQuery query =
        new ListCourseAttemptsQuery(
            callerId, subscriptionId, courseId, page != null ? page : 0, size != null ? size : 20);

    CourseAttemptPageView pageView = listMyCourseAttemptsQuery.listMyCourseAttempts(query);

    List<CourseAttempt> dtos = pageView.content().stream().map(this::toDto).toList();
    CourseAttemptPage result =
        new CourseAttemptPage(
            pageView.page(),
            pageView.size(),
            pageView.totalElements(),
            pageView.totalPages(),
            dtos);

    return ResponseEntity.ok(result);
  }

  // -------------------------------------------------------------------------
  // GET /me/course-attempts/{attemptId} — get one
  // -------------------------------------------------------------------------

  @Override
  @PreAuthorize("hasRole('STUDENT')")
  public ResponseEntity<CourseAttempt> getCourseAttempt(UUID attemptId) {
    UUID callerId = resolveCallerId();
    CourseAttemptView view = getCourseAttemptQuery.getCourseAttempt(attemptId, callerId);
    return ResponseEntity.ok(toDto(view));
  }

  // -------------------------------------------------------------------------
  // PUT /me/course-attempts/{attemptId} — update
  // -------------------------------------------------------------------------

  @Override
  @PreAuthorize("hasRole('STUDENT')")
  public ResponseEntity<CourseAttempt> updateCourseAttempt(
      String xXsrfToken, UUID attemptId, CourseAttemptUpdateRequest courseAttemptUpdateRequest) {

    UUID callerId = resolveCallerId();
    Long versionLong = courseAttemptUpdateRequest.getVersion();
    long version = versionLong != null ? versionLong : 0L;

    String updateStatusName =
        courseAttemptUpdateRequest.getStatus() != null
            ? courseAttemptUpdateRequest.getStatus().getValue()
            : null;

    UpdateCourseAttemptCommand command =
        new UpdateCourseAttemptCommand(
            attemptId,
            callerId,
            updateStatusName,
            courseAttemptUpdateRequest.getFinalGrade() != null
                ? BigDecimal.valueOf(courseAttemptUpdateRequest.getFinalGrade())
                : null,
            courseAttemptUpdateRequest.getPeriodId(),
            courseAttemptUpdateRequest.getJourneyCode(),
            version);

    CourseAttemptView view = updateCourseAttemptUseCase.updateCourseAttempt(command);
    return ResponseEntity.ok(toDto(view));
  }

  // -------------------------------------------------------------------------
  // DELETE /me/course-attempts/{attemptId} — delete
  // -------------------------------------------------------------------------

  @Override
  @PreAuthorize("hasRole('STUDENT')")
  public ResponseEntity<Void> deleteCourseAttempt(String xXsrfToken, UUID attemptId) {
    UUID callerId = resolveCallerId();
    deleteCourseAttemptUseCase.deleteCourseAttempt(attemptId, callerId);
    return ResponseEntity.noContent().build();
  }

  // -------------------------------------------------------------------------
  // GET /me/subscriptions/{id}/curriculum — curriculum view
  // -------------------------------------------------------------------------

  @Override
  @PreAuthorize("hasRole('STUDENT')")
  public ResponseEntity<CurriculumView> getSubscriptionCurriculum(UUID subscriptionId) {
    UUID callerId = resolveCallerId();
    CurriculumViewData data =
        getSubscriptionCurriculumQuery.getSubscriptionCurriculum(subscriptionId, callerId);
    CurriculumView response = toCurriculumViewDto(data);
    return ResponseEntity.ok(response);
  }

  // -------------------------------------------------------------------------
  // GET /me/subscriptions/{id}/progress — progress summary
  // -------------------------------------------------------------------------

  @Override
  @PreAuthorize("hasRole('STUDENT')")
  public ResponseEntity<ProgressSummary> getSubscriptionProgress(UUID subscriptionId) {
    UUID callerId = resolveCallerId();
    ProgressSummaryView view =
        getSubscriptionProgressQuery.getSubscriptionProgress(subscriptionId, callerId);
    ProgressSummary response = toProgressSummaryDto(subscriptionId, view);
    return ResponseEntity.ok(response);
  }

  // -------------------------------------------------------------------------
  // Principal resolution (BOLA safe — never from request parameter)
  // -------------------------------------------------------------------------

  private UUID resolveCallerId() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    String sub = extractSubject(auth);
    UserProfile profile = getCurrentUserQuery.getCurrentUserBySubject(sub);
    return profile.id();
  }

  private String extractSubject(Authentication auth) {
    if (auth != null && auth.getPrincipal() instanceof OidcUser oidcUser) {
      return oidcUser.getSubject();
    }
    if (auth != null && auth.getName() != null) {
      return auth.getName();
    }
    throw new IllegalStateException("No authenticated principal found in SecurityContext");
  }

  // -------------------------------------------------------------------------
  // DTO mappers: application DTOs → generated contract models
  // -------------------------------------------------------------------------

  private CurriculumView toCurriculumViewDto(CurriculumViewData data) {
    JourneyRef journeyRef =
        new JourneyRef(data.referenceJourneyCode(), data.referenceJourneyName());

    List<CurriculumViewSemestersInner> semesters =
        data.semesters().stream().map(this::toSemesterDto).toList();

    return new CurriculumView(data.subscriptionId(), journeyRef, semesters);
  }

  private CurriculumViewSemestersInner toSemesterDto(CurriculumSemesterData semesterData) {
    List<CurriculumCourse> courses =
        semesterData.courses().stream().map(this::toCurriculumCourseDto).toList();
    return new CurriculumViewSemestersInner(semesterData.recommendedSemester(), courses);
  }

  private CurriculumCourse toCurriculumCourseDto(CurriculumCourseData courseData) {
    // Map String type to generated enum (no domain enums in REST layer)
    CourseType courseType = CourseType.fromValue(courseData.type());
    // Map String progressStatus to generated enum
    CourseProgressStatus progressStatus =
        CourseProgressStatus.fromValue(courseData.progressStatus());

    return new CurriculumCourse(
            courseData.courseId(),
            courseData.name(),
            courseData.credits(),
            courseType,
            progressStatus)
        .officialCode(courseData.officialCode())
        .unmetRequirements(courseData.unmetRequirements())
        .filledElectiveName(courseData.filledElectiveName());
  }

  /**
   * Maps {@link ProgressSummaryView} to the generated {@link ProgressSummary} contract model.
   *
   * <p>{@code programName} is resolved from the application view (programcatalog port).
   *
   * <p>{@code PeriodAverage.periodName} is resolved from the application view (offering port).
   */
  private ProgressSummary toProgressSummaryDto(UUID subscriptionId, ProgressSummaryView view) {
    // programName is the human-readable name resolved from programcatalog
    String programName = view.programName();

    Double estimatedAvg =
        view.estimatedHistoricalAverage() != null
            ? view.estimatedHistoricalAverage().doubleValue()
            : null;

    List<PeriodAverage> periodAverages =
        view.averagesByPeriod() != null
            ? view.averagesByPeriod().stream().map(this::toPeriodAverageDto).toList()
            : List.of();

    return new ProgressSummary(
            subscriptionId,
            programName,
            view.totalPlanCredits(),
            view.approvedCredits(),
            view.pendingCredits(),
            view.progressPercentage().doubleValue(),
            view.passedCourses(),
            view.pendingFailedCourses(),
            view.disclaimer())
        .estimatedHistoricalAverage(estimatedAvg)
        .averagesByPeriod(periodAverages);
  }

  private PeriodAverage toPeriodAverageDto(PeriodAverageView periodView) {
    // Resolve the human-readable period name; fall back to the raw id when the catalog
    // lookup yielded nothing (periodName is a required field in the contract).
    String periodName =
        periodView.periodName() != null
            ? periodView.periodName()
            : periodView.periodId().toString();
    return new PeriodAverage(
        periodView.periodId(), periodName, periodView.estimatedAverage().doubleValue());
  }

  // -------------------------------------------------------------------------
  // DTO mapper: CourseAttemptView → generated CourseAttempt model
  // -------------------------------------------------------------------------

  private CourseAttempt toDto(CourseAttemptView view) {
    String courseName = view.courseName() != null ? view.courseName() : "N/A";

    OffsetDateTime registeredAt =
        view.registeredAt() != null
            ? view.registeredAt().atOffset(ZoneOffset.UTC)
            : OffsetDateTime.now(ZoneOffset.UTC);

    // periodId is contract-REQUIRED (non-null since domain enforces it). Pass through directly.
    UUID periodId = view.periodId();

    co.edu.uniquindio.brujula.generated.model.AttemptStatus contractStatus =
        co.edu.uniquindio.brujula.generated.model.AttemptStatus.fromValue(view.statusName());

    Double finalGrade = view.finalGrade() != null ? view.finalGrade().doubleValue() : null;

    // periodName is an optional display field resolved by the service; a null value falls
    // back to the raw period id in the frontend.
    return new CourseAttempt(
            view.id(),
            view.subscriptionId(),
            view.courseId(),
            courseName,
            periodId,
            view.journeyCode(),
            contractStatus,
            registeredAt,
            view.version())
        .finalGrade(finalGrade)
        .periodName(view.periodName());
  }

  // -------------------------------------------------------------------------
  // Request hash computation for idempotency deduplication
  // -------------------------------------------------------------------------

  private String computeHash(CourseAttemptCreateRequest req) {
    try {
      String input =
          req.getSubscriptionId()
              + "|"
              + req.getCourseId()
              + "|"
              + req.getPeriodId()
              + "|"
              + req.getJourneyCode()
              + "|"
              + req.getStatus()
              + "|"
              + req.getFinalGrade();
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hash);
    } catch (Exception e) {
      throw new IllegalStateException("Failed to compute request hash", e);
    }
  }
}
