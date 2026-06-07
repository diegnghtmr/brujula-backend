package co.edu.uniquindio.brujula.curriculum.application.dto;

import java.util.UUID;

/**
 * Application-layer DTO for a DegreeWorkModality read view.
 *
 * <p>Returned by query services to REST adapters (ARQ-001 — controllers do not touch domain).
 * No Spring/framework imports.
 *
 * @param id the modality UUID
 * @param planId the study plan UUID
 * @param name the modality name (e.g. "Aplicación", "Investigación")
 * @param credits optional credits (nullable)
 * @param curricularWeeklyHours optional weekly hours (nullable)
 * @param status the modality status string (ACTIVE, ARCHIVED)
 * @param version optimistic lock version
 */
public record DegreeWorkModalityView(
    UUID id,
    UUID planId,
    String name,
    Integer credits,
    Integer curricularWeeklyHours,
    String status,
    long version) {}
