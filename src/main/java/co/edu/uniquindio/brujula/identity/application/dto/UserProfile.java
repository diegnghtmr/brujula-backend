package co.edu.uniquindio.brujula.identity.application.dto;

import java.util.UUID;

/**
 * Application-layer DTO carrying the user profile data.
 *
 * <p>Returned by {@link co.edu.uniquindio.brujula.identity.application.port.in.GetCurrentUserQuery}
 * so that REST adapters do not need to import domain classes directly. Controllers depend only on
 * application port/in interfaces and their return types.
 *
 * <p>Lives in {@code application.dto} (not {@code application.port.in}) because all classes in
 * {@code port.in} must be interfaces.
 *
 * @param id internal UUID
 * @param oidcSubject the OIDC sub claim
 * @param email the user's email
 * @param fullName the user's full name
 * @param role role string (STUDENT / STAFF / ADMIN)
 * @param status status string (ACTIVE / INACTIVE / PENDING_VERIFICATION)
 * @param onboardingCompleted whether onboarding is complete
 */
public record UserProfile(
    UUID id,
    String oidcSubject,
    String email,
    String fullName,
    String role,
    String status,
    boolean onboardingCompleted) {}
