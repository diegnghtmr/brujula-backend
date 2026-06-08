package co.edu.uniquindio.brujula.simulation.application.port.out;

import java.util.Set;
import java.util.UUID;

/**
 * Output port: read a student's passed course set from the progress module.
 *
 * <p>Delegates to {@code progress::api.GetStudentCourseHistoryQuery}. Pure read — no mutation.
 *
 * <p>Named by need (ARQ-001). Implemented by {@code ProgressReadAdapter}.
 */
public interface ProgressReadPort {

  /**
   * Return the set of courseIds the student has effectively passed within the given program.
   *
   * @param ownerId   student UUID
   * @param programId program UUID (scope the lookup to the correct subscription)
   * @return set of passed course UUIDs; empty if none or no subscription found
   */
  Set<UUID> passedCourseIds(UUID ownerId, UUID programId);
}
