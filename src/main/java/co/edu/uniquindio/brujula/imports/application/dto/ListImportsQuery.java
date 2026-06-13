package co.edu.uniquindio.brujula.imports.application.dto;

/**
 * Query parameters for the list-imports operation.
 *
 * <p>Application DTO — no Spring/domain imports.
 *
 * @param actorEmail actor's institutional email (for STAFF program-scoping)
 * @param actorRole "STAFF" or "ADMIN" — ADMIN sees all batches
 * @param kindFilter optional ImportKind name filter; null = all kinds
 * @param statusFilter optional ImportStatus name filter; null = all statuses
 * @param page page index (0-based)
 * @param size page size
 */
public record ListImportsQuery(
    String actorEmail,
    String actorRole,
    String kindFilter,
    String statusFilter,
    int page,
    int size) {}
