package co.edu.uniquindio.brujula.offering.application.dto;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Command DTO for the idempotent {@code createPeriod} use case.
 *
 * <p>Plain types only (no domain VOs) per ARQ-001.
 * Carries {@code idempotencyKey} + {@code requestHash} for the idempotency store.
 *
 * @param idempotencyKey  client-supplied UUID header
 * @param actorId         authenticated ADMIN user ID
 * @param name            period name, e.g. "2026-2"
 * @param startDate       inclusive start date (nullable)
 * @param endDate         inclusive end date (nullable)
 * @param status          status string, e.g. "UPCOMING" (nullable — defaults to UPCOMING)
 * @param requestHash     SHA-256 hex digest of the serialized request body
 */
public record CreatePeriodCommand(
    UUID idempotencyKey,
    UUID actorId,
    String name,
    LocalDate startDate,
    LocalDate endDate,
    String status,
    String requestHash) {}
