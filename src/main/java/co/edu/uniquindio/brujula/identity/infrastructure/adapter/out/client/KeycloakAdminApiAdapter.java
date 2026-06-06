package co.edu.uniquindio.brujula.identity.infrastructure.adapter.out.client;

import co.edu.uniquindio.brujula.identity.application.port.out.KeycloakUserId;
import co.edu.uniquindio.brujula.identity.application.port.out.KeycloakUserProvisioningPort;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;

/**
 * Driven adapter implementing {@link KeycloakUserProvisioningPort} via Keycloak Admin REST API.
 *
 * <p>Uses Spring {@link RestClient} (Boot 4) with client-credentials token acquisition. Token is
 * cached in an {@link AtomicReference} and refreshed ~30 seconds before expiry to avoid clock-skew
 * races and 401s under load.
 *
 * <p>Keycloak 26.4 endpoint shapes (obs #1574):
 *
 * <ul>
 *   <li>Create user: {@code POST /admin/realms/{realm}/users} — 201 + {@code Location} header.
 *   <li>Execute actions: {@code PUT .../users/{id}/execute-actions-email} — body = JSON array,
 *       query params {@code redirectUri}, {@code clientId}. 204.
 *   <li>Token: {@code POST /realms/{realm}/protocol/openid-connect/token} — form-urlencoded {@code
 *       grant_type=client_credentials}. 200 with {@code access_token} + {@code expires_in}.
 * </ul>
 *
 * <p>Token caching strategy: AtomicReference CAS — thread-safe without locking. Both token and
 * admin API requests share the same underlying RestClient so tests can intercept with a single
 * {@code MockRestServiceServer}.
 *
 * <p>Registered as a bean via {@link KeycloakAdminApiConfiguration}.
 */
public class KeycloakAdminApiAdapter implements KeycloakUserProvisioningPort {

  private static final Logger log = LoggerFactory.getLogger(KeycloakAdminApiAdapter.class);

  /** Refresh token this many seconds before the stated expiry to avoid clock-skew races. */
  private static final int REFRESH_BUFFER_SECONDS = 30;

  /** Shared RestClient — used for both token acquisition and admin API calls. */
  private final RestClient restClient;

  private final String adminBaseUrl;
  private final String tokenUrl;
  private final String clientId;
  private final String clientSecret;

  /** Cached token state. {@code null} means no token has been acquired yet. */
  private final AtomicReference<CachedToken> cachedToken = new AtomicReference<>();

  /**
   * Constructor for explicit instantiation (by {@link KeycloakAdminApiConfiguration} or tests).
   *
   * <p>A single {@link RestClient.Builder} is used to create the shared RestClient. This ensures
   * that {@code MockRestServiceServer} can intercept ALL HTTP calls (both token and admin API) from
   * the same underlying transport.
   *
   * @param restClientBuilder the builder — tests inject a builder backed by a tracked RestTemplate
   * @param adminBaseUrl base URL for admin API, e.g. {@code
   *     http://localhost:9090/admin/realms/brujula-dev}
   * @param tokenUrl full token endpoint URL, e.g. {@code
   *     http://localhost:9090/realms/brujula-dev/protocol/openid-connect/token}
   * @param clientId the service-account client ID ({@code brujula-registration-svc})
   * @param clientSecret the client secret (from environment — never hard-coded)
   */
  public KeycloakAdminApiAdapter(
      RestClient.Builder restClientBuilder,
      String adminBaseUrl,
      String tokenUrl,
      String clientId,
      String clientSecret) {
    // Build WITHOUT a baseUrl so the adapter can call both adminBaseUrl and tokenUrl with
    // absolute URIs. Tests bind MockRestServiceServer to the underlying RestTemplate, which
    // intercepts all requests regardless of host.
    this.restClient = restClientBuilder.build();
    this.adminBaseUrl = adminBaseUrl;
    this.tokenUrl = tokenUrl;
    this.clientId = clientId;
    this.clientSecret = clientSecret;
  }

  // -------------------------------------------------------------------------
  // Port implementation
  // -------------------------------------------------------------------------

  /** {@inheritDoc} */
  @Override
  public KeycloakUserId createUser(ProvisionUserCommand command) {
    var token = resolveToken();

    // Split fullName into firstName + lastName heuristically.
    var nameParts = splitName(command.fullName());

    // Build the create-user request body per Keycloak 26.4 API shape (obs #1574).
    var requestBody =
        new CreateUserRequest(
            command.email(), // username = email (institutional address is unique)
            command.email(),
            nameParts[0],
            nameParts[1],
            true, // enabled=true
            false // emailVerified=false — Keycloak sets this after VERIFY_EMAIL action
            );

    URI location =
        restClient
            .post()
            .uri(adminBaseUrl + "/users")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .body(requestBody)
            .retrieve()
            .onStatus(
                status -> status.value() == 409,
                (req, resp) -> {
                  throw new KeycloakProvisioningException(
                      "Keycloak returned 409 Conflict — user already exists with this email",
                      "KEYCLOAK_DUPLICATE_USER");
                })
            .onStatus(
                HttpStatusCode::isError,
                (req, resp) -> {
                  throw new KeycloakProvisioningException(
                      "Keycloak returned " + resp.getStatusCode() + " when creating user",
                      "KEYCLOAK_" + resp.getStatusCode().value());
                })
            .toBodilessEntity()
            .getHeaders()
            .getLocation();

    if (location == null) {
      throw new KeycloakProvisioningException(
          "Keycloak create-user returned 201 but no Location header", "KEYCLOAK_NO_LOCATION");
    }

    // Extract user UUID from Location URI tail: .../users/{uuid}
    var path = location.getPath();
    var userId = path.substring(path.lastIndexOf('/') + 1);
    log.debug("Keycloak user created: {}", userId);
    return new KeycloakUserId(userId);
  }

  /**
   * {@inheritDoc}
   *
   * <p>OPTION A (obs #1574): sends {@code ["VERIFY_EMAIL","UPDATE_PASSWORD"]} so the user both
   * verifies their email and sets their initial password on Keycloak's page in one flow. The
   * backend never handles a password — Keycloak stores the credential exclusively (DT-011).
   */
  @Override
  public void triggerEmailVerification(KeycloakUserId userId, String redirectUri) {
    var token = resolveToken();

    restClient
        .put()
        .uri(
            adminBaseUrl
                + "/users/{id}/execute-actions-email?redirectUri={redirectUri}&clientId={clientId}",
            userId.value(),
            redirectUri,
            // SPA public client — its redirect URIs must include redirectUri in the realm config.
            "brujula-frontend")
        .header("Authorization", "Bearer " + token)
        .contentType(MediaType.APPLICATION_JSON)
        // OPTION A (obs #1574): VERIFY_EMAIL sets emailVerified=true; UPDATE_PASSWORD prompts the
        // user to set their initial password on Keycloak's page after clicking the email link.
        // Without UPDATE_PASSWORD the Keycloak user has no credential and cannot log in.
        .body(List.of("VERIFY_EMAIL", "UPDATE_PASSWORD"))
        .retrieve()
        .onStatus(
            HttpStatusCode::isError,
            (req, resp) -> {
              throw new KeycloakProvisioningException(
                  "Keycloak returned "
                      + resp.getStatusCode()
                      + " for execute-actions-email"
                      + " [VERIFY_EMAIL, UPDATE_PASSWORD] for user "
                      + userId.value(),
                  "KEYCLOAK_" + resp.getStatusCode().value());
            })
        .toBodilessEntity();

    log.debug("Email verification triggered for Keycloak user: {}", userId.value());
  }

  /** {@inheritDoc} */
  @Override
  public void triggerPasswordReset(String email, String redirectUri) {
    var token = resolveToken();

    // Step 1: look up user by email — GET /users?email={email}&exact=true
    var users =
        restClient
            .get()
            .uri(adminBaseUrl + "/users?email={email}&exact=true", email)
            .header("Authorization", "Bearer " + token)
            .retrieve()
            .onStatus(
                HttpStatusCode::isError,
                (req, resp) -> {
                  throw new KeycloakProvisioningException(
                      "Keycloak returned "
                          + resp.getStatusCode()
                          + " when looking up user by email",
                      "KEYCLOAK_" + resp.getStatusCode().value());
                })
            .body(KeycloakUserSearchResult[].class);

    if (users == null || users.length == 0) {
      // No user found — return silently (no user enumeration).
      log.warn(
          "triggerPasswordReset: no Keycloak user found for email {} (silently suppressed)", email);
      return;
    }

    var keycloakUserId = users[0].id();
    log.debug("Found Keycloak user {} for password reset", keycloakUserId);

    // Step 2: trigger UPDATE_PASSWORD action
    restClient
        .put()
        .uri(
            adminBaseUrl
                + "/users/{id}/execute-actions-email?redirectUri={redirectUri}&clientId={clientId}",
            keycloakUserId,
            redirectUri,
            "brujula-frontend")
        .header("Authorization", "Bearer " + token)
        .contentType(MediaType.APPLICATION_JSON)
        .body(List.of("UPDATE_PASSWORD"))
        .retrieve()
        .onStatus(
            HttpStatusCode::isError,
            (req, resp) -> {
              throw new KeycloakProvisioningException(
                  "Keycloak returned "
                      + resp.getStatusCode()
                      + " for execute-actions-email"
                      + " [UPDATE_PASSWORD] for user "
                      + keycloakUserId,
                  "KEYCLOAK_" + resp.getStatusCode().value());
            })
        .toBodilessEntity();

    log.debug("Password reset triggered for Keycloak user: {}", keycloakUserId);
  }

  // -------------------------------------------------------------------------
  // Token management
  // -------------------------------------------------------------------------

  /**
   * Resolve a valid access token, using the cache if still valid.
   *
   * <p>F-004 RESOLVED: Uses CAS on the {@link AtomicReference} to make the slot update atomic. If
   * two threads detect expiry simultaneously, BOTH will call {@link #acquireToken()} — the CAS
   * ensures only one token wins the slot, but does NOT prevent dual token acquisition. This is
   * acceptable: Keycloak handles duplicate client-credentials tokens fine, and the window is very
   * small. A locking strategy would prevent the double call at the cost of contention; given the
   * low concurrency of token refreshes (once per ~5–10 minutes), the current approach is correct.
   */
  private String resolveToken() {
    var current = cachedToken.get();
    if (current != null && current.isValid()) {
      return current.accessToken();
    }
    var newToken = acquireToken();
    cachedToken.compareAndSet(current, newToken);
    return cachedToken.get().accessToken();
  }

  /**
   * Acquire a new access token via client-credentials grant (obs #1574).
   *
   * <p>POST {tokenUrl} form-urlencoded: grant_type=client_credentials, client_id, client_secret.
   */
  private CachedToken acquireToken() {
    var form = new LinkedMultiValueMap<String, String>();
    form.add("grant_type", "client_credentials");
    form.add("client_id", clientId);
    form.add("client_secret", clientSecret);

    var response =
        restClient
            .post()
            .uri(tokenUrl)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(form)
            .retrieve()
            .onStatus(
                HttpStatusCode::isError,
                (req, resp) -> {
                  throw new KeycloakProvisioningException(
                      "Failed to acquire Keycloak access token: HTTP " + resp.getStatusCode(),
                      "KEYCLOAK_TOKEN_ERROR");
                })
            .body(TokenResponse.class);

    if (response == null || response.access_token() == null) {
      throw new KeycloakProvisioningException(
          "Keycloak token response was empty or missing access_token", "KEYCLOAK_TOKEN_EMPTY");
    }

    var expiresAt =
        Instant.now().plusSeconds(Math.max(0, response.expires_in() - REFRESH_BUFFER_SECONDS));
    log.debug("Acquired Keycloak admin token, valid until approximately: {}", expiresAt);
    return new CachedToken(response.access_token(), expiresAt);
  }

  // -------------------------------------------------------------------------
  // Internal records
  // -------------------------------------------------------------------------

  private record CachedToken(String accessToken, Instant expiresAt) {

    boolean isValid() {
      return Instant.now().isBefore(expiresAt);
    }
  }

  /** Keycloak token response shape (obs #1574: access_token + expires_in). */
  private record TokenResponse(String access_token, int expires_in, String token_type) {}

  /** Create-user request body shape per Keycloak 26.4 Admin API (obs #1574). */
  private record CreateUserRequest(
      String username,
      String email,
      String firstName,
      String lastName,
      boolean enabled,
      boolean emailVerified) {}

  /** Keycloak user search result shape (subset of GET /users response). */
  private record KeycloakUserSearchResult(String id, String email) {}

  // -------------------------------------------------------------------------
  // Helpers
  // -------------------------------------------------------------------------

  /**
   * Split a full name into [firstName, lastName].
   *
   * <p>Heuristic: first word = firstName, remainder = lastName. If only one word, lastName = "".
   */
  private static String[] splitName(String fullName) {
    var trimmed = fullName.trim();
    var spaceIdx = trimmed.indexOf(' ');
    if (spaceIdx < 0) {
      return new String[] {trimmed, ""};
    }
    return new String[] {trimmed.substring(0, spaceIdx), trimmed.substring(spaceIdx + 1).trim()};
  }
}
