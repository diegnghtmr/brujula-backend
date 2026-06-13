package co.edu.uniquindio.brujula.administration.domain;

import java.util.UUID;

/**
 * StaffAccount aggregate — represents an administrative staff account.
 *
 * <p>Lifecycle: created ACTIVE; can be deactivated/reactivated by ADMIN. Optimistic locking via
 * version field. The assignment lifecycle (writes) is deferred.
 *
 * <p>No framework annotations — wired by infrastructure configuration.
 */
public final class StaffAccount {

  private final UUID id;
  private final String email;
  private final String fullName;
  private StaffAccountStatus status;
  private long version;

  private StaffAccount(
      UUID id, String email, String fullName, StaffAccountStatus status, long version) {
    this.id = id;
    this.email = email;
    this.fullName = fullName;
    this.status = status;
    this.version = version;
  }

  /**
   * Factory for creating a new staff account. New accounts start ACTIVE with version 0.
   *
   * @param email institutional email address (required, non-blank)
   * @param fullName display name (required, non-blank)
   * @return a new StaffAccount
   */
  public static StaffAccount create(String email, String fullName) {
    if (email == null || email.isBlank()) {
      throw new IllegalArgumentException("email must not be null or blank");
    }
    if (fullName == null || fullName.isBlank()) {
      throw new IllegalArgumentException("fullName must not be null or blank");
    }
    return new StaffAccount(UUID.randomUUID(), email, fullName, StaffAccountStatus.ACTIVE, 0L);
  }

  /** Reconstitution factory for loading a persisted StaffAccount from the database. */
  public static StaffAccount reconstitute(
      UUID id, String email, String fullName, StaffAccountStatus status, long version) {
    return new StaffAccount(id, email, fullName, status, version);
  }

  /** Deactivate this staff account. */
  public void deactivate() {
    this.status = StaffAccountStatus.INACTIVE;
  }

  /** Activate this staff account. */
  public void activate() {
    this.status = StaffAccountStatus.ACTIVE;
  }

  public UUID getId() {
    return id;
  }

  public String getEmail() {
    return email;
  }

  public String getFullName() {
    return fullName;
  }

  public StaffAccountStatus getStatus() {
    return status;
  }

  public long getVersion() {
    return version;
  }
}
