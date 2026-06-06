package co.edu.uniquindio.brujula.programcatalog.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import co.edu.uniquindio.brujula.programcatalog.application.dto.PagedResult;
import co.edu.uniquindio.brujula.programcatalog.application.dto.ProgramView;
import co.edu.uniquindio.brujula.programcatalog.application.port.in.ListProgramsQuery;
import co.edu.uniquindio.brujula.programcatalog.application.port.out.LoadProgramPort;
import co.edu.uniquindio.brujula.programcatalog.application.service.ListProgramsService;
import co.edu.uniquindio.brujula.programcatalog.domain.AcademicProgram;
import co.edu.uniquindio.brujula.programcatalog.domain.ProgramId;
import co.edu.uniquindio.brujula.programcatalog.domain.ProgramStatus;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * WU2-2.1 RED: Verifies ListProgramsService returns paginated results and filters by status.
 *
 * <p>When statusFilter is "ACTIVE", only ACTIVE programs are returned.
 * When statusFilter is null, all programs are returned.
 */
class ListProgramsServiceTest {

  private LoadProgramPort loadProgramPort;
  private ListProgramsQuery service;

  @BeforeEach
  void setUp() {
    loadProgramPort = mock(LoadProgramPort.class);
    service = new ListProgramsService(loadProgramPort);
  }

  @Test
  void returnsAllProgramsWhenNoStatusFilter() {
    AcademicProgram active = AcademicProgram.reconstitute(
        ProgramId.generate(), "Ingeniería de Sistemas", "FIET", ProgramStatus.ACTIVE, 0L);
    AcademicProgram archived = AcademicProgram.reconstitute(
        ProgramId.generate(), "Old Program", "FIET", ProgramStatus.ARCHIVED, 1L);

    when(loadProgramPort.loadAll()).thenReturn(List.of(active, archived));

    PagedResult<ProgramView> result = service.listPrograms(null, 0, 10);

    assertThat(result.content()).hasSize(2);
    assertThat(result.totalElements()).isEqualTo(2);
  }

  @Test
  void returnsOnlyActiveWhenStatusFilterIsActive() {
    AcademicProgram active = AcademicProgram.reconstitute(
        ProgramId.generate(), "Ingeniería de Sistemas", "FIET", ProgramStatus.ACTIVE, 0L);
    AcademicProgram inactive = AcademicProgram.reconstitute(
        ProgramId.generate(), "Lenguas Modernas", "FCHSH", ProgramStatus.INACTIVE, 0L);
    AcademicProgram archived = AcademicProgram.reconstitute(
        ProgramId.generate(), "Old Program", "FIET", ProgramStatus.ARCHIVED, 1L);

    when(loadProgramPort.loadAll()).thenReturn(List.of(active, inactive, archived));

    PagedResult<ProgramView> result = service.listPrograms("ACTIVE", 0, 10);

    assertThat(result.content()).hasSize(1);
    assertThat(result.content().get(0).status()).isEqualTo("ACTIVE");
  }

  @Test
  void paginationIsApplied() {
    AcademicProgram p1 = AcademicProgram.reconstitute(
        ProgramId.generate(), "Program 1", "FIET", ProgramStatus.ACTIVE, 0L);
    AcademicProgram p2 = AcademicProgram.reconstitute(
        ProgramId.generate(), "Program 2", "FIET", ProgramStatus.ACTIVE, 0L);
    AcademicProgram p3 = AcademicProgram.reconstitute(
        ProgramId.generate(), "Program 3", "FIET", ProgramStatus.ACTIVE, 0L);

    when(loadProgramPort.loadAll()).thenReturn(List.of(p1, p2, p3));

    PagedResult<ProgramView> result = service.listPrograms(null, 0, 2);

    assertThat(result.content()).hasSize(2);
    assertThat(result.totalElements()).isEqualTo(3);
    assertThat(result.page()).isEqualTo(0);
    assertThat(result.size()).isEqualTo(2);
  }

  @Test
  void secondPageReturnsRemainingItems() {
    AcademicProgram p1 = AcademicProgram.reconstitute(
        ProgramId.generate(), "Program 1", "FIET", ProgramStatus.ACTIVE, 0L);
    AcademicProgram p2 = AcademicProgram.reconstitute(
        ProgramId.generate(), "Program 2", "FIET", ProgramStatus.ACTIVE, 0L);
    AcademicProgram p3 = AcademicProgram.reconstitute(
        ProgramId.generate(), "Program 3", "FIET", ProgramStatus.ACTIVE, 0L);

    when(loadProgramPort.loadAll()).thenReturn(List.of(p1, p2, p3));

    PagedResult<ProgramView> result = service.listPrograms(null, 1, 2);

    assertThat(result.content()).hasSize(1);
    assertThat(result.page()).isEqualTo(1);
  }

  @Test
  void returnsEmptyWhenNoPrograms() {
    when(loadProgramPort.loadAll()).thenReturn(List.of());

    PagedResult<ProgramView> result = service.listPrograms(null, 0, 10);

    assertThat(result.content()).isEmpty();
    assertThat(result.totalElements()).isEqualTo(0);
  }

  // FIX-4 (S-2): guard against invalid pagination parameters

  @Test
  void throwsIllegalArgumentWhenPageIsNegative() {
    when(loadProgramPort.loadAll()).thenReturn(List.of());

    assertThatThrownBy(() -> service.listPrograms(null, -1, 10))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void throwsIllegalArgumentWhenPageSizeIsZero() {
    when(loadProgramPort.loadAll()).thenReturn(List.of());

    assertThatThrownBy(() -> service.listPrograms(null, 0, 0))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void throwsIllegalArgumentWhenPageSizeIsNegative() {
    when(loadProgramPort.loadAll()).thenReturn(List.of());

    assertThatThrownBy(() -> service.listPrograms(null, 0, -5))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void programViewContainsRequiredFields() {
    ProgramId id = ProgramId.generate();
    AcademicProgram program = AcademicProgram.reconstitute(
        id, "Ingeniería de Sistemas", "FIET", ProgramStatus.ACTIVE, 2L);

    when(loadProgramPort.loadAll()).thenReturn(List.of(program));

    PagedResult<ProgramView> result = service.listPrograms(null, 0, 10);

    ProgramView view = result.content().get(0);
    assertThat(view.id()).isEqualTo(id.value());
    assertThat(view.name()).isEqualTo("Ingeniería de Sistemas");
    assertThat(view.faculty()).isEqualTo("FIET");
    assertThat(view.status()).isEqualTo("ACTIVE");
    assertThat(view.version()).isEqualTo(2L);
  }
}
