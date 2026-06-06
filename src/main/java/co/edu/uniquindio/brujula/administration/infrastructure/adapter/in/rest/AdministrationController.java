package co.edu.uniquindio.brujula.administration.infrastructure.adapter.in.rest;

import co.edu.uniquindio.brujula.administration.application.dto.CreateStaffCommand;
import co.edu.uniquindio.brujula.administration.application.dto.StaffAccountView;
import co.edu.uniquindio.brujula.administration.application.port.in.CreateStaffUseCase;
import co.edu.uniquindio.brujula.administration.application.port.in.ListStaffQuery;
import co.edu.uniquindio.brujula.administration.application.port.in.ManageAllowedEmailDomainsUseCase;
import co.edu.uniquindio.brujula.administration.application.port.in.UpdateStaffUseCase;
import co.edu.uniquindio.brujula.administration.application.service.StaffNotFoundException;
import co.edu.uniquindio.brujula.generated.api.AdministrationApi;
import co.edu.uniquindio.brujula.generated.model.AcademicPeriod;
import co.edu.uniquindio.brujula.generated.model.AcademicPeriodCreateRequest;
import co.edu.uniquindio.brujula.generated.model.AcademicPeriodUpdateRequest;
import co.edu.uniquindio.brujula.generated.model.AccountStatus;
import co.edu.uniquindio.brujula.generated.model.AddAllowedEmailDomainRequest;
import co.edu.uniquindio.brujula.generated.model.AllowedEmailDomain;
import co.edu.uniquindio.brujula.generated.model.AssignProgramToStaffRequest;
import co.edu.uniquindio.brujula.generated.model.CreditLimit;
import co.edu.uniquindio.brujula.generated.model.CreditLimitUpsertRequest;
import co.edu.uniquindio.brujula.generated.model.Journey;
import co.edu.uniquindio.brujula.generated.model.JourneyCreateRequest;
import co.edu.uniquindio.brujula.generated.model.Program;
import co.edu.uniquindio.brujula.generated.model.ProgramCreateRequest;
import co.edu.uniquindio.brujula.generated.model.ProgramUpdateRequest;
import co.edu.uniquindio.brujula.generated.model.Staff;
import co.edu.uniquindio.brujula.generated.model.StaffAssignment;
import co.edu.uniquindio.brujula.generated.model.StaffCreateRequest;
import co.edu.uniquindio.brujula.generated.model.StaffUpdateRequest;
import co.edu.uniquindio.brujula.identity.application.port.in.GetCurrentUserQuery;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.ObjectMapper;

/**
 * REST adapter implementing {@link AdministrationApi} (contract-first, ARQ-001).
 *
 * <p>Implements Fase-1 endpoints fully: createStaff (idempotent), listStaff, updateStaff, getStaff,
 * addAllowedEmailDomain, listAllowedEmailDomains, removeAllowedEmailDomain.
 *
 * <p>Fase-2 stubs (return 501): assignProgramToStaff, listStaffAssignments, revokeStaffAssignment,
 * setCreditLimit, listCreditLimits, createPeriod, updatePeriod, createProgram, updateProgram,
 * createJourney.
 *
 * <p>ARQ-001: only generated model and application-layer DTOs imported here — no domain types.
 */
@RestController
class AdministrationController implements AdministrationApi {

  private final CreateStaffUseCase createStaffUseCase;
  private final ListStaffQuery listStaffQuery;
  private final UpdateStaffUseCase updateStaffUseCase;
  private final ManageAllowedEmailDomainsUseCase manageAllowedEmailDomainsUseCase;
  private final GetCurrentUserQuery getCurrentUserQuery;
  private final ObjectMapper objectMapper;

  AdministrationController(
      CreateStaffUseCase createStaffUseCase,
      ListStaffQuery listStaffQuery,
      UpdateStaffUseCase updateStaffUseCase,
      ManageAllowedEmailDomainsUseCase manageAllowedEmailDomainsUseCase,
      GetCurrentUserQuery getCurrentUserQuery,
      ObjectMapper objectMapper) {
    this.createStaffUseCase = createStaffUseCase;
    this.listStaffQuery = listStaffQuery;
    this.updateStaffUseCase = updateStaffUseCase;
    this.manageAllowedEmailDomainsUseCase = manageAllowedEmailDomainsUseCase;
    this.getCurrentUserQuery = getCurrentUserQuery;
    this.objectMapper = objectMapper;
  }

  // -------------------------------------------------------------------------
  // Fase-1 endpoints
  // -------------------------------------------------------------------------

  @Override
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Staff> createStaff(
      UUID idempotencyKey, String xXsrfToken, StaffCreateRequest request) {

    UUID actorId = resolveActorId();
    String requestHash = hashRequest(request);

    var command =
        new CreateStaffCommand(
            idempotencyKey, actorId, request.getEmail(), request.getFullName(), requestHash);

    StaffAccountView view = createStaffUseCase.createStaff(command);
    return ResponseEntity.status(HttpStatus.CREATED).body(toStaffDto(view));
  }

  @Override
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<co.edu.uniquindio.brujula.generated.model.StaffPage> listStaff(
      @Nullable AccountStatus status, Integer page, Integer size, @Nullable List<String> sort) {

    // Pass null status name when no filter requested; ListStaffQuery accepts null
    String statusName = status != null ? status.getValue() : null;
    int pageNum = page != null ? page : 0;
    int pageSize = size != null ? size : 20;

    var staffPage = listStaffQuery.listStaff(statusName, pageNum, pageSize);

    List<Staff> items = staffPage.items().stream().map(this::toStaffDto).toList();
    var dto =
        new co.edu.uniquindio.brujula.generated.model.StaffPage(
            pageNum, pageSize, staffPage.totalElements(), staffPage.totalPages(), items);
    return ResponseEntity.ok(dto);
  }

  @Override
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Staff> getStaff(UUID staffId) {
    StaffAccountView view =
        listStaffQuery.getById(staffId).orElseThrow(() -> new StaffNotFoundException(staffId));
    return ResponseEntity.ok(toStaffDto(view));
  }

  @Override
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Staff> updateStaff(
      String xXsrfToken, UUID staffId, StaffUpdateRequest request) {

    String statusName = request.getStatus() != null ? request.getStatus().getValue() : "ACTIVE";
    long version = request.getVersion() != null ? request.getVersion() : 0L;
    UUID actorId = resolveActorId();

    StaffAccountView view = updateStaffUseCase.updateStatus(staffId, statusName, version, actorId);
    return ResponseEntity.ok(toStaffDto(view));
  }

  @Override
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<AllowedEmailDomain> addAllowedEmailDomain(
      String xXsrfToken, AddAllowedEmailDomainRequest request) {

    UUID actorId = resolveActorId();
    var domain = manageAllowedEmailDomainsUseCase.addDomain(request.getDomain(), actorId);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(new AllowedEmailDomain(domain.id(), domain.domain()));
  }

  @Override
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<List<AllowedEmailDomain>> listAllowedEmailDomains() {
    List<AllowedEmailDomain> result =
        manageAllowedEmailDomainsUseCase.listAllowedDomains().stream()
            .map(d -> new AllowedEmailDomain(d.id(), d.domain()))
            .toList();
    return ResponseEntity.ok(result);
  }

  @Override
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Void> removeAllowedEmailDomain(String xXsrfToken, UUID domainId) {
    UUID actorId = resolveActorId();
    manageAllowedEmailDomainsUseCase.removeDomain(domainId, actorId);
    return ResponseEntity.noContent().build();
  }

  // -------------------------------------------------------------------------
  // Fase-2 stubs — return 501 Not Implemented
  // -------------------------------------------------------------------------

  @Override
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<StaffAssignment> assignProgramToStaff(
      String xXsrfToken, UUID staffId, AssignProgramToStaffRequest request) {
    return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
  }

  @Override
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<List<StaffAssignment>> listStaffAssignments(UUID staffId) {
    return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
  }

  @Override
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Void> revokeStaffAssignment(
      String xXsrfToken, UUID staffId, UUID assignmentId) {
    return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
  }

  @Override
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<CreditLimit> setCreditLimit(
      String xXsrfToken, CreditLimitUpsertRequest request) {
    return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
  }

  @Override
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<List<CreditLimit>> listCreditLimits(
      @Nullable UUID programId, @Nullable UUID periodId) {
    return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
  }

  @Override
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<AcademicPeriod> createPeriod(
      UUID idempotencyKey, String xXsrfToken, AcademicPeriodCreateRequest request) {
    return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
  }

  @Override
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<AcademicPeriod> updatePeriod(
      String xXsrfToken, UUID periodId, AcademicPeriodUpdateRequest request) {
    return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
  }

  @Override
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Program> createProgram(
      UUID idempotencyKey, String xXsrfToken, ProgramCreateRequest request) {
    return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
  }

  @Override
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Program> updateProgram(
      String xXsrfToken, UUID programId, ProgramUpdateRequest request) {
    return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
  }

  @Override
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Journey> createJourney(
      UUID idempotencyKey, String xXsrfToken, JourneyCreateRequest request) {
    return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
  }

  // -------------------------------------------------------------------------
  // Private helpers — ARQ-001 compliant (application DTOs only)
  // -------------------------------------------------------------------------

  /** Resolve actor UUID from session principal (OIDC sub lookup). BOLA safe: never from request. */
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

  // DTO mappers: application DTOs → generated model (ARQ-005)

  private Staff toStaffDto(StaffAccountView view) {
    return new Staff(
        view.id(),
        view.fullName(),
        view.email(),
        AccountStatus.fromValue(view.status()),
        view.version());
  }
}
