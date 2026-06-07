package co.edu.uniquindio.brujula.progress.application.dto;

import java.util.List;

/**
 * Application-layer paged result for course attempts.
 *
 * @param content list of attempts on the current page
 * @param page current page number (0-based)
 * @param size page size
 * @param totalElements total number of matching attempts
 * @param totalPages total number of pages
 */
public record CourseAttemptPageView(
    List<CourseAttemptView> content,
    int page,
    int size,
    long totalElements,
    int totalPages) {}
