package co.edu.uniquindio.brujula.programcatalog.application.port.out;

import co.edu.uniquindio.brujula.programcatalog.domain.PlanId;
import co.edu.uniquindio.brujula.programcatalog.domain.StudyPlan;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Output port: load study plans from persistence.
 *
 * <p>Named by need (ISP) per hexagonal architecture conventions. No Spring/framework imports.
 */
public interface LoadStudyPlanPort {

  /**
   * Load all study plans belonging to an academic program.
   *
   * @param programId the program UUID
   * @return all plans (all statuses — filtering happens in the service)
   */
  List<StudyPlan> loadByProgramId(UUID programId);

  /**
   * Load a single study plan by its typed ID.
   *
   * @param planId the plan typed UUID
   * @return the plan if found, empty otherwise
   */
  Optional<StudyPlan> loadById(PlanId planId);
}
