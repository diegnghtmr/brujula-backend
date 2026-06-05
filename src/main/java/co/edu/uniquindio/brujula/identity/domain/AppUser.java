package co.edu.uniquindio.brujula.identity.domain;

import java.util.Objects;
import java.util.UUID;

/**
 * AppUser — the central aggregate of the identity module.
 *
 * <p>Represents a provisioned user whose identity is backed by Keycloak OIDC. Zero Spring/JPA
 * imports (ARQ-002). All invariants are enforced in factory methods.
 *
 * <p>Design §2.1: id, oidcSubject, email, fullName, Role VO, AccountStatus, onboardingCompleted,
 * version (optimistic lock). Email is immutable post-create.
 *
 * <p>Two factory methods:
 *
 * <ul>
 *   <li>{@link #provision} — first-login creation; sets defaults.
 *   <li>{@link #reconstitute} — restores from persistence without default logic.
 * </ul>
 */
public final class AppUser {

  private final UUID id;
  private final String oidcSubject;
  private final String email;
  private final String fullName;
  private final Role role;
  private final AccountStatus status;
  private final boolean onboardingCompleted;
  private final long version;

  private AppUser(
      UUID id,
      String oidcSubject,
      String email,
      String fullName,
      Role role,
      AccountStatus status,
      boolean onboardingCompleted,
      long version) {
    this.id = id;
    this.oidcSubject = oidcSubject;
    this.email = email;
    this.fullName = fullName;
    this.role = role;
    this.status = status;
    this.onboardingCompleted = onboardingCompleted;
    this.version = version;
  }

  /**
   * Provision a new user on first OIDC login.
   *
   * <p>Defaults: status=ACTIVE, onboardingCompleted=false, version=0, id=random UUID.
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
        UUID.randomUUID(), oidcSubject, email, fullName, role, AccountStatus.ACTIVE, false, 0L);
  }

  /**
   * Reconstitute an AppUser from persistence. No default logic applied — every field is explicit.
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
        id, oidcSubject, email, fullName, role, status, onboardingCompleted, version);
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
    return "AppUser[id=%s, oidcSubject=%s, email=%s, role=%s, status=%s]"
        .formatted(id, oidcSubject, email, role, status);
  }
}
