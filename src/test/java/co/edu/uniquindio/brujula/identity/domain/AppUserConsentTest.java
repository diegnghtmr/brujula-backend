package co.edu.uniquindio.brujula.identity.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.Test;

/**
 * RED → GREEN domain unit tests for AppUser consent capture and OnboardingState transitions.
 *
 * <p>Pure JUnit 5 + AssertJ — zero Spring context.
 *
 * <p>Invariants tested:
 *
 * <ul>
 *   <li>register factory method creates user with onboardingState=PENDING_VERIFICATION
 *   <li>captureConsent stores timestamp + termsVersion + privacyVersion
 *   <li>captureConsent validates null inputs
 *   <li>activate() transitions PENDING_VERIFICATION → ACTIVE
 *   <li>activate() on an already-ACTIVE user is idempotent (no-op)
 *   <li>OnboardingState enum covers all states
 * </ul>
 */
class AppUserConsentTest {

  // -------------------------------------------------------------------------
  // register factory — PENDING_VERIFICATION default
  // -------------------------------------------------------------------------

  @Test
  void register_creates_user_with_pending_verification_state() {
    var user = AppUser.register("student@uniquindio.edu.co", "Juan Pérez");

    assertThat(user.onboardingState()).isEqualTo(OnboardingState.PENDING_VERIFICATION);
  }

  @Test
  void register_creates_user_with_student_role_by_default() {
    var user = AppUser.register("student@uniquindio.edu.co", "Juan Pérez");

    assertThat(user.role()).isEqualTo(Role.STUDENT);
  }

  @Test
  void register_creates_user_with_active_account_status() {
    var user = AppUser.register("student@uniquindio.edu.co", "Juan Pérez");

    assertThat(user.status()).isEqualTo(AccountStatus.ACTIVE);
  }

  @Test
  void register_creates_user_with_null_oidc_subject() {
    // Self-registered students do not have an oidcSubject until they log in via OIDC
    var user = AppUser.register("student@uniquindio.edu.co", "Juan Pérez");

    assertThat(user.oidcSubject()).isNull();
  }

  @Test
  void register_throws_when_email_is_null() {
    assertThatThrownBy(() -> AppUser.register(null, "Juan Pérez"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("email");
  }

  @Test
  void register_throws_when_email_is_blank() {
    assertThatThrownBy(() -> AppUser.register("  ", "Juan Pérez"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("email");
  }

  @Test
  void register_throws_when_full_name_is_null() {
    assertThatThrownBy(() -> AppUser.register("s@uniquindio.edu.co", null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("fullName");
  }

  // -------------------------------------------------------------------------
  // captureConsent
  // -------------------------------------------------------------------------

  @Test
  void captureConsent_stores_all_three_fields() {
    var user = AppUser.register("student@uniquindio.edu.co", "Juan Pérez");
    var now = Instant.now();

    user.captureConsent("1.0", "1.0", now);

    assertThat(user.termsVersion()).isEqualTo("1.0");
    assertThat(user.privacyVersion()).isEqualTo("1.0");
    assertThat(user.consentTimestamp()).isEqualTo(now);
  }

  @Test
  void captureConsent_throws_when_terms_version_is_null() {
    var user = AppUser.register("student@uniquindio.edu.co", "Juan Pérez");

    assertThatThrownBy(() -> user.captureConsent(null, "1.0", Instant.now()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("termsVersion");
  }

  @Test
  void captureConsent_throws_when_terms_version_is_blank() {
    var user = AppUser.register("student@uniquindio.edu.co", "Juan Pérez");

    assertThatThrownBy(() -> user.captureConsent("  ", "1.0", Instant.now()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("termsVersion");
  }

  @Test
  void captureConsent_throws_when_privacy_version_is_null() {
    var user = AppUser.register("student@uniquindio.edu.co", "Juan Pérez");

    assertThatThrownBy(() -> user.captureConsent("1.0", null, Instant.now()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("privacyVersion");
  }

  @Test
  void captureConsent_throws_when_timestamp_is_null() {
    var user = AppUser.register("student@uniquindio.edu.co", "Juan Pérez");

    assertThatThrownBy(() -> user.captureConsent("1.0", "1.0", null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("consentTimestamp");
  }

  // -------------------------------------------------------------------------
  // activate() — PENDING_VERIFICATION → ACTIVE
  // -------------------------------------------------------------------------

  @Test
  void activate_transitions_pending_verification_to_active() {
    var user = AppUser.register("student@uniquindio.edu.co", "Juan Pérez");
    assertThat(user.onboardingState()).isEqualTo(OnboardingState.PENDING_VERIFICATION);

    user.activate();

    assertThat(user.onboardingState()).isEqualTo(OnboardingState.ACTIVE);
  }

  @Test
  void activate_on_already_active_user_is_idempotent() {
    // Reconstituted ACTIVE user — activate() should not throw
    var user =
        AppUser.reconstitute(
            java.util.UUID.randomUUID(),
            "sub-xyz",
            "u@uniquindio.edu.co",
            "Bob Smith",
            Role.STUDENT,
            AccountStatus.ACTIVE,
            false,
            1L,
            OnboardingState.ACTIVE,
            null,
            null,
            null);

    // Must not throw — idempotent
    user.activate();

    assertThat(user.onboardingState()).isEqualTo(OnboardingState.ACTIVE);
  }

  // -------------------------------------------------------------------------
  // OnboardingState enum coverage
  // -------------------------------------------------------------------------

  @Test
  void onboarding_state_enum_covers_all_spec_states() {
    assertThat(OnboardingState.values())
        .containsExactlyInAnyOrder(OnboardingState.PENDING_VERIFICATION, OnboardingState.ACTIVE);
  }

  // -------------------------------------------------------------------------
  // linkOidcSubject — C-002 domain invariant
  // -------------------------------------------------------------------------

  @Test
  void linkOidcSubject_sets_subject_on_null_subject_user() {
    // register() creates a user with oidcSubject=null
    var user = AppUser.register("student@uniquindio.edu.co", "Juan Pérez");
    assertThat(user.oidcSubject()).isNull();

    user.linkOidcSubject("real-sub-uuid");

    assertThat(user.oidcSubject()).isEqualTo("real-sub-uuid");
  }

  @Test
  void linkOidcSubject_throws_when_subject_already_set() {
    // A user that already has a subject (e.g. reconstituted after linking)
    var user =
        AppUser.reconstitute(
            java.util.UUID.randomUUID(),
            "existing-sub",
            "u@uniquindio.edu.co",
            "Bob Smith",
            Role.STUDENT,
            AccountStatus.ACTIVE,
            false,
            1L,
            OnboardingState.ACTIVE,
            null,
            null,
            null);

    assertThatThrownBy(() -> user.linkOidcSubject("another-sub"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("already linked");
  }

  @Test
  void linkOidcSubject_throws_when_subject_is_null() {
    var user = AppUser.register("student@uniquindio.edu.co", "Juan Pérez");

    assertThatThrownBy(() -> user.linkOidcSubject(null))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void linkOidcSubject_throws_when_subject_is_blank() {
    var user = AppUser.register("student@uniquindio.edu.co", "Juan Pérez");

    assertThatThrownBy(() -> user.linkOidcSubject("   "))
        .isInstanceOf(IllegalArgumentException.class);
  }

  // -------------------------------------------------------------------------
  // provision factory — still ACTIVE by default (OIDC flow unchanged)
  // -------------------------------------------------------------------------

  @Test
  void provision_via_oidc_gets_active_onboarding_state() {
    var user = AppUser.provision("sub-123", "s@uniquindio.edu.co", "Student", Role.STUDENT);

    assertThat(user.onboardingState()).isEqualTo(OnboardingState.ACTIVE);
  }
}
