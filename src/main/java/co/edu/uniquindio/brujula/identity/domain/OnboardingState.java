package co.edu.uniquindio.brujula.identity.domain;

/**
 * Onboarding lifecycle state for an AppUser.
 *
 * <p>State machine:
 *
 * <pre>
 *   PENDING_VERIFICATION — registered via POST /auth/registration; email not yet verified.
 *   ACTIVE               — email verified (or OIDC-provisioned); can access academic functions.
 * </pre>
 *
 * <p>Transitions:
 *
 * <ul>
 *   <li>OIDC-provisioned users start directly in ACTIVE (they authenticated via Keycloak OIDC).
 *   <li>Self-registered students start in PENDING_VERIFICATION; transition to ACTIVE when the
 *       backend detects email_verified=true on the next OIDC login (sync-on-login pattern).
 * </ul>
 *
 * <p>Maps to the {@code onboarding_state} column in {@code identity.app_user}.
 */
public enum OnboardingState {

  /**
   * Student completed self-registration but has not yet verified their email address. Cannot access
   * academic functions.
   */
  PENDING_VERIFICATION,

  /**
   * User's email is verified (or provisioned via OIDC login). Full access to permitted features
   * granted.
   */
  ACTIVE
}
