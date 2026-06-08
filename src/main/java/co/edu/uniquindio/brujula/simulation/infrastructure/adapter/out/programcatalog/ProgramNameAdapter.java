package co.edu.uniquindio.brujula.simulation.infrastructure.adapter.out.programcatalog;

import co.edu.uniquindio.brujula.programcatalog.application.port.in.GetProgramQuery;
import co.edu.uniquindio.brujula.simulation.application.port.out.ProgramNameReadPort;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Anti-corruption adapter: bridges simulation's {@link ProgramNameReadPort} to
 * programcatalog's published {@code programcatalog::api} named interface (INV-001).
 *
 * <p>Returns the program's display name. The simulation application layer and domain never
 * see programcatalog types (ARQ-002).
 *
 * <p>Package-private per ARQ-006b. Wired by {@code SimulationConfiguration}.
 */
@Component
class ProgramNameAdapter implements ProgramNameReadPort {

  private final GetProgramQuery getProgramQuery;

  ProgramNameAdapter(GetProgramQuery getProgramQuery) {
    this.getProgramQuery = getProgramQuery;
  }

  @Override
  public String programName(UUID programId) {
    return getProgramQuery.getProgram(programId).name();
  }
}
