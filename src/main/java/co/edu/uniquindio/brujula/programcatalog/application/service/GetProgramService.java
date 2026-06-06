package co.edu.uniquindio.brujula.programcatalog.application.service;

import co.edu.uniquindio.brujula.programcatalog.application.dto.ProgramView;
import co.edu.uniquindio.brujula.programcatalog.application.port.in.GetProgramQuery;
import co.edu.uniquindio.brujula.programcatalog.application.port.out.LoadProgramPort;
import co.edu.uniquindio.brujula.programcatalog.domain.AcademicProgram;
import co.edu.uniquindio.brujula.programcatalog.domain.ProgramId;
import co.edu.uniquindio.brujula.programcatalog.domain.ProgramNotFoundException;

/**
 * Use case implementation: get a single academic program by ID.
 *
 * <p>Throws {@link ProgramNotFoundException} (maps to HTTP 404) if the program is not found.
 * No Spring annotations — wired via infrastructure configuration (ARQ-002).
 */
public class GetProgramService implements GetProgramQuery {

  private final LoadProgramPort loadProgramPort;

  public GetProgramService(LoadProgramPort loadProgramPort) {
    this.loadProgramPort = loadProgramPort;
  }

  @Override
  public ProgramView getProgram(ProgramId programId) {
    AcademicProgram program = loadProgramPort.loadById(programId)
        .orElseThrow(() -> new ProgramNotFoundException(programId));
    return toView(program);
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
