package co.edu.uniquindio.brujula.progress.application.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Application-layer DTO for a Subscription (returned by use cases, serialized for idempotency).
 *
 * <p>Includes programName and planName for the REST response contract. ARQ-001: no domain types.
 *
 * @param id subscription UUID
 * @param programId program UUID
 * @param programName human-readable program name
 * @param planId plan UUID
 * @param planName human-readable plan name
 * @param referenceJourneyCode journey code (e.g. DIURNA)
 * @param referenceJourneyName human-readable journey name
 * @param version optimistic lock version
 * @param createdAt creation timestamp
 */
public record SubscriptionView(
    UUID id,
    UUID programId,
    String programName,
    UUID planId,
    String planName,
    String referenceJourneyCode,
    String referenceJourneyName,
    long version,
    Instant createdAt) {}
