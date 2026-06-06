package co.edu.uniquindio.brujula.programcatalog.application.service;

import co.edu.uniquindio.brujula.programcatalog.application.dto.PagedResult;
import co.edu.uniquindio.brujula.programcatalog.application.dto.ProgramView;
import co.edu.uniquindio.brujula.programcatalog.application.port.in.ListProgramsQuery;
import co.edu.uniquindio.brujula.programcatalog.application.port.out.LoadProgramPort;
import co.edu.uniquindio.brujula.programcatalog.domain.AcademicProgram;
import co.edu.uniquindio.brujula.programcatalog.domain.ProgramStatus;
import java.util.List;

/**
 * Use case implementation: list academic programs with optional status filter and pagination.
 *
 * <p>Status filter is case-sensitive against {@link ProgramStatus#name()}. Pagination is applied
 * in-memory over the filtered set. No Spring annotations — wired via infrastructure configuration
 * (ARQ-002).
 */
public class ListProgramsService implements ListProgramsQuery {

  private final LoadProgramPort loadProgramPort;

  public ListProgramsService(LoadProgramPort loadProgramPort) {
    this.loadProgramPort = loadProgramPort;
  }

  @Override
  public PagedResult<ProgramView> listPrograms(String statusFilter, int page, int pageSize) {
    if (page < 0 || pageSize <= 0) {
      throw new IllegalArgumentException(
          "page must be >= 0 and pageSize must be > 0, got page=%d pageSize=%d"
              .formatted(page, pageSize));
    }
    List<AcademicProgram> all = loadProgramPort.loadAll();
    List<AcademicProgram> filtered = applyStatusFilter(all, statusFilter);
    List<ProgramView> paged = applyPagination(filtered, page, pageSize);
    return new PagedResult<>(paged, filtered.size(), page, pageSize);
  }

  // ---------------------------------------------------------------------------
  // Filter helpers
  // ---------------------------------------------------------------------------

  private List<AcademicProgram> applyStatusFilter(List<AcademicProgram> programs, String statusFilter) {
    if (statusFilter == null || statusFilter.isBlank()) {
      return programs;
    }
    ProgramStatus target = ProgramStatus.valueOf(statusFilter.toUpperCase());
    return programs.stream()
        .filter(p -> p.status() == target)
        .toList();
  }

  private List<ProgramView> applyPagination(List<AcademicProgram> programs, int page, int pageSize) {
    int from = page * pageSize;
    if (from >= programs.size()) {
      return List.of();
    }
    int to = Math.min(from + pageSize, programs.size());
    return programs.subList(from, to).stream()
        .map(this::toView)
        .toList();
  }

  // ---------------------------------------------------------------------------
  // Domain → DTO mapper
  // ---------------------------------------------------------------------------

  private ProgramView toView(AcademicProgram program) {
    return new ProgramView(
        program.id().value(),
        program.name(),
        program.faculty(),
        program.status().name(),
        program.version());
  }
}
