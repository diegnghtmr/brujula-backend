package co.edu.uniquindio.brujula.audit.application.dto;

import java.util.List;

/**
 * Paginated result of audit events for the read-side query.
 *
 * <p>Contains {@link AuditEventItem} DTOs so that REST adapters do not need to import domain types.
 *
 * @param items the audit events on this page
 * @param page zero-based page number
 * @param size page size
 * @param totalElements total number of matching events
 * @param totalPages total number of pages
 */
public record AuditEventPage(
    List<AuditEventItem> items, int page, int size, long totalElements, int totalPages) {}
