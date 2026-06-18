package co.edu.uniquindio.brujula.programcatalog.application.dto;

import java.util.List;

/**
 * Generic paginated result container.
 *
 * <p>Framework-free pagination wrapper returned by list query services. No Spring/framework
 * imports.
 *
 * <p>Field names match the OpenAPI contract's {@code PageMeta} schema: {@code size} (items per
 * page), {@code totalPages} (computed). REST adapter maps this directly to the generated {@code
 * ProgramPage}/{@code PageMeta}.
 *
 * @param <T> the element type
 * @param content the current page content
 * @param totalElements total number of elements across all pages (before pagination)
 * @param page zero-based page number
 * @param size number of elements per page
 */
public record PagedResult<T>(List<T> content, long totalElements, int page, int size) {

  /**
   * Returns the total number of pages.
   *
   * <p>Computed as {@code ceil(totalElements / size)}. Returns {@code 0} when {@code size <= 0} to
   * avoid division-by-zero.
   */
  public int totalPages() {
    if (size <= 0) {
      return 0;
    }
    return (int) Math.ceil((double) totalElements / size);
  }
}
