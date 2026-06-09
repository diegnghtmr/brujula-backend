package co.edu.uniquindio.brujula.curriculum.application.dto;

import java.util.UUID;

/**
 * Command DTO for creating a new CourseRequirement.
 *
 * <p>Carries all fields needed to create a CourseRequirement aggregate. No framework imports.
 *
 * @param idempotencyKey unique per-request key for exactly-once semantics
 * @param requestHash hash of the canonical request body (for idempotency conflict detection)
 * @param actorId UUID of the authenticated actor
 * @param targetCourseId the course that will have this requirement
 * @param planId the study plan UUID (used for cycle detection — load plan's full graph)
 * @param type the requirement type string (COURSE_PASSED, SEQUENCE, MINIMUM_CREDITS, CONFIGURABLE)
 * @param requiredCourseId the course that must be completed first (nullable for
 *     MINIMUM_CREDITS/CONFIGURABLE)
 * @param requiredCredits minimum credits required (nullable; only for MINIMUM_CREDITS)
 * @param simultaneityAllowed whether simultaneous enrollment is permitted
 */
public record CreateCourseRequirementCommand(
    UUID idempotencyKey,
    String requestHash,
    UUID actorId,
    UUID targetCourseId,
    UUID planId,
    String type,
    UUID requiredCourseId,
    Integer requiredCredits,
    Boolean simultaneityAllowed) {}
