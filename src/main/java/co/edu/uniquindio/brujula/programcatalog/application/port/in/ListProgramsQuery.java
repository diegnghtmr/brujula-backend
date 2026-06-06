package co.edu.uniquindio.brujula.programcatalog.application.port.in;

import co.edu.uniquindio.brujula.programcatalog.application.dto.PagedResult;
import co.edu.uniquindio.brujula.programcatalog.application.dto.ProgramView;

/**
 * Input port: list academic programs with optional status filtering and pagination.
 *
 * <p>Backs {@code GET /programs} (CAT-001). When {@code statusFilter} is "ACTIVE", only active
 * programs are returned. A null filter returns all programs.
 *
 * <p>No Spring/framework imports (ARQ-003).
 */
public interface ListProgramsQuery {

  /**
   * Return a paginated list of academic programs, optionally filtered by status.
   *
   * @param statusFilter optional status name to filter by (e.g. "ACTIVE"); null returns all
   * @param page zero-based page number
   * @param pageSize number of items per page
   * @return paginated result of {@link ProgramView} DTOs
   */
  PagedResult<ProgramView> listPrograms(String statusFilter, int page, int pageSize);
}
