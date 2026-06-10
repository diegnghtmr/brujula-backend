package co.edu.uniquindio.brujula.progress.application.dto;

import java.util.UUID;

/**
 * Query parameters for listing course attempts.
 *
 * @param actorId authenticated student's user ID (for BOLA filtering)
 * @param subscriptionId optional subscription filter
 * @param courseId optional course filter
 * @param page 0-based page number
 * @param size page size
 */
public record ListCourseAttemptsQuery(
    UUID actorId, UUID subscriptionId, UUID courseId, int page, int size) {}
