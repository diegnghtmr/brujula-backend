package co.edu.uniquindio.brujula.curriculum.application.dto;

import java.util.UUID;

/**
 * Application-layer DTO for a CourseRequirement read view.
 *
 * <p>Returned by {@link co.edu.uniquindio.brujula.curriculum.application.port.in.ListCourseRequirementsQuery}
 * to REST adapters. Includes all CUR-002 fields including the optional
 * {@code confidence} and {@code inferred} metadata (ADR-F2-004, CROSS-001 extension).
 *
 * <p>No Spring/framework imports.
 *
 * @param id the requirement UUID
 * @param targetCourseId the course that has this requirement
 * @param type the requirement type (COURSE_PASSED, SEQUENCE, MINIMUM_CREDITS, CONFIGURABLE)
 * @param requiredCourseId the specific prerequisite course UUID (nullable for MINIMUM_CREDITS/CONFIGURABLE)
 * @param requiredCredits minimum credit count required (nullable; only for MINIMUM_CREDITS)
 * @param simultaneityAllowed whether simultaneous enrollment is allowed (nullable)
 * @param confidence confidence level of this edge (HIGH/MEDIUM/LOW; nullable)
 * @param inferred whether this edge was inferred rather than officially documented (nullable)
 * @param status the requirement status (ACTIVE or ARCHIVED)
 */
public record CourseRequirementView(
    UUID id,
    UUID targetCourseId,
    String type,
    UUID requiredCourseId,
    Integer requiredCredits,
    Boolean simultaneityAllowed,
    String confidence,
    Boolean inferred,
    String status) {}
