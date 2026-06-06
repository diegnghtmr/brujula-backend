package co.edu.uniquindio.brujula.programcatalog.application.dto;

import java.util.UUID;

/**
 * Application-layer DTO for a StudyPlan read view.
 *
 * <p>Returned by query services to REST adapters (ARQ-001 — controllers do not touch domain).
 * No Spring/framework imports.
 *
 * @param id the plan UUID
 * @param programId the owning program UUID
 * @param name the plan name
 * @param status the plan status string (DRAFT, ACTIVE, ARCHIVED)
 * @param totalCredits total graduation credits
 * @param referenceYear optional year of the plan agreement (nullable)
 * @param version optimistic lock version (DT-014)
 */
public record StudyPlanView(
    UUID id,
    UUID programId,
    String name,
    String status,
    int totalCredits,
    Integer referenceYear,
    long version) {}
