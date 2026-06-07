package co.edu.uniquindio.brujula.progress.infrastructure.adapter.in.rest;

import co.edu.uniquindio.brujula.generated.api.SubscriptionsApi;
import co.edu.uniquindio.brujula.generated.model.JourneyRef;
import co.edu.uniquindio.brujula.generated.model.Subscription;
import co.edu.uniquindio.brujula.generated.model.SubscriptionCreateRequest;
import co.edu.uniquindio.brujula.generated.model.SubscriptionUpdateRequest;
import co.edu.uniquindio.brujula.identity.application.dto.UserProfile;
import co.edu.uniquindio.brujula.identity.application.port.in.GetCurrentUserQuery;
import co.edu.uniquindio.brujula.progress.application.dto.CreateSubscriptionCommand;
import co.edu.uniquindio.brujula.progress.application.dto.SubscriptionView;
import co.edu.uniquindio.brujula.progress.application.dto.UpdateSubscriptionCommand;
import co.edu.uniquindio.brujula.progress.application.port.in.CreateSubscriptionUseCase;
import co.edu.uniquindio.brujula.progress.application.port.in.DeleteSubscriptionUseCase;
import co.edu.uniquindio.brujula.progress.application.port.in.GetSubscriptionQuery;
import co.edu.uniquindio.brujula.progress.application.port.in.ListMySubscriptionsQuery;
import co.edu.uniquindio.brujula.progress.application.port.in.UpdateSubscriptionUseCase;
import java.net.URI;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST adapter: student subscription endpoints ({@code /me/subscriptions/**}).
 *
 * <p>Implements the generated {@link SubscriptionsApi} contract-first interface (ARQ-001).
 * Package-private per ARQ-006b. STUDENT-only; STAFF/ADMIN access is rejected via
 * {@code @PreAuthorize("hasRole('STUDENT')")} (CROSS-2).
 *
 * <p>BOLA defence: caller identity always resolved from the session principal, never from a request
 * parameter (TRD §10).
 *
 * <p>Name resolution (programName, planName, journeyName): the catalog look-up is a WU3 concern.
 * For WU2, null names are passed through to the response. The generated Subscription model marks
 * them {@code @NotNull} in the OpenAPI spec, so they are rendered as a placeholder string
 * {@code "N/A"} rather than null to avoid a serialisation validation failure.
 *
 * <p>Satisfies: SUB-1..4, CROSS-2, CROSS-3, CROSS-4. ARQ-001.
 */
@RestController
class SubscriptionController implements SubscriptionsApi {

  private final CreateSubscriptionUseCase createSubscriptionUseCase;
  private final ListMySubscriptionsQuery listMySubscriptionsQuery;
  private final GetSubscriptionQuery getSubscriptionQuery;
  private final UpdateSubscriptionUseCase updateSubscriptionUseCase;
  private final DeleteSubscriptionUseCase deleteSubscriptionUseCase;
  private final GetCurrentUserQuery getCurrentUserQuery;

  SubscriptionController(
      CreateSubscriptionUseCase createSubscriptionUseCase,
      ListMySubscriptionsQuery listMySubscriptionsQuery,
      GetSubscriptionQuery getSubscriptionQuery,
      UpdateSubscriptionUseCase updateSubscriptionUseCase,
      DeleteSubscriptionUseCase deleteSubscriptionUseCase,
      GetCurrentUserQuery getCurrentUserQuery) {
    this.createSubscriptionUseCase = createSubscriptionUseCase;
    this.listMySubscriptionsQuery = listMySubscriptionsQuery;
    this.getSubscriptionQuery = getSubscriptionQuery;
    this.updateSubscriptionUseCase = updateSubscriptionUseCase;
    this.deleteSubscriptionUseCase = deleteSubscriptionUseCase;
    this.getCurrentUserQuery = getCurrentUserQuery;
  }

  // -------------------------------------------------------------------------
  // POST /me/subscriptions — create
  // -------------------------------------------------------------------------

  @Override
  @PreAuthorize("hasRole('STUDENT')")
  public ResponseEntity<Subscription> createSubscription(
      UUID idempotencyKey,
      String xXsrfToken,
      SubscriptionCreateRequest subscriptionCreateRequest) {

    UUID callerId = resolveCallerId();
    String requestHash = computeHash(subscriptionCreateRequest);

    CreateSubscriptionCommand command =
        new CreateSubscriptionCommand(
            idempotencyKey,
            callerId,
            subscriptionCreateRequest.getProgramId(),
            subscriptionCreateRequest.getPlanId(),
            subscriptionCreateRequest.getReferenceJourneyCode(),
            requestHash);

    SubscriptionView view = createSubscriptionUseCase.createSubscription(command);
    Subscription response = toDto(view);

    return ResponseEntity.created(URI.create("/me/subscriptions/" + view.id())).body(response);
  }

  // -------------------------------------------------------------------------
  // GET /me/subscriptions — list
  // -------------------------------------------------------------------------

  @Override
  @PreAuthorize("hasRole('STUDENT')")
  public ResponseEntity<List<Subscription>> listMySubscriptions() {
    UUID callerId = resolveCallerId();
    List<SubscriptionView> views = listMySubscriptionsQuery.listMySubscriptions(callerId);
    List<Subscription> result = views.stream().map(this::toDto).toList();
    return ResponseEntity.ok(result);
  }

  // -------------------------------------------------------------------------
  // GET /me/subscriptions/{subscriptionId} — get one
  // -------------------------------------------------------------------------

  @Override
  @PreAuthorize("hasRole('STUDENT')")
  public ResponseEntity<Subscription> getSubscription(UUID subscriptionId) {
    UUID callerId = resolveCallerId();
    SubscriptionView view = getSubscriptionQuery.getSubscription(subscriptionId, callerId);
    return ResponseEntity.ok(toDto(view));
  }

  // -------------------------------------------------------------------------
  // PATCH /me/subscriptions/{subscriptionId} — update
  // -------------------------------------------------------------------------

  @Override
  @PreAuthorize("hasRole('STUDENT')")
  public ResponseEntity<Subscription> updateSubscription(
      String xXsrfToken,
      UUID subscriptionId,
      SubscriptionUpdateRequest subscriptionUpdateRequest) {

    UUID callerId = resolveCallerId();

    Long versionLong = subscriptionUpdateRequest.getVersion();
    long version = versionLong != null ? versionLong : 0L;

    UpdateSubscriptionCommand command =
        new UpdateSubscriptionCommand(
            subscriptionId,
            callerId,
            subscriptionUpdateRequest.getPlanId(),
            subscriptionUpdateRequest.getReferenceJourneyCode(),
            Boolean.TRUE.equals(subscriptionUpdateRequest.getAcknowledgeHistoryImpact()),
            version);

    SubscriptionView view = updateSubscriptionUseCase.updateSubscription(command);
    return ResponseEntity.ok(toDto(view));
  }

  // -------------------------------------------------------------------------
  // DELETE /me/subscriptions/{subscriptionId} — soft-delete
  // -------------------------------------------------------------------------

  @Override
  @PreAuthorize("hasRole('STUDENT')")
  public ResponseEntity<Void> deleteSubscription(String xXsrfToken, UUID subscriptionId) {
    UUID callerId = resolveCallerId();
    deleteSubscriptionUseCase.deleteSubscription(subscriptionId, callerId);
    return ResponseEntity.noContent().build();
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
  // DTO mapper: SubscriptionView → generated Subscription model (ARQ-005)
  // -------------------------------------------------------------------------

  private Subscription toDto(SubscriptionView view) {
    // WU3-SEAM: programName/planName/journeyName resolved by catalog look-up in WU3.
    // For WU2, placeholder "N/A" is used to satisfy @NotNull constraints in the generated model.
    String programName = view.programName() != null ? view.programName() : "N/A";
    String planName = view.planName() != null ? view.planName() : "N/A";
    String journeyName = view.referenceJourneyName() != null ? view.referenceJourneyName() : "N/A";

    JourneyRef journeyRef = new JourneyRef(view.referenceJourneyCode(), journeyName);

    OffsetDateTime createdAt = view.createdAt() != null
        ? view.createdAt().atOffset(ZoneOffset.UTC)
        : OffsetDateTime.now(ZoneOffset.UTC);

    return new Subscription(
        view.id(),
        view.programId(),
        programName,
        view.planId(),
        planName,
        journeyRef,
        view.version(),
        createdAt);
  }

  // -------------------------------------------------------------------------
  // Request hash computation (for idempotency deduplication)
  // -------------------------------------------------------------------------

  /**
   * SHA-256 hex digest of the deterministic request body fields.
   *
   * <p>FIX-W5: replaces the previous {@code Objects.hash()} (32-bit int, high collision risk) with
   * SHA-256 (256-bit), matching the approach in {@code AdministrationController.hashRequest()}.
   * The input is a stable concatenation of programId + planId + referenceJourneyCode so that two
   * distinct bodies always produce distinct hash values.
   */
  private String computeHash(SubscriptionCreateRequest req) {
    try {
      String input =
          req.getProgramId() + "|" + req.getPlanId() + "|" + req.getReferenceJourneyCode();
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hash);
    } catch (Exception e) {
      throw new IllegalStateException("Failed to compute request hash", e);
    }
  }
}
