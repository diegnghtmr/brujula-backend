package co.edu.uniquindio.brujula.offering.application.dto;

import java.util.UUID;

/**
 * Command DTO for the update-offering use case (notes + version, NOT idempotent).
 *
 * <p>Status transitions are NOT permitted through this endpoint — use publishOffering.
 *
 * <p>Satisfies: OFFW-004.
 */
public record UpdateOfferingCommand(
    UUID offeringId, UUID actorId, String notes, long expectedVersion) {}
