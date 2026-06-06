package co.edu.uniquindio.brujula.administration.application.dto;

import java.util.UUID;

/**
 * Command DTO for creating a staff account (idempotent).
 *
 * <p>Lives in {@code application.dto} to comply with ARQ-003 (port.in must be interfaces only).
 *
 * @param idempotencyKey client-supplied UUID from the Idempotency-Key header
 * @param actorId the UUID of the authenticated ADMIN creating the staff account
 * @param email institutional email address
 * @param fullName display name
 * @param requestHash SHA-256 of the request body (used for conflict detection)
 */
public record CreateStaffCommand(
    UUID idempotencyKey, UUID actorId, String email, String fullName, String requestHash) {}
