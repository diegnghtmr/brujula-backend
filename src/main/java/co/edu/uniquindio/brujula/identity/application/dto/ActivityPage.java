package co.edu.uniquindio.brujula.identity.application.dto;

import java.util.List;

/**
 * Paged result of activity events.
 *
 * <p>Lives in {@code application.dto} (not {@code application.port.in}) to satisfy ARQ-003, which
 * requires all classes in {@code port.in} to be interfaces.
 *
 * @param items the events in the current page
 * @param page zero-based page number
 * @param size page size requested
 * @param totalElements total count of matching events
 * @param totalPages total number of pages
 */
public record ActivityPage(
    List<ActivityItem> items, int page, int size, long totalElements, int totalPages) {}
