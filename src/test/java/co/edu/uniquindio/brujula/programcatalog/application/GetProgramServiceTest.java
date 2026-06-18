package co.edu.uniquindio.brujula.programcatalog.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import co.edu.uniquindio.brujula.programcatalog.application.dto.ProgramView;
import co.edu.uniquindio.brujula.programcatalog.application.port.in.GetProgramQuery;
import co.edu.uniquindio.brujula.programcatalog.application.port.out.LoadProgramPort;
import co.edu.uniquindio.brujula.programcatalog.application.service.GetProgramService;
import co.edu.uniquindio.brujula.programcatalog.domain.AcademicProgram;
import co.edu.uniquindio.brujula.programcatalog.domain.ProgramId;
import co.edu.uniquindio.brujula.programcatalog.domain.ProgramNotFoundException;
import co.edu.uniquindio.brujula.programcatalog.domain.ProgramStatus;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Verifies GetProgramService returns the program when found, and throws ProgramNotFoundException
 * (mapped to 404) when not found.
 */
class GetProgramServiceTest {

  private LoadProgramPort loadProgramPort;
  private GetProgramQuery service;

  @BeforeEach
  void setUp() {
    loadProgramPort = mock(LoadProgramPort.class);
    service = new GetProgramService(loadProgramPort);
  }

  @Test
  void returnsProgramViewWhenFound() {
    ProgramId id = ProgramId.generate();
    AcademicProgram program =
        AcademicProgram.reconstitute(
            id, "Ingeniería de Sistemas", "FIET", ProgramStatus.ACTIVE, 1L);

    when(loadProgramPort.loadById(id)).thenReturn(Optional.of(program));

    ProgramView view = service.getProgram(id.value());

    assertThat(view.id()).isEqualTo(id.value());
    assertThat(view.name()).isEqualTo("Ingeniería de Sistemas");
    assertThat(view.faculty()).isEqualTo("FIET");
    assertThat(view.status()).isEqualTo("ACTIVE");
    assertThat(view.version()).isEqualTo(1L);
  }

  @Test
  void throwsProgramNotFoundExceptionWhenMissing() {
    ProgramId id = ProgramId.generate();

    when(loadProgramPort.loadById(id)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.getProgram(id.value()))
        .isInstanceOf(ProgramNotFoundException.class);
  }

  @Test
  void notFoundExceptionMessageContainsId() {
    ProgramId id = ProgramId.generate();

    when(loadProgramPort.loadById(id)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.getProgram(id.value()))
        .isInstanceOf(ProgramNotFoundException.class)
        .hasMessageContaining(id.value().toString());
  }
}
