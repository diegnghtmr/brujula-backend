package co.edu.uniquindio.brujula.identity.infrastructure.adapter.out.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpMethod.PUT;
import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import co.edu.uniquindio.brujula.identity.application.port.out.KeycloakUserId;
import co.edu.uniquindio.brujula.identity.application.port.out.KeycloakUserProvisioningPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;

/**
 * Offline integration test for {@link KeycloakAdminApiAdapter}.
 *
 * <p>Uses {@link MockRestServiceServer} bound to the underlying {@link RestTemplate} (via {@code
 * RestClient.builder(restTemplate)}) to intercept ALL HTTP calls — both token acquisition and Admin
 * API calls — without a live Keycloak instance.
 *
 * <p>Asserts EXACT Keycloak 26.4 Admin REST API request shapes per obs #1574:
 *
 * <ul>
 *   <li>Token: POST form-urlencoded grant_type=client_credentials
 *   <li>createUser: POST /admin/realms/brujula-dev/users, 201 + Location header
 *   <li>triggerEmailVerification: PUT .../execute-actions-email body ["VERIFY_EMAIL"]
 *   <li>triggerPasswordReset: GET /users?email=... + PUT execute-actions-email ["UPDATE_PASSWORD"]
 * </ul>
 *
 * <p>Strict TDD — tests written BEFORE adapter implementation (RED→GREEN cycle).
 */
class KeycloakAdminApiAdapterTest {

  private static final String REALM = "brujula-dev";
  private static final String KC_BASE = "http://localhost:9090";
  private static final String ADMIN_BASE_URL = KC_BASE + "/admin/realms/" + REALM;
  private static final String TOKEN_URL =
      KC_BASE + "/realms/" + REALM + "/protocol/openid-connect/token";
  private static final String CLIENT_ID = "brujula-registration-svc";
  private static final String CLIENT_SECRET = "test-secret";
  private static final String ACCESS_TOKEN = "fake-access-token-xyz";

  private MockRestServiceServer mockServer;
  private KeycloakAdminApiAdapter adapter;

  @BeforeEach
  void setUp() {
    // A RestTemplate whose HTTP execution is intercepted by MockRestServiceServer.
    var restTemplate = new RestTemplate();
    mockServer = MockRestServiceServer.createServer(restTemplate);

    // RestClient.builder(restTemplate) builds a RestClient that delegates to the RestTemplate,
    // allowing MockRestServiceServer to intercept all requests including absolute URIs.
    var restClientBuilder = RestClient.builder(restTemplate);

    adapter =
        new KeycloakAdminApiAdapter(
            restClientBuilder, ADMIN_BASE_URL, TOKEN_URL, CLIENT_ID, CLIENT_SECRET);
  }

  // -------------------------------------------------------------------------
  // 1. createUser — happy path (201 + Location header → parsed UUID)
  // -------------------------------------------------------------------------

  @Test
  void createUser_shouldPostToKeycloakUsersEndpoint_andReturnExtractedId() {
    stubTokenEndpoint(ACCESS_TOKEN, 300);

    var expectedUserId = "a2f0e8c1-1234-5678-9abc-def012345678";
    mockServer
        .expect(requestTo(ADMIN_BASE_URL + "/users"))
        .andExpect(method(POST))
        .andExpect(header("Authorization", "Bearer " + ACCESS_TOKEN))
        .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
        .andRespond(
            withStatus(HttpStatus.CREATED)
                .headers(locationHeader(ADMIN_BASE_URL + "/users/" + expectedUserId)));

    var command =
        new KeycloakUserProvisioningPort.ProvisionUserCommand(
            "student@uniquindio.edu.co", "Juan Pérez");

    KeycloakUserId result = adapter.createUser(command);

    assertThat(result).isNotNull();
    assertThat(result.value()).isEqualTo(expectedUserId);
    mockServer.verify();
  }

  // -------------------------------------------------------------------------
  // 2. triggerEmailVerification — VERIFY_EMAIL action, 204
  // -------------------------------------------------------------------------

  @Test
  void triggerEmailVerification_shouldPutExecuteActionsEmail_withVerifyEmailAction() {
    stubTokenEndpoint(ACCESS_TOKEN, 300);

    var userId = "a2f0e8c1-1234-5678-9abc-def012345678";
    mockServer
        .expect(requestTo(containsString("/users/" + userId + "/execute-actions-email")))
        .andExpect(method(PUT))
        .andExpect(header("Authorization", "Bearer " + ACCESS_TOKEN))
        .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
        .andExpect(content().string(containsString("VERIFY_EMAIL")))
        .andRespond(withStatus(HttpStatus.NO_CONTENT));

    // Should complete without exception
    adapter.triggerEmailVerification(
        new KeycloakUserId(userId), "http://localhost:5173/verify-email");

    mockServer.verify();
  }

  // -------------------------------------------------------------------------
  // 3. triggerPasswordReset — user lookup by email + UPDATE_PASSWORD action
  // -------------------------------------------------------------------------

  @Test
  void triggerPasswordReset_shouldLookupUserByEmail_thenPutUpdatePasswordAction() {
    stubTokenEndpoint(ACCESS_TOKEN, 300);

    var userId = "b3a1c2d4-5678-1234-abcd-ef0123456789";
    var email = "student@uniquindio.edu.co";

    // Stub GET /users?email=... to return user list
    mockServer
        .expect(requestTo(containsString("/users")))
        .andExpect(method(GET))
        .andExpect(header("Authorization", "Bearer " + ACCESS_TOKEN))
        .andRespond(
            withSuccess(
                "[{\"id\":\"" + userId + "\",\"email\":\"" + email + "\"}]", APPLICATION_JSON));

    // Stub PUT execute-actions-email with UPDATE_PASSWORD
    mockServer
        .expect(requestTo(containsString("/users/" + userId + "/execute-actions-email")))
        .andExpect(method(PUT))
        .andExpect(header("Authorization", "Bearer " + ACCESS_TOKEN))
        .andExpect(content().string(containsString("UPDATE_PASSWORD")))
        .andRespond(withStatus(HttpStatus.NO_CONTENT));

    adapter.triggerPasswordReset(email, "http://localhost:5173/reset-password");

    mockServer.verify();
  }

  // -------------------------------------------------------------------------
  // 4. 409 Conflict — KEYCLOAK_DUPLICATE_USER code
  // -------------------------------------------------------------------------

  @Test
  void createUser_whenKeycloakReturns409_shouldThrowWithDuplicateUserCode() {
    stubTokenEndpoint(ACCESS_TOKEN, 300);

    mockServer
        .expect(requestTo(ADMIN_BASE_URL + "/users"))
        .andExpect(method(POST))
        .andRespond(
            withStatus(HttpStatus.CONFLICT)
                .contentType(APPLICATION_JSON)
                .body("{\"errorMessage\":\"User exists with same email\"}"));

    var command =
        new KeycloakUserProvisioningPort.ProvisionUserCommand(
            "student@uniquindio.edu.co", "Juan Pérez");

    assertThatThrownBy(() -> adapter.createUser(command))
        .isInstanceOf(KeycloakUserProvisioningPort.KeycloakProvisioningException.class)
        .satisfies(
            ex -> {
              var kex = (KeycloakUserProvisioningPort.KeycloakProvisioningException) ex;
              assertThat(kex.getCode()).isEqualTo("KEYCLOAK_DUPLICATE_USER");
            });

    mockServer.verify();
  }

  // -------------------------------------------------------------------------
  // 5. Token caching — token endpoint called ONCE for two createUser calls
  // -------------------------------------------------------------------------

  @Test
  void tokenAcquisition_shouldCacheToken_andNotCallTokenEndpointTwice() {
    // Token endpoint should be called exactly once
    stubTokenEndpointExactly(ACCESS_TOKEN, 300, 1);

    var userId1 = "user-id-one-1234";
    var userId2 = "user-id-two-5678";

    // First createUser call
    mockServer
        .expect(requestTo(ADMIN_BASE_URL + "/users"))
        .andExpect(method(POST))
        .andRespond(
            withStatus(HttpStatus.CREATED)
                .headers(locationHeader(ADMIN_BASE_URL + "/users/" + userId1)));

    // Second createUser call — same token, no additional token request
    mockServer
        .expect(requestTo(ADMIN_BASE_URL + "/users"))
        .andExpect(method(POST))
        .andRespond(
            withStatus(HttpStatus.CREATED)
                .headers(locationHeader(ADMIN_BASE_URL + "/users/" + userId2)));

    var cmd1 =
        new KeycloakUserProvisioningPort.ProvisionUserCommand(
            "student1@uniquindio.edu.co", "Student One");
    var cmd2 =
        new KeycloakUserProvisioningPort.ProvisionUserCommand(
            "student2@uniquindio.edu.co", "Student Two");

    KeycloakUserId id1 = adapter.createUser(cmd1);
    KeycloakUserId id2 = adapter.createUser(cmd2);

    assertThat(id1.value()).isEqualTo(userId1);
    assertThat(id2.value()).isEqualTo(userId2);
    mockServer.verify();
  }

  // -------------------------------------------------------------------------
  // 6. 500 server error → KeycloakProvisioningException
  // -------------------------------------------------------------------------

  @Test
  void createUser_whenKeycloakReturns500_shouldThrowKeycloakProvisioningException() {
    stubTokenEndpoint(ACCESS_TOKEN, 300);

    mockServer
        .expect(requestTo(ADMIN_BASE_URL + "/users"))
        .andExpect(method(POST))
        .andRespond(
            withStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(APPLICATION_JSON)
                .body("{\"error\":\"Internal Server Error\"}"));

    var command =
        new KeycloakUserProvisioningPort.ProvisionUserCommand(
            "student@uniquindio.edu.co", "Juan Pérez");

    assertThatThrownBy(() -> adapter.createUser(command))
        .isInstanceOf(KeycloakUserProvisioningPort.KeycloakProvisioningException.class);

    mockServer.verify();
  }

  // -------------------------------------------------------------------------
  // 7. Token request shape: form-urlencoded grant_type=client_credentials (obs #1574)
  // -------------------------------------------------------------------------

  @Test
  void tokenRequest_shouldUseClientCredentialsGrantType_withFormUrlencodedBody() {
    // Verify EXACT token request shape from obs #1574
    mockServer
        .expect(requestTo(TOKEN_URL))
        .andExpect(method(POST))
        .andExpect(content().contentTypeCompatibleWith(APPLICATION_FORM_URLENCODED))
        .andExpect(
            content()
                .string(
                    allOf(
                        containsString("grant_type=client_credentials"),
                        containsString("client_id=" + CLIENT_ID),
                        containsString("client_secret=" + CLIENT_SECRET))))
        .andRespond(withSuccess(tokenResponse(ACCESS_TOKEN, 300), APPLICATION_JSON));

    mockServer
        .expect(requestTo(ADMIN_BASE_URL + "/users"))
        .andExpect(method(POST))
        .andRespond(
            withStatus(HttpStatus.CREATED)
                .headers(locationHeader(ADMIN_BASE_URL + "/users/some-uuid-id")));

    var command =
        new KeycloakUserProvisioningPort.ProvisionUserCommand(
            "student@uniquindio.edu.co", "Juan Pérez");
    adapter.createUser(command);

    mockServer.verify();
  }

  // -------------------------------------------------------------------------
  // Helpers
  // -------------------------------------------------------------------------

  private void stubTokenEndpoint(String token, int expiresIn) {
    stubTokenEndpointExactly(token, expiresIn, 1);
  }

  private void stubTokenEndpointExactly(String token, int expiresIn, int times) {
    mockServer
        .expect(ExpectedCount.times(times), requestTo(TOKEN_URL))
        .andExpect(method(POST))
        .andRespond(withSuccess(tokenResponse(token, expiresIn), APPLICATION_JSON));
  }

  private static String tokenResponse(String token, int expiresIn) {
    return """
        {"access_token":"%s","expires_in":%d,"token_type":"Bearer"}
        """
        .formatted(token, expiresIn);
  }

  private static HttpHeaders locationHeader(String location) {
    var headers = new HttpHeaders();
    headers.set("Location", location);
    return headers;
  }
}
