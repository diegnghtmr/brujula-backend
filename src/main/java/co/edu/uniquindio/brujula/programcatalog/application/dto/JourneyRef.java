package co.edu.uniquindio.brujula.programcatalog.application.dto;

import java.util.UUID;

/**
 * Application-layer DTO for a Journey reference (read view).
 *
 * <p>Returned by {@code ListJourneysQuery} to REST adapters (CAT-003).
 * No Spring/framework imports.
 *
 * @param id the journey UUID
 * @param code the journey code (e.g. "DIURNA", "NOCTURNA")
 * @param name the human-readable journey name
 */
public record JourneyRef(UUID id, String code, String name) {}
