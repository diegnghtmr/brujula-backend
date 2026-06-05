package co.edu.uniquindio.brujula.identity.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * RED → GREEN domain unit tests for AppUser aggregate.
 *
 * <p>Pure JUnit 5 + AssertJ — zero Spring context (ARQ-002).
 *
 * <p>Invariants tested: oidcSubject required, email required, name required, default role STUDENT,
 * onboardingState PENDING on create, optimistic version starts at 0, status ACTIVE on first
 * provisioning.
 *
 * <p>Satisfies: S1-B-1, Spec §Identity Provisioning, Design §2.1.
 */
class AppUserTest {

  // -------------------------------------------------------------------------
  // Factory: happy path
  // -------------------------------------------------------------------------

  @Test
  void provision_creates_user_with_required_fields() {
    var user =
        AppUser.provision("sub-abc", "student@uniquindio.edu.co", "Alice Pérez", Role.STUDENT);

    assertThat(user.id()).isNotNull();
    assertThat(user.oidcSubject()).isEqualTo("sub-abc");
    assertThat(user.email()).isEqualTo("student@uniquindio.edu.co");
    assertThat(user.fullName()).isEqualTo("Alice Pérez");
    assertThat(user.role()).isEqualTo(Role.STUDENT);
  }

  @Test
  void provision_sets_onboarding_pending_by_default() {
    var user = AppUser.provision("sub-1", "s@uniquindio.edu.co", "Name", Role.STUDENT);
    assertThat(user.onboardingCompleted()).isFalse();
  }

  @Test
  void provision_sets_status_active() {
    var user = AppUser.provision("sub-1", "s@uniquindio.edu.co", "Name", Role.STUDENT);
    assertThat(user.status()).isEqualTo(AccountStatus.ACTIVE);
  }

  @Test
  void provision_sets_version_zero() {
    var user = AppUser.provision("sub-1", "s@uniquindio.edu.co", "Name", Role.STUDENT);
    assertThat(user.version()).isZero();
  }

  @Test
  void each_provisioned_user_gets_unique_id() {
    var u1 = AppUser.provision("sub-1", "a@uniquindio.edu.co", "Alice", Role.STUDENT);
    var u2 = AppUser.provision("sub-2", "b@uniquindio.edu.co", "Bob", Role.STUDENT);
    assertThat(u1.id()).isNotEqualTo(u2.id());
  }

  // -------------------------------------------------------------------------
  // Invariant: oidcSubject required
  // -------------------------------------------------------------------------

  @Test
  void provision_throws_when_oidc_subject_is_null() {
    assertThatThrownBy(() -> AppUser.provision(null, "s@uniquindio.edu.co", "Name", Role.STUDENT))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("oidcSubject");
  }

  @Test
  void provision_throws_when_oidc_subject_is_blank() {
    assertThatThrownBy(() -> AppUser.provision("  ", "s@uniquindio.edu.co", "Name", Role.STUDENT))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("oidcSubject");
  }

  // -------------------------------------------------------------------------
  // Invariant: email required
  // -------------------------------------------------------------------------

  @Test
  void provision_throws_when_email_is_null() {
    assertThatThrownBy(() -> AppUser.provision("sub-1", null, "Name", Role.STUDENT))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("email");
  }

  @Test
  void provision_throws_when_email_is_blank() {
    assertThatThrownBy(() -> AppUser.provision("sub-1", "   ", "Name", Role.STUDENT))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("email");
  }

  // -------------------------------------------------------------------------
  // Invariant: full name required
  // -------------------------------------------------------------------------

  @Test
  void provision_throws_when_full_name_is_null() {
    assertThatThrownBy(() -> AppUser.provision("sub-1", "s@uniquindio.edu.co", null, Role.STUDENT))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("fullName");
  }

  @Test
  void provision_throws_when_full_name_is_blank() {
    assertThatThrownBy(() -> AppUser.provision("sub-1", "s@uniquindio.edu.co", "   ", Role.STUDENT))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("fullName");
  }

  // -------------------------------------------------------------------------
  // Invariant: role required
  // -------------------------------------------------------------------------

  @Test
  void provision_throws_when_role_is_null() {
    assertThatThrownBy(() -> AppUser.provision("sub-1", "s@uniquindio.edu.co", "Name", null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("role");
  }

  // -------------------------------------------------------------------------
  // Reconstitution from persistence
  // -------------------------------------------------------------------------

  @Test
  void reconstitute_restores_all_fields() {
    UUID id = UUID.randomUUID();
    var user =
        AppUser.reconstitute(
            id,
            "sub-xyz",
            "u@uniquindio.edu.co",
            "Bob Smith",
            Role.ADMIN,
            AccountStatus.INACTIVE,
            true,
            2L);

    assertThat(user.id()).isEqualTo(id);
    assertThat(user.oidcSubject()).isEqualTo("sub-xyz");
    assertThat(user.email()).isEqualTo("u@uniquindio.edu.co");
    assertThat(user.fullName()).isEqualTo("Bob Smith");
    assertThat(user.role()).isEqualTo(Role.ADMIN);
    assertThat(user.status()).isEqualTo(AccountStatus.INACTIVE);
    assertThat(user.onboardingCompleted()).isTrue();
    assertThat(user.version()).isEqualTo(2L);
  }

  // -------------------------------------------------------------------------
  // Role value object
  // -------------------------------------------------------------------------

  @Test
  void role_values_cover_all_spec_roles() {
    assertThat(Role.values()).containsExactlyInAnyOrder(Role.STUDENT, Role.STAFF, Role.ADMIN);
  }

  // -------------------------------------------------------------------------
  // AccountStatus value object
  // -------------------------------------------------------------------------

  @Test
  void account_status_covers_spec_states() {
    assertThat(AccountStatus.values())
        .containsExactlyInAnyOrder(
            AccountStatus.ACTIVE, AccountStatus.INACTIVE, AccountStatus.PENDING_VERIFICATION);
  }
}
