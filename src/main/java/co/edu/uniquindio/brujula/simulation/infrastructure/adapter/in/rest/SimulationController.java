package co.edu.uniquindio.brujula.simulation.infrastructure.adapter.in.rest;

import co.edu.uniquindio.brujula.generated.api.SimulationsApi;
import co.edu.uniquindio.brujula.generated.model.DuplicateSimulationRequest;
import co.edu.uniquindio.brujula.generated.model.Simulation;
import co.edu.uniquindio.brujula.generated.model.SimulationCreateRequest;
import co.edu.uniquindio.brujula.generated.model.SimulationDetail;
import co.edu.uniquindio.brujula.generated.model.SimulationPage;
import co.edu.uniquindio.brujula.generated.model.SimulationSelection;
import co.edu.uniquindio.brujula.generated.model.SimulationSelectionCreateRequest;
import co.edu.uniquindio.brujula.generated.model.SimulationStatus;
import co.edu.uniquindio.brujula.generated.model.SimulationUpdateRequest;
import co.edu.uniquindio.brujula.identity.application.dto.UserProfile;
import co.edu.uniquindio.brujula.identity.application.port.in.GetCurrentUserQuery;
import co.edu.uniquindio.brujula.simulation.application.dto.AddSelectionCommand;
import co.edu.uniquindio.brujula.simulation.application.dto.CreateSimulationCommand;
import co.edu.uniquindio.brujula.simulation.application.dto.DuplicateSimulationCommand;
import co.edu.uniquindio.brujula.simulation.application.dto.ListSimulationsCriteria;
import co.edu.uniquindio.brujula.simulation.application.dto.SimulationDetailView;
import co.edu.uniquindio.brujula.simulation.application.dto.SimulationPageView;
import co.edu.uniquindio.brujula.simulation.application.dto.SimulationSelectionView;
import co.edu.uniquindio.brujula.simulation.application.dto.SimulationView;
import co.edu.uniquindio.brujula.simulation.application.dto.UpdateSimulationCommand;
import co.edu.uniquindio.brujula.simulation.application.port.in.AddSimulationSelectionUseCase;
import co.edu.uniquindio.brujula.simulation.application.port.in.CreateSimulationUseCase;
import co.edu.uniquindio.brujula.simulation.application.port.in.DeleteSimulationUseCase;
import co.edu.uniquindio.brujula.simulation.application.port.in.DuplicateSimulationUseCase;
import co.edu.uniquindio.brujula.simulation.application.port.in.GetSimulationQuery;
import co.edu.uniquindio.brujula.simulation.application.port.in.ListMySimulationsQuery;
import co.edu.uniquindio.brujula.simulation.application.port.in.ListSimulationSelectionsQuery;
import co.edu.uniquindio.brujula.simulation.application.port.in.RemoveSimulationSelectionUseCase;
import co.edu.uniquindio.brujula.simulation.application.port.in.ReviewSimulationUseCase;
import co.edu.uniquindio.brujula.simulation.application.port.in.UpdateSimulationUseCase;
import java.net.URI;
import java.security.MessageDigest;
import java.util.ArrayList;
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
 * REST adapter: student simulation endpoints ({@code /me/simulations/**}).
 *
 * <p>Implements the generated {@link SimulationsApi} contract-first interface (ARQ-001).
 * Package-private per ARQ-006b. STUDENT-only.
 *
 * <p>WU3 SCOPE: create, list, get (skeleton), update, delete, duplicate are implemented.
 * WU4a SCOPE: addSelection, removeSelection, listSelections are implemented.
 * The following endpoint returns HTTP 501 until WU5:
 * <ul>
 *   <li>{@code POST .../review} (reviewSimulation) — WU5
 * </ul>
 *
 * <p>Satisfies: SIM-001..006, VAL-001, VAL-010, VAL-011, X-001..004, INV-001..002..004.
 */
@RestController
class SimulationController implements SimulationsApi {

  private final CreateSimulationUseCase createSimulationUseCase;
  private final ListMySimulationsQuery listMySimulationsQuery;
  private final GetSimulationQuery getSimulationQuery;
  private final UpdateSimulationUseCase updateSimulationUseCase;
  private final DeleteSimulationUseCase deleteSimulationUseCase;
  private final DuplicateSimulationUseCase duplicateSimulationUseCase;
  private final AddSimulationSelectionUseCase addSimulationSelectionUseCase;
  private final RemoveSimulationSelectionUseCase removeSimulationSelectionUseCase;
  private final ListSimulationSelectionsQuery listSimulationSelectionsQuery;
  private final ReviewSimulationUseCase reviewSimulationUseCase;
  private final SimulationRestMapper mapper;
  private final GetCurrentUserQuery getCurrentUserQuery;

  SimulationController(
      CreateSimulationUseCase createSimulationUseCase,
      ListMySimulationsQuery listMySimulationsQuery,
      GetSimulationQuery getSimulationQuery,
      UpdateSimulationUseCase updateSimulationUseCase,
      DeleteSimulationUseCase deleteSimulationUseCase,
      DuplicateSimulationUseCase duplicateSimulationUseCase,
      AddSimulationSelectionUseCase addSimulationSelectionUseCase,
      RemoveSimulationSelectionUseCase removeSimulationSelectionUseCase,
      ListSimulationSelectionsQuery listSimulationSelectionsQuery,
      ReviewSimulationUseCase reviewSimulationUseCase,
      SimulationRestMapper mapper,
      GetCurrentUserQuery getCurrentUserQuery) {
    this.createSimulationUseCase = createSimulationUseCase;
    this.listMySimulationsQuery = listMySimulationsQuery;
    this.getSimulationQuery = getSimulationQuery;
    this.updateSimulationUseCase = updateSimulationUseCase;
    this.deleteSimulationUseCase = deleteSimulationUseCase;
    this.duplicateSimulationUseCase = duplicateSimulationUseCase;
    this.addSimulationSelectionUseCase = addSimulationSelectionUseCase;
    this.removeSimulationSelectionUseCase = removeSimulationSelectionUseCase;
    this.listSimulationSelectionsQuery = listSimulationSelectionsQuery;
    this.reviewSimulationUseCase = reviewSimulationUseCase;
    this.mapper = mapper;
    this.getCurrentUserQuery = getCurrentUserQuery;
  }

  // -------------------------------------------------------------------------
  // POST /me/simulations — create (SIM-001, X-001)
  // -------------------------------------------------------------------------

  @Override
  @PreAuthorize("hasRole('STUDENT')")
  public ResponseEntity<Simulation> createSimulation(
      UUID idempotencyKey,
      String xXsrfToken,
      SimulationCreateRequest simulationCreateRequest) {

    UUID callerId = resolveCallerId();
    String requestHash = computeHash(simulationCreateRequest);

    CreateSimulationCommand command = new CreateSimulationCommand(
        idempotencyKey,
        callerId,
        simulationCreateRequest.getName(),
        simulationCreateRequest.getPeriodId(),
        new ArrayList<>(simulationCreateRequest.getProgramIds()),
        requestHash);

    SimulationView view = createSimulationUseCase.createSimulation(command);
    Simulation response = mapper.toDto(view);

    return ResponseEntity.created(URI.create("/me/simulations/" + view.id())).body(response);
  }

  // -------------------------------------------------------------------------
  // GET /me/simulations — list (SIM-002)
  // -------------------------------------------------------------------------

  @Override
  @PreAuthorize("hasRole('STUDENT')")
  public ResponseEntity<SimulationPage> listMySimulations(
      @Nullable UUID periodId,
      @Nullable SimulationStatus status,
      Integer page,
      Integer size,
      @Nullable List<String> sort) {

    UUID callerId = resolveCallerId();
    String statusFilter = status != null ? status.getValue() : null;

    ListSimulationsCriteria criteria = new ListSimulationsCriteria(
        callerId, periodId, statusFilter,
        page != null ? page : 0,
        size != null ? size : 20);

    SimulationPageView pageView = listMySimulationsQuery.listMySimulations(criteria);
    return ResponseEntity.ok(mapper.toPageDto(pageView));
  }

  // -------------------------------------------------------------------------
  // GET /me/simulations/{simulationId} — get full detail (SIM-003, DET-001..003, STALE-001, X-003)
  // -------------------------------------------------------------------------

  @Override
  @PreAuthorize("hasRole('STUDENT')")
  public ResponseEntity<SimulationDetail> getSimulation(UUID simulationId) {
    UUID callerId = resolveCallerId();
    SimulationDetailView detailView = getSimulationQuery.getSimulation(simulationId, callerId);
    SimulationDetail detail = mapper.toDetailDto(detailView);
    return ResponseEntity.ok(detail);
  }

  // -------------------------------------------------------------------------
  // PATCH /me/simulations/{simulationId} — update (SIM-004, X-002, X-003)
  // -------------------------------------------------------------------------

  @Override
  @PreAuthorize("hasRole('STUDENT')")
  public ResponseEntity<Simulation> updateSimulation(
      String xXsrfToken,
      UUID simulationId,
      SimulationUpdateRequest simulationUpdateRequest) {

    UUID callerId = resolveCallerId();
    long version = simulationUpdateRequest.getVersion() != null
        ? simulationUpdateRequest.getVersion()
        : 0L;

    UpdateSimulationCommand command = new UpdateSimulationCommand(
        simulationId, callerId, simulationUpdateRequest.getName(), version);

    SimulationView view = updateSimulationUseCase.updateSimulation(command);
    return ResponseEntity.ok(mapper.toDto(view));
  }

  // -------------------------------------------------------------------------
  // DELETE /me/simulations/{simulationId} — soft delete (SIM-005, X-003)
  // -------------------------------------------------------------------------

  @Override
  @PreAuthorize("hasRole('STUDENT')")
  public ResponseEntity<Void> deleteSimulation(String xXsrfToken, UUID simulationId) {
    UUID callerId = resolveCallerId();
    deleteSimulationUseCase.deleteSimulation(simulationId, callerId);
    return ResponseEntity.noContent().build();
  }

  // -------------------------------------------------------------------------
  // POST /me/simulations/{simulationId}/duplications — duplicate (SIM-006, X-001)
  // -------------------------------------------------------------------------

  @Override
  @PreAuthorize("hasRole('STUDENT')")
  public ResponseEntity<Simulation> duplicateSimulation(
      UUID idempotencyKey,
      String xXsrfToken,
      UUID simulationId,
      @Nullable DuplicateSimulationRequest duplicateSimulationRequest) {

    UUID callerId = resolveCallerId();
    String name = duplicateSimulationRequest != null ? duplicateSimulationRequest.getName() : null;
    String requestHash = computeDuplicateHash(simulationId, name);

    DuplicateSimulationCommand command = new DuplicateSimulationCommand(
        idempotencyKey, simulationId, callerId, name, requestHash);

    SimulationView view = duplicateSimulationUseCase.duplicateSimulation(command);
    return ResponseEntity.created(URI.create("/me/simulations/" + view.id())).body(mapper.toDto(view));
  }

  // -------------------------------------------------------------------------
  // POST .../selections — addSelection (VAL-001, WU4a)
  // -------------------------------------------------------------------------

  @Override
  @PreAuthorize("hasRole('STUDENT')")
  public ResponseEntity<SimulationSelection> addSimulationSelection(
      UUID idempotencyKey,
      String xXsrfToken,
      UUID simulationId,
      SimulationSelectionCreateRequest simulationSelectionCreateRequest) {

    UUID callerId = resolveCallerId();
    boolean acknowledgeWarnings = Boolean.TRUE.equals(
        simulationSelectionCreateRequest.getAcknowledgeWarnings());
    String requestHash = computeSelectionHash(simulationId,
        simulationSelectionCreateRequest.getScheduleOptionId());

    AddSelectionCommand command = new AddSelectionCommand(
        idempotencyKey,
        simulationId,
        callerId,
        simulationSelectionCreateRequest.getScheduleOptionId(),
        acknowledgeWarnings,
        requestHash);

    SimulationSelectionView view = addSimulationSelectionUseCase.addSelection(command);
    SimulationSelection response = mapper.toSelectionDto(view);

    return ResponseEntity
        .created(URI.create("/me/simulations/" + simulationId + "/selections/" + view.id()))
        .body(response);
  }

  // -------------------------------------------------------------------------
  // DELETE .../selections/{selectionId} — removeSelection (VAL-010, WU4a)
  // -------------------------------------------------------------------------

  @Override
  @PreAuthorize("hasRole('STUDENT')")
  public ResponseEntity<Void> removeSimulationSelection(
      String xXsrfToken, UUID simulationId, UUID selectionId) {
    UUID callerId = resolveCallerId();
    removeSimulationSelectionUseCase.removeSelection(simulationId, selectionId, callerId);
    return ResponseEntity.noContent().build();
  }

  // -------------------------------------------------------------------------
  // GET .../selections — listSimulationSelections (VAL-011, WU4a)
  // -------------------------------------------------------------------------

  @Override
  @PreAuthorize("hasRole('STUDENT')")
  public ResponseEntity<List<SimulationSelection>> listSimulationSelections(UUID simulationId) {
    UUID callerId = resolveCallerId();
    List<SimulationSelectionView> views =
        listSimulationSelectionsQuery.listSelections(simulationId, callerId);
    List<SimulationSelection> response = views.stream()
        .map(mapper::toSelectionDto)
        .toList();
    return ResponseEntity.ok(response);
  }

  // -------------------------------------------------------------------------
  // POST .../review — reviewSimulation (STALE-002, WU5)
  // -------------------------------------------------------------------------

  @Override
  @PreAuthorize("hasRole('STUDENT')")
  public ResponseEntity<SimulationDetail> reviewSimulation(String xXsrfToken, UUID simulationId) {
    UUID callerId = resolveCallerId();
    SimulationDetailView detailView = reviewSimulationUseCase.reviewSimulation(simulationId, callerId);
    SimulationDetail detail = mapper.toDetailDto(detailView);
    return ResponseEntity.ok(detail);
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
  // Hash computation for idempotency deduplication
  // -------------------------------------------------------------------------

  private String computeHash(SimulationCreateRequest req) {
    try {
      String input = req.getName() + "|" + req.getPeriodId() + "|"
          + req.getProgramIds().stream().map(UUID::toString).sorted()
              .reduce("", (a, b) -> a + b);
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hash);
    } catch (Exception e) {
      throw new IllegalStateException("Failed to compute request hash", e);
    }
  }

  private String computeDuplicateHash(UUID simulationId, String name) {
    try {
      String input = simulationId + "|" + (name != null ? name : "");
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hash);
    } catch (Exception e) {
      throw new IllegalStateException("Failed to compute duplicate hash", e);
    }
  }

  private String computeSelectionHash(UUID simulationId, UUID scheduleOptionId) {
    try {
      String input = simulationId + "|" + scheduleOptionId;
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hash);
    } catch (Exception e) {
      throw new IllegalStateException("Failed to compute selection hash", e);
    }
  }
}
