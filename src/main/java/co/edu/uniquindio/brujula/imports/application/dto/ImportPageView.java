package co.edu.uniquindio.brujula.imports.application.dto;

import java.util.List;

/**
 * Application-layer DTO for a paginated list of ImportSummary views.
 *
 * <p>Returned by {@code ListImportsQueryService} and mapped to the generated ImportPage at the REST
 * boundary. No Spring/domain imports.
 *
 * @param content page content
 * @param page current page index (0-based)
 * @param size page size
 * @param totalElements total matching elements across all pages
 * @param totalPages total pages
 */
public record ImportPageView(
    List<ImportSummary> content, int page, int size, long totalElements, int totalPages) {}
