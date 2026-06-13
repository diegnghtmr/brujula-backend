package co.edu.uniquindio.brujula.identity.application.port.out;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/**
 * Contract test (compile-time + behavioral) for {@link KeycloakUserProvisioningPort}.
 *
 * <p>Uses a {@link FakeKeycloakProvisioningAdapter} in-memory stub to verify the port interface
 * compiles, has the correct method signatures, and delegates correctly. No Spring context needed —
 * pure JUnit 5 test (application layer is framework-free).
 *
 * <p>Strict TDD RED phase: this test was written BEFORE the port interface existed.
 */
class KeycloakUserProvisioningPortContractTest {

  // -------------------------------------------------------------------------
  // In-memory test double (stub)
  // -------------------------------------------------------------------------

  private static class FakeKeycloakProvisioningAdapter implements KeycloakUserProvisioningPort {

    private ProvisionUserCommand lastCreateCommand;
    private KeycloakUserId lastTriggeredUserId;
    private String lastPasswordResetEmail;
    private boolean shouldThrow;

    @Override
    public KeycloakUserId createUser(ProvisionUserCommand command) {
      this.lastCreateCommand = command;
      if (shouldThrow) {
        throw new KeycloakUserProvisioningPort.KeycloakProvisioningException(
            "Keycloak error", "KEYCLOAK_ERROR");
      }
      return new KeycloakUserId("fake-keycloak-id-123");
    }

    @Override
    public void triggerEmailVerification(KeycloakUserId userId, String redirectUri) {
      this.lastTriggeredUserId = userId;
      if (shouldThrow) {
        throw new KeycloakUserProvisioningPort.KeycloakProvisioningException(
            "Keycloak error", "KEYCLOAK_ERROR");
      }
    }

    @Override
    public void triggerPasswordReset(String email, String redirectUri) {
      this.lastPasswordResetEmail = email;
      if (shouldThrow) {
        throw new KeycloakUserProvisioningPort.KeycloakProvisioningException(
            "Keycloak error", "KEYCLOAK_ERROR");
      }
    }

    @Override
    public void resendEmailVerification(String email, String redirectUri) {
      if (shouldThrow) {
        throw new KeycloakUserProvisioningPort.KeycloakProvisioningException(
            "Keycloak error", "KEYCLOAK_ERROR");
      }
    }
  }

  // -------------------------------------------------------------------------
  // Tests
  // -------------------------------------------------------------------------

  @Test
  void createUser_returnsKeycloakUserId() {
    var adapter = new FakeKeycloakProvisioningAdapter();
    var command =
        new KeycloakUserProvisioningPort.ProvisionUserCommand(
            "student@uniquindio.edu.co", "Juan Pérez");

    var userId = adapter.createUser(command);

    assertThat(userId).isNotNull();
    assertThat(userId.value()).isNotBlank();
    assertThat(adapter.lastCreateCommand).isEqualTo(command);
  }

  @Test
  void triggerEmailVerification_delegatesToAdapter() {
    var adapter = new FakeKeycloakProvisioningAdapter();
    var userId = new KeycloakUserId("user-id-abc");

    adapter.triggerEmailVerification(userId, "http://localhost:5173/verify-email");

    assertThat(adapter.lastTriggeredUserId).isEqualTo(userId);
  }

  @Test
  void triggerPasswordReset_delegatesToAdapter() {
    var adapter = new FakeKeycloakProvisioningAdapter();

    adapter.triggerPasswordReset("student@uniquindio.edu.co", "http://localhost:5173/reset");

    assertThat(adapter.lastPasswordResetEmail).isEqualTo("student@uniquindio.edu.co");
  }

  @Test
  void createUser_whenAdapterThrows_propagatesKeycloakProvisioningException() {
    var adapter = new FakeKeycloakProvisioningAdapter();
    adapter.shouldThrow = true;
    var command =
        new KeycloakUserProvisioningPort.ProvisionUserCommand(
            "student@uniquindio.edu.co", "Juan Pérez");

    assertThatThrownBy(() -> adapter.createUser(command))
        .isInstanceOf(KeycloakUserProvisioningPort.KeycloakProvisioningException.class)
        .hasMessageContaining("Keycloak error");
  }

  @Test
  void provisionUserCommand_isValueObject_withCorrectFields() {
    var command =
        new KeycloakUserProvisioningPort.ProvisionUserCommand(
            "test@uniquindio.edu.co", "Test User");

    assertThat(command.email()).isEqualTo("test@uniquindio.edu.co");
    assertThat(command.fullName()).isEqualTo("Test User");
  }

  @Test
  void keycloakUserId_isValueObject_withCorrectField() {
    var userId = new KeycloakUserId("some-uuid-value");

    assertThat(userId.value()).isEqualTo("some-uuid-value");
  }

  @Test
  void keycloakProvisioningException_hasCodeAndMessage() {
    var ex =
        new KeycloakUserProvisioningPort.KeycloakProvisioningException(
            "User already exists", "KEYCLOAK_DUPLICATE_USER");

    assertThat(ex.getMessage()).isEqualTo("User already exists");
    assertThat(ex.getCode()).isEqualTo("KEYCLOAK_DUPLICATE_USER");
  }
}
