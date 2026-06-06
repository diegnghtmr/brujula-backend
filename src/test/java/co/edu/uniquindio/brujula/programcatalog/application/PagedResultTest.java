package co.edu.uniquindio.brujula.programcatalog.application;

import static org.assertj.core.api.Assertions.assertThat;

import co.edu.uniquindio.brujula.programcatalog.application.dto.PagedResult;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * FIX-1 (C-1) RED: PagedResult must have a {@code totalPages} computed field and the per-page
 * capacity field must be named {@code size} (matching the OpenAPI contract's PageMeta schema).
 *
 * <p>totalPages math: ceil(totalElements / size), with edge-case protection for size == 0.
 */
class PagedResultTest {

  @Test
  void totalPagesIsZeroWhenTotalElementsIsZero() {
    PagedResult<String> result = new PagedResult<>(List.of(), 0L, 0, 10);

    assertThat(result.totalPages()).isEqualTo(0);
  }

  @Test
  void totalPagesIsOneWhenElementsExactlyFillOnePage() {
    PagedResult<String> result = new PagedResult<>(List.of("a"), 10L, 0, 10);

    assertThat(result.totalPages()).isEqualTo(1);
  }

  @Test
  void totalPagesRoundsUpWhenElementsOverflowPage() {
    PagedResult<String> result = new PagedResult<>(List.of("a"), 11L, 0, 10);

    assertThat(result.totalPages()).isEqualTo(2);
  }

  @Test
  void totalPagesFor25ElementsAndSize10Is3() {
    PagedResult<String> result = new PagedResult<>(List.of(), 25L, 0, 10);

    assertThat(result.totalPages()).isEqualTo(3);
  }

  @Test
  void totalPagesIsZeroWhenSizeIsZeroToAvoidDivisionByZero() {
    PagedResult<String> result = new PagedResult<>(List.of(), 10L, 0, 0);

    assertThat(result.totalPages()).isEqualTo(0);
  }

  @Test
  void fieldIsNamedSizeNotPageSize() {
    PagedResult<String> result = new PagedResult<>(List.of(), 5L, 1, 20);

    // This test asserts the accessor is named 'size()', not 'pageSize()'.
    assertThat(result.size()).isEqualTo(20);
  }
}
