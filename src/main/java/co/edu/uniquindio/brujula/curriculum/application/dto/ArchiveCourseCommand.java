package co.edu.uniquindio.brujula.curriculum.application.dto;

import java.util.UUID;

/**
 * Command DTO for archiving a Course (soft-delete, optimistic lock).
 *
 * <p>No framework imports.
 *
 * @param actorId UUID of the authenticated actor
 * @param courseId the course to archive
 * @param expectedVersion the client's known version (optimistic lock check)
 */
public record ArchiveCourseCommand(UUID actorId, UUID courseId, long expectedVersion) {}
