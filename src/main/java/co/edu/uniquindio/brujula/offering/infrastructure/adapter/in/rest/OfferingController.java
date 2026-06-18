package co.edu.uniquindio.brujula.offering.infrastructure.adapter.in.rest;

import co.edu.uniquindio.brujula.generated.api.OfferingsApi;
import co.edu.uniquindio.brujula.generated.model.AcademicPeriod;
import co.edu.uniquindio.brujula.generated.model.OfferingCreateRequest;
import co.edu.uniquindio.brujula.generated.model.OfferingDetail;
import co.edu.uniquindio.brujula.generated.model.OfferingPage;
import co.edu.uniquindio.brujula.generated.model.OfferingStatus;
import co.edu.uniquindio.brujula.generated.model.OfferingUpdateRequest;
import co.edu.uniquindio.brujula.generated.model.PeriodStatus;
import co.edu.uniquindio.brujula.generated.model.PublishOfferingRequest;
import co.edu.uniquindio.brujula.generated.model.ScheduleOption;
import co.edu.uniquindio.brujula.generated.model.ScheduleOptionCreateRequest;
import co.edu.uniquindio.brujula.generated.model.ScheduleOptionUpdateRequest;
import co.edu.uniquindio.brujula.identity.application.port.in.GetCurrentUserQuery;
import co.edu.uniquindio.brujula.offering.application.dto.CreateOfferingCommand;
import co.edu.uniquindio.brujula.offering.application.dto.ListOfferingsCriteria;
import co.edu.uniquindio.brujula.offering.application.dto.OfferingDetailView;
import co.edu.uniquindio.brujula.offering.application.dto.OfferingPageView;
import co.edu.uniquindio.brujula.offering.application.dto.PublishOfferingCommand;
import co.edu.uniquindio.brujula.offering.application.dto.UpdateOfferingCommand;
import co.edu.uniquindio.brujula.offering.application.port.in.CreateOfferingUseCase;
import co.edu.uniquindio.brujula.offering.application.port.in.GetOfferingQuery;
import co.edu.uniquindio.brujula.offering.application.port.in.ListOfferingsQuery;
import co.edu.uniquindio.brujula.offering.application.port.in.ListPeriodsQuery;
import co.edu.uniquindio.brujula.offering.application.port.in.PublishOfferingUseCase;
import co.edu.uniquindio.brujula.offering.application.port.in.UpdateOfferingUseCase;
import java.net.URI;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.ObjectMapper;

/**
 * REST adapter: offering catalogue endpoints ({@code /periods}, {@code /offerings/**}).
 *
 * <p>Implements the generated {@link OfferingsApi} contract-first interface (ARQ-001).
 * Package-private per ARQ-006b. Read endpoints (GET /periods, GET /offerings, GET /offerings/{id})
 * are accessible by STUDENT role. Staff write endpoints implemented in WU2.
 *
 * <p>Satisfies: OFF-001, OFF-002, OFF-003, API-001, OFFW-003, OFFW-004.
 */
@RestController
class OfferingController implements OfferingsApi {

  private final ListPeriodsQuery listPeriodsQuery;
  private final ListOfferingsQuery listOfferingsQuery;
  private final GetOfferingQuery getOfferingQuery;
  private final CreateOfferingUseCase createOfferingUseCase;
  private final UpdateOfferingUseCase updateOfferingUseCase;
  private final PublishOfferingUseCase publishOfferingUseCase;
  private final GetCurrentUserQuery getCurrentUserQuery;
  private final ObjectMapper objectMapper;
  private final OfferingRestMapper restMapper;

  OfferingController(
      ListPeriodsQuery listPeriodsQuery,
      ListOfferingsQuery listOfferingsQuery,
      GetOfferingQuery getOfferingQuery,
      CreateOfferingUseCase createOfferingUseCase,
      UpdateOfferingUseCase updateOfferingUseCase,
      PublishOfferingUseCase publishOfferingUseCase,
      GetCurrentUserQuery getCurrentUserQuery,
      ObjectMapper objectMapper) {
    this.listPeriodsQuery = listPeriodsQuery;
    this.listOfferingsQuery = listOfferingsQuery;
    this.getOfferingQuery = getOfferingQuery;
    this.createOfferingUseCase = createOfferingUseCase;
    this.updateOfferingUseCase = updateOfferingUseCase;
    this.publishOfferingUseCase = publishOfferingUseCase;
    this.getCurrentUserQuery = getCurrentUserQuery;
    this.objectMapper = objectMapper;
    this.restMapper = new OfferingRestMapper();
  }

  // -------------------------------------------------------------------------
  // GET /periods — list academic periods (student-readable)
  // -------------------------------------------------------------------------

  @Override
  @PreAuthorize("hasAnyRole('STUDENT', 'STAFF', 'ADMIN')")
  public ResponseEntity<List<AcademicPeriod>> listPeriods(@Nullable PeriodStatus status) {
    // status filter mapped from generated PeriodStatus enum (null = all non-archived)
    String statusFilter = status != null ? status.name() : null;
    List<AcademicPeriod> periods =
        listPeriodsQuery.listPeriods(statusFilter).stream().map(restMapper::toContract).toList();
    return ResponseEntity.ok(periods);
  }

  // -------------------------------------------------------------------------
  // GET /offerings — list published offerings (student-readable)
  // -------------------------------------------------------------------------

  @Override
  @PreAuthorize("hasAnyRole('STUDENT', 'STAFF', 'ADMIN')")
  public ResponseEntity<OfferingPage> listOfferings(
      UUID periodId,
      @Nullable UUID programId,
      @Nullable UUID courseId,
      @Nullable OfferingStatus status,
      @Nullable List<String> journeyCode,
      Integer page,
      Integer size,
      @Nullable List<String> sort) {
    // OFFW-007: pass caller role so the service routes to the correct persistence query
    String callerRole = resolveCallerRole();
    ListOfferingsCriteria criteria =
        new ListOfferingsCriteria(
            periodId,
            programId,
            null, // courseId filter — reserved for future use
            status != null ? status.name() : null,
            page == null ? 0 : page,
            size == null ? 20 : size,
            callerRole,
            journeyCode);
    OfferingPageView pageView = listOfferingsQuery.listOfferings(criteria);
    return ResponseEntity.ok(restMapper.toContractPage(pageView));
  }

  // -------------------------------------------------------------------------
  // GET /offerings/{offeringId} — get offering detail (student-readable)
  // -------------------------------------------------------------------------

  @Override
  @PreAuthorize("hasAnyRole('STUDENT', 'STAFF', 'ADMIN')")
  public ResponseEntity<OfferingDetail> getOffering(UUID offeringId) {
    OfferingDetailView detail = getOfferingQuery.getOffering(offeringId);
    return ResponseEntity.ok(restMapper.toContractDetail(detail));
  }

  // -------------------------------------------------------------------------
  // Staff write endpoints (WU2)
  // -------------------------------------------------------------------------

  @Override
  @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
  public ResponseEntity<OfferingDetail> createOffering(
      UUID idempotencyKey,
      String X_XSRF_TOKEN,
      UUID programId,
      OfferingCreateRequest offeringCreateRequest) {
    UUID actorId = resolveActorId();
    String requestHash = hashRequest(offeringCreateRequest);
    CreateOfferingCommand command =
        restMapper.fromRequest(
            idempotencyKey, programId, offeringCreateRequest, requestHash, actorId);
    OfferingDetailView view = createOfferingUseCase.createOffering(command);
    URI location = URI.create("/staff/programs/" + programId + "/offerings/" + view.id());
    return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED)
        .location(location)
        .body(restMapper.toContractDetail(view));
  }

  @Override
  public ResponseEntity<ScheduleOption> createScheduleOption(
      String X_XSRF_TOKEN,
      UUID offeringId,
      ScheduleOptionCreateRequest scheduleOptionCreateRequest) {
    throw new UnsupportedOperationException("createScheduleOption not implemented in WU1");
  }

  @Override
  public ResponseEntity<Void> deleteScheduleOption(String X_XSRF_TOKEN, UUID optionId) {
    throw new UnsupportedOperationException("deleteScheduleOption not implemented in WU1");
  }

  @Override
  @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
  public ResponseEntity<OfferingDetail> publishOffering(
      UUID idempotencyKey,
      String X_XSRF_TOKEN,
      UUID offeringId,
      PublishOfferingRequest publishOfferingRequest) {
    UUID actorId = resolveActorId();
    String requestHash = hashRequest(publishOfferingRequest);
    PublishOfferingCommand command =
        restMapper.fromPublishRequest(
            idempotencyKey, offeringId, publishOfferingRequest, requestHash, actorId);
    OfferingDetailView view = publishOfferingUseCase.publishOffering(command);
    return ResponseEntity.ok(restMapper.toContractDetail(view));
  }

  @Override
  @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
  public ResponseEntity<OfferingDetail> updateOffering(
      String X_XSRF_TOKEN, UUID offeringId, OfferingUpdateRequest offeringUpdateRequest) {
    UUID actorId = resolveActorId();
    UpdateOfferingCommand command =
        restMapper.fromUpdateRequest(offeringId, offeringUpdateRequest, actorId);
    OfferingDetailView view = updateOfferingUseCase.updateOffering(command);
    return ResponseEntity.ok(restMapper.toContractDetail(view));
  }

  @Override
  public ResponseEntity<ScheduleOption> updateScheduleOption(
      String X_XSRF_TOKEN, UUID optionId, ScheduleOptionUpdateRequest scheduleOptionUpdateRequest) {
    throw new UnsupportedOperationException("updateScheduleOption not implemented in WU1");
  }

  // -------------------------------------------------------------------------
  // Private helpers — ARQ-001 compliant (application DTOs only)
  // -------------------------------------------------------------------------

  /**
   * Resolve the caller's role from the Spring Security context for OFFW-007 routing. Returns the
   * first ROLE_ authority (STAFF/ADMIN/STUDENT) or null if not determinable.
   */
  private String resolveCallerRole() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null) {
      return null;
    }
    return auth.getAuthorities().stream()
        .map(a -> a.getAuthority())
        .filter(a -> a.startsWith("ROLE_"))
        .map(a -> a.substring("ROLE_".length()))
        .findFirst()
        .orElse(null);
  }

  /** Resolve actor UUID from session principal. BOLA safe: never from request. */
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
