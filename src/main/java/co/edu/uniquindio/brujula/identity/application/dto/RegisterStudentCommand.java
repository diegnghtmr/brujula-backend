package co.edu.uniquindio.brujula.identity.application.dto;

/**
 * Command for student self-registration (Slice 2, S2-B-3).
 *
 * <p>Carries all data required by {@code RegisterStudentService}. Under OPTION A (DT-011), the
 * backend NEVER handles a password. Keycloak sends an {@code UPDATE_PASSWORD} action-email so the
 * user sets their initial credential on Keycloak's own page after clicking the verification link.
 * The consent versions are validated against configured current versions.
 *
 * <p>Lives in {@code identity.application.dto} (ARQ-003): DTOs are permitted in the application
 * layer but not in port.in (port.in is interfaces only, ARQ-003).
 */
public record RegisterStudentCommand(
    String email,
    String fullName,
    String termsVersion,
    String privacyVersion) {

  public RegisterStudentCommand {
    if (email == null || email.isBlank()) {
      throw new IllegalArgumentException("email must not be null or blank");
    }
    if (fullName == null || fullName.isBlank()) {
      throw new IllegalArgumentException("fullName must not be null or blank");
    }
  }
}
