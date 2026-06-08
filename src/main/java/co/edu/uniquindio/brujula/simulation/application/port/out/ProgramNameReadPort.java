package co.edu.uniquindio.brujula.simulation.application.port.out;

import java.util.UUID;

/**
 * Output port: resolve the display name of an academic program.
 *
 * <p>Delegates to {@code programcatalog::api.GetProgramQuery}. The simulation domain never sees
 * programcatalog types (ARQ-002, INV-001). Named by need (ARQ-001).
 *
 * <p>Used by {@code AddSimulationSelectionService} to populate {@code ProgramCreditLimit.programName}
 * with the real program name instead of a placeholder (CRITICAL-2 fix).
 */
public interface ProgramNameReadPort {

  /**
   * Return the display name of the program with the given ID.
   *
   * @param programId the program UUID
   * @return the program name (non-null)
   * @throws co.edu.uniquindio.brujula.programcatalog.domain.ProgramNotFoundException if not found
   */
  String programName(UUID programId);
}
