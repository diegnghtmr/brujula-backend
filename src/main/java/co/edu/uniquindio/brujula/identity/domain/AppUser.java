package co.edu.uniquindio.brujula.identity.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * AppUser — the central aggregate of the identity module.
 *
 * <p>Represents a provisioned user whose identity is backed by Keycloak OIDC. Zero Spring/JPA
 * imports. All invariants are enforced in factory methods and business methods.
 *
 * <p>Fields: id, oidcSubject, email, fullName, Role VO, AccountStatus, OnboardingState, consent
 * fields (termsVersion, privacyVersion, consentTimestamp), onboardingCompleted (legacy), version
 * (optimistic lock). Email is immutable post-create.
 *
 * <p>Three factory methods:
 *
 * <ul>
 *   <li>{@link #provision} — first-login OIDC creation; sets defaults (ACTIVE state).
 *   <li>{@link #register} — self-registration via POST /auth/registration; sets
 *       PENDING_VERIFICATION state. Consent captured separately via {@link #captureConsent}.
 *   <li>{@link #reconstitute} — restores from persistence without default logic.
 * </ul>
 *
 * <p>State transitions:
 *
 * <ul>
 *   <li>{@link #captureConsent} — records consent evidence on a registered user.
 *   <li>{@link #activate} — transitions PENDING_VERIFICATION → ACTIVE (sync-on-login).
 * </ul>
 */
public final class AppUser {

  private final UUID id;
  private String
      oidcSubject; // null for self-registered users before first OIDC login; mutable only via
  // linkOidcSubject
  private final String email;
  private final String fullName;
  private final Role role;
  private final AccountStatus status;
  private final boolean onboardingCompleted; // legacy field — kept for backward compat
  private final long version;

  // Fields added for self-registration flow
  private OnboardingState onboardingState;
  private Instant consentTimestamp;
  private String termsVersion;
  private String privacyVersion;

  private AppUser(
      UUID id,
      String oidcSubject,
      String email,
      String fullName,
      Role role,
      AccountStatus status,
      boolean onboardingCompleted,
      long version,
      OnboardingState onboardingState,
      Instant consentTimestamp,
      String termsVersion,
      String privacyVersion) {
    this.id = id;
    this.oidcSubject = oidcSubject;
    this.email = email;
    this.fullName = fullName;
    this.role = role;
    this.status = status;
    this.onboardingCompleted = onboardingCompleted;
    this.version = version;
    this.onboardingState = onboardingState;
    this.consentTimestamp = consentTimestamp;
    this.termsVersion = termsVersion;
    this.privacyVersion = privacyVersion;
  }

  // -------------------------------------------------------------------------
  // Factory: OIDC login provisioning
  // -------------------------------------------------------------------------

  /**
   * Provision a new user on first OIDC login.
   *
   * <p>Defaults: status=ACTIVE, onboardingState=ACTIVE (OIDC-authenticated users are directly
   * active), onboardingCompleted=false, version=0, id=random UUID.
   *
   * @throws IllegalArgumentException if any required argument is null/blank
   */
  public static AppUser provision(String oidcSubject, String email, String fullName, Role role) {
    requireNonBlank(oidcSubject, "oidcSubject");
    requireNonBlank(email, "email");
    requireNonBlank(fullName, "fullName");
    if (role == null) {
      throw new IllegalArgumentException("role must not be null");
    }
    return new AppUser(
        UUID.randomUUID(),
        oidcSubject,
        email,
        fullName,
        role,
        AccountStatus.ACTIVE,
        false,
        0L,
        OnboardingState.ACTIVE,
        null,
        null,
        null);
  }

  // -------------------------------------------------------------------------
  // Factory: self-registration
  // -------------------------------------------------------------------------

  /**
   * Register a new student via self-registration (POST /auth/registration).
   *
   * <p>Defaults: role=STUDENT, status=ACTIVE, onboardingState=PENDING_VERIFICATION, version=0,
   * id=random UUID. No oidcSubject yet (assigned on first OIDC login). Consent is captured
   * separately via {@link #captureConsent}.
   *
   * @param email the institutional email address (domain validated by caller)
   * @param fullName the student's full name
   * @throws IllegalArgumentException if any required argument is null/blank
   */
  public static AppUser register(String email, String fullName) {
    requireNonBlank(email, "email");
    requireNonBlank(fullName, "fullName");
    return new AppUser(
        UUID.randomUUID(),
        null, // no OIDC subject yet
        email,
        fullName,
        Role.STUDENT,
        AccountStatus.ACTIVE,
        false,
        0L,
        OnboardingState.PENDING_VERIFICATION,
        null,
        null,
        null);
  }

  // -------------------------------------------------------------------------
  // Factory: reconstitution from persistence
  // -------------------------------------------------------------------------

  /**
   * Reconstitute an AppUser from persistence (no consent fields — backward compat overload).
   *
   * <p>No default logic applied — every field is explicit. Uses ACTIVE as default onboardingState
   * for backward compat with existing OIDC-provisioned rows.
   */
  public static AppUser reconstitute(
      UUID id,
      String oidcSubject,
      String email,
      String fullName,
      Role role,
      AccountStatus status,
      boolean onboardingCompleted,
      long version) {
    return new AppUser(
        id,
        oidcSubject,
        email,
        fullName,
        role,
        status,
        onboardingCompleted,
        version,
        OnboardingState.ACTIVE,
        null,
        null,
        null);
  }

  /**
   * Reconstitute an AppUser from persistence with full fields including consent evidence.
   *
   * <p>Full reconstitution including consent evidence and onboardingState.
   */
  public static AppUser reconstitute(
      UUID id,
      String oidcSubject,
      String email,
      String fullName,
      Role role,
      AccountStatus status,
      boolean onboardingCompleted,
      long version,
      OnboardingState onboardingState,
      Instant consentTimestamp,
      String termsVersion,
      String privacyVersion) {
    return new AppUser(
        id,
        oidcSubject,
        email,
        fullName,
        role,
        status,
        onboardingCompleted,
        version,
        onboardingState != null ? onboardingState : OnboardingState.ACTIVE,
        consentTimestamp,
        termsVersion,
        privacyVersion);
  }

  // -------------------------------------------------------------------------
  // Business methods
  // -------------------------------------------------------------------------

  /**
   * Capture consent evidence on a registered user.
   *
   * <p>Records the terms version, privacy policy version, and timestamp of acceptance. All three
   * fields are required.
   *
   * @param termsVersion the version of terms of use accepted (e.g. "1.0")
   * @param privacyVersion the version of the privacy policy accepted (e.g. "1.0")
   * @param consentTimestamp the moment the user accepted (must not be null)
   * @throws IllegalArgumentException if any argument is null or blank
   */
  public void captureConsent(String termsVersion, String privacyVersion, Instant consentTimestamp) {
    if (termsVersion == null || termsVersion.isBlank()) {
      throw new IllegalArgumentException("termsVersion must not be null or blank");
    }
    if (privacyVersion == null || privacyVersion.isBlank()) {
      throw new IllegalArgumentException("privacyVersion must not be null or blank");
    }
    if (consentTimestamp == null) {
      throw new IllegalArgumentException("consentTimestamp must not be null");
    }
    this.termsVersion = termsVersion;
    this.privacyVersion = privacyVersion;
    this.consentTimestamp = consentTimestamp;
  }

  /**
   * Link an OIDC subject to a previously subject-less self-registered user.
   *
   * <p>Called by {@code ProvisionUserOnLoginService} when a self-registered user (created via POST
   * /auth/registration with oidcSubject=null) completes their first OIDC login. Invariant: this
   * method may only be called once — throws if oidcSubject is already set.
   *
   * @param subject the OIDC subject (sub claim) from the Keycloak token
   * @throws IllegalStateException if oidcSubject is already set (linkage already done)
   * @throws IllegalArgumentException if subject is null or blank
   */
  public void linkOidcSubject(String subject) {
    if (subject == null || subject.isBlank()) {
      throw new IllegalArgumentException("OIDC subject must not be null or blank");
    }
    if (this.oidcSubject != null) {
      throw new IllegalStateException(
          "Cannot link OIDC subject: already linked to " + this.oidcSubject);
    }
    this.oidcSubject = subject;
  }

  /**
   * Activate this user — transitions {@link OnboardingState#PENDING_VERIFICATION} to {@link
   * OnboardingState#ACTIVE}.
   *
   * <p>Called by {@code ProvisionUserOnLoginService} when the OIDC login carries {@code
   * email_verified=true} (sync-on-login pattern). Idempotent: no-op if already ACTIVE.
   */
  public void activate() {
    if (this.onboardingState == OnboardingState.PENDING_VERIFICATION) {
      this.onboardingState = OnboardingState.ACTIVE;
    }
    // Already ACTIVE — idempotent, no-op.
  }

  // -------------------------------------------------------------------------
  // Accessors
  // -------------------------------------------------------------------------

  public UUID id() {
    return id;
  }

  public String oidcSubject() {
    return oidcSubject;
  }

  public String email() {
    return email;
  }

  public String fullName() {
    return fullName;
  }

  public Role role() {
    return role;
  }

  public AccountStatus status() {
    return status;
  }

  public boolean onboardingCompleted() {
    return onboardingCompleted;
  }

  public long version() {
    return version;
  }

  public OnboardingState onboardingState() {
    return onboardingState;
  }

  public Instant consentTimestamp() {
    return consentTimestamp;
  }

  public String termsVersion() {
    return termsVersion;
  }

  public String privacyVersion() {
    return privacyVersion;
  }

  // -------------------------------------------------------------------------
  // Helpers
  // -------------------------------------------------------------------------

  private static void requireNonBlank(String value, String fieldName) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(fieldName + " must not be null or blank");
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof AppUser other)) return false;
    return Objects.equals(id, other.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }

  @Override
  public String toString() {
    return "AppUser[id=%s, oidcSubject=%s, email=%s, role=%s, status=%s, onboardingState=%s]"
        .formatted(id, oidcSubject, email, role, status, onboardingState);
  }
}
