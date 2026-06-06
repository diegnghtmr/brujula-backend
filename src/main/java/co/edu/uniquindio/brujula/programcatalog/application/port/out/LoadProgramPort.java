package co.edu.uniquindio.brujula.programcatalog.application.port.out;

import co.edu.uniquindio.brujula.programcatalog.domain.AcademicProgram;
import co.edu.uniquindio.brujula.programcatalog.domain.ProgramId;
import java.util.List;
import java.util.Optional;

/**
 * Output port: load academic programs from persistence.
 *
 * <p>Named by need (ISP) per hexagonal architecture conventions. No Spring/framework imports.
 */
public interface LoadProgramPort {

  /**
   * Load all academic programs (all statuses — filtering happens in the service).
   *
   * @return all programs
   */
  List<AcademicProgram> loadAll();

  /**
   * Load a single academic program by its typed ID.
   *
   * @param programId the program typed UUID
   * @return the program if found, empty otherwise
   */
  Optional<AcademicProgram> loadById(ProgramId programId);
}
