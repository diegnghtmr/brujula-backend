package co.edu.uniquindio.brujula.administration.application.dto;

import java.util.List;

/**
 * Paginated result for staff account queries.
 *
 * <p>Contains {@link StaffAccountView} DTOs so that REST adapters do not need to import domain
 * types (ARQ-001).
 *
 * @param items the staff accounts on this page
 * @param page zero-based page number
 * @param size page size
 * @param totalElements total number of matching accounts
 * @param totalPages total number of pages
 */
public record StaffPage(
    List<StaffAccountView> items, int page, int size, long totalElements, int totalPages) {}
