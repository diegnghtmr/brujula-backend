package co.edu.uniquindio.brujula.identity.application.dto;

import java.util.UUID;

/**
 * Result of a successful student self-registration.
 *
 * <p>Carries minimal data needed by the REST adapter to build the 202 response. Lives in {@code
 * identity.application.dto}.
 *
 * <p>The {@code onboardingState} is a String representation of the domain {@code OnboardingState}
 * enum (e.g. {@code "PENDING_VERIFICATION"}, {@code "ACTIVE"}). Using String instead of the domain
 * enum keeps this DTO framework-free and prevents the REST adapter from importing domain types
 * directly.
 */
public record RegisterStudentResult(UUID userId, String email, String onboardingState) {}
