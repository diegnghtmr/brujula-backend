package co.edu.uniquindio.brujula.curriculum.application.dto;

import java.util.UUID;

/**
 * Command DTO for updating an existing CourseRequirement (optimistic lock, NOT idempotent).
 *
 * <p>No framework imports.
 *
 * @param actorId UUID of the authenticated actor
 * @param requirementId the requirement to update
 * @param expectedVersion the client's known version (optimistic lock check)
 * @param type the new requirement type string (nullable if unchanged)
 * @param requiredCourseId the new required course (nullable)
 * @param requiredCredits the new credit count (nullable)
 * @param simultaneityAllowed the new simultaneity flag (nullable)
 */
public record UpdateCourseRequirementCommand(
    UUID actorId,
    UUID requirementId,
    long expectedVersion,
    String type,
    UUID requiredCourseId,
    Integer requiredCredits,
    Boolean simultaneityAllowed) {}
