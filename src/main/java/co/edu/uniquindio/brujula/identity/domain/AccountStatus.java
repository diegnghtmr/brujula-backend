package co.edu.uniquindio.brujula.identity.domain;

/**
 * AccountStatus value object — tracks lifecycle state of an app_user account.
 *
 * <p>Maps to the {@code AccountStatus} enum in the OpenAPI contract and to the {@code status} CHECK
 * constraint in {@code identity.app_user}.
 */
public enum AccountStatus {
  ACTIVE,
  INACTIVE,
  PENDING_VERIFICATION
}
