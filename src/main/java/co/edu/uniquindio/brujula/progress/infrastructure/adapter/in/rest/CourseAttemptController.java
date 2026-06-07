package co.edu.uniquindio.brujula.progress.infrastructure.adapter.in.rest;

import co.edu.uniquindio.brujula.generated.api.ProgressApi;
import co.edu.uniquindio.brujula.generated.model.CourseAttempt;
import co.edu.uniquindio.brujula.generated.model.CourseAttemptCreateRequest;
import co.edu.uniquindio.brujula.generated.model.CourseAttemptPage;
import co.edu.uniquindio.brujula.generated.model.CourseAttemptUpdateRequest;
import co.edu.uniquindio.brujula.generated.model.CurriculumView;
import co.edu.uniquindio.brujula.generated.model.ProgressSummary;
import org.springframework.http.HttpStatus;
import co.edu.uniquindio.brujula.identity.application.dto.UserProfile;
import co.edu.uniquindio.brujula.identity.application.port.in.GetCurrentUserQuery;
import co.edu.uniquindio.brujula.progress.application.dto.CourseAttemptPageView;
import co.edu.uniquindio.brujula.progress.application.dto.CourseAttemptView;
import co.edu.uniquindio.brujula.progress.application.dto.CreateCourseAttemptCommand;
import co.edu.uniquindio.brujula.progress.application.dto.ListCourseAttemptsQuery;
import co.edu.uniquindio.brujula.progress.application.dto.UpdateCourseAttemptCommand;
import co.edu.uniquindio.brujula.progress.application.port.in.CreateCourseAttemptUseCase;
import co.edu.uniquindio.brujula.progress.application.port.in.DeleteCourseAttemptUseCase;
import co.edu.uniquindio.brujula.progress.application.port.in.GetCourseAttemptQuery;
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
 * <p>Implements the generated {@link ProgressApi} contract-first interface (ARQ-001). Package-private
 * per ARQ-006b. STUDENT-only; STAFF/ADMIN access rejected via {@code @PreAuthorize("hasRole('STUDENT')")}.
 *
 * <p>Also implements curriculum and progress summary endpoints ({@code WU4-SEAM}) with placeholder
 * responses. The full implementation is in WU4.
 *
 * <p>Satisfies: PRG-1..4, CROSS-2, CROSS-3, CROSS-4. ARQ-001.
 */
@RestController
class CourseAttemptController implements ProgressApi {

  private final CreateCourseAttemptUseCase createCourseAttemptUseCase;
  private final ListMyCourseAttemptsQuery listMyCourseAttemptsQuery;
  private final GetCourseAttemptQuery getCourseAttemptQuery;
  private final UpdateCourseAttemptUseCase updateCourseAttemptUseCase;
  private final DeleteCourseAttemptUseCase deleteCourseAttemptUseCase;
  private final GetCurrentUserQuery getCurrentUserQuery;

  CourseAttemptController(
      CreateCourseAttemptUseCase createCourseAttemptUseCase,
      ListMyCourseAttemptsQuery listMyCourseAttemptsQuery,
      GetCourseAttemptQuery getCourseAttemptQuery,
      UpdateCourseAttemptUseCase updateCourseAttemptUseCase,
      DeleteCourseAttemptUseCase deleteCourseAttemptUseCase,
      GetCurrentUserQuery getCurrentUserQuery) {
    this.createCourseAttemptUseCase = createCourseAttemptUseCase;
    this.listMyCourseAttemptsQuery = listMyCourseAttemptsQuery;
    this.getCourseAttemptQuery = getCourseAttemptQuery;
    this.updateCourseAttemptUseCase = updateCourseAttemptUseCase;
    this.deleteCourseAttemptUseCase = deleteCourseAttemptUseCase;
    this.getCurrentUserQuery = getCurrentUserQuery;
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

    String statusName = courseAttemptCreateRequest.getStatus() != null
        ? courseAttemptCreateRequest.getStatus().getValue()
        : null;

    CreateCourseAttemptCommand command = new CreateCourseAttemptCommand(
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

    return ResponseEntity
        .created(URI.create("/me/course-attempts/" + view.id()))
        .body(response);
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
    ListCourseAttemptsQuery query = new ListCourseAttemptsQuery(
        callerId, subscriptionId, courseId, page != null ? page : 0, size != null ? size : 20);

    CourseAttemptPageView pageView = listMyCourseAttemptsQuery.listMyCourseAttempts(query);

    List<CourseAttempt> dtos = pageView.content().stream().map(this::toDto).toList();
    CourseAttemptPage result = new CourseAttemptPage(
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
      String xXsrfToken,
      UUID attemptId,
      CourseAttemptUpdateRequest courseAttemptUpdateRequest) {

    UUID callerId = resolveCallerId();
    Long versionLong = courseAttemptUpdateRequest.getVersion();
    long version = versionLong != null ? versionLong : 0L;

    String updateStatusName = courseAttemptUpdateRequest.getStatus() != null
        ? courseAttemptUpdateRequest.getStatus().getValue()
        : null;

    UpdateCourseAttemptCommand command = new UpdateCourseAttemptCommand(
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
  // WU4-SEAM: curriculum and progress summary (placeholder stubs)
  // -------------------------------------------------------------------------

  @Override
  @PreAuthorize("hasRole('STUDENT')")
  public ResponseEntity<CurriculumView> getSubscriptionCurriculum(UUID subscriptionId) {
    // WU4-SEAM: full implementation in WU4 (CourseAvailabilityService + curriculum integration).
    // Returning 501 so consumers can distinguish "not implemented" from "zero progress".
    return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
  }

  @Override
  @PreAuthorize("hasRole('STUDENT')")
  public ResponseEntity<ProgressSummary> getSubscriptionProgress(UUID subscriptionId) {
    // WU4-SEAM: full implementation in WU4 (ProgressSummaryCalculator).
    // Returning 501 so consumers can distinguish "not implemented" from "zero progress".
    return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
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
  // DTO mapper: CourseAttemptView → generated CourseAttempt model (ARQ-005)
  // -------------------------------------------------------------------------

  private CourseAttempt toDto(CourseAttemptView view) {
    String courseName = view.courseName() != null ? view.courseName() : "N/A";

    OffsetDateTime registeredAt = view.registeredAt() != null
        ? view.registeredAt().atOffset(ZoneOffset.UTC)
        : OffsetDateTime.now(ZoneOffset.UTC);

    // FIX-S2: periodId is nullable/opaque per ADR-F3-009 and the contract schema.
    // Pass null through as null; do NOT substitute a sentinel zero UUID.
    UUID periodId = view.periodId();

    co.edu.uniquindio.brujula.generated.model.AttemptStatus contractStatus =
        co.edu.uniquindio.brujula.generated.model.AttemptStatus.fromValue(view.statusName());

    Double finalGrade = view.finalGrade() != null ? view.finalGrade().doubleValue() : null;

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
        .finalGrade(finalGrade);
  }

  // -------------------------------------------------------------------------
  // Request hash computation for idempotency deduplication
  // -------------------------------------------------------------------------

  private String computeHash(CourseAttemptCreateRequest req) {
    try {
      String input = req.getSubscriptionId() + "|" + req.getCourseId()
          + "|" + req.getPeriodId() + "|" + req.getJourneyCode()
          + "|" + req.getStatus() + "|" + req.getFinalGrade();
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hash);
    } catch (Exception e) {
      throw new IllegalStateException("Failed to compute request hash", e);
    }
  }
}
