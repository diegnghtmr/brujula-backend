package co.edu.uniquindio.brujula.simulation.infrastructure.adapter.out.progress;

import co.edu.uniquindio.brujula.progress.application.port.in.api.GetStudentCourseHistoryQuery;
import co.edu.uniquindio.brujula.simulation.application.port.out.ProgressReadPort;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Anti-corruption adapter: bridges simulation's {@link ProgressReadPort} to progress's published
 * {@code progress::api.GetStudentCourseHistoryQuery}.
 *
 * <p>Read-only — no mutations to progress domain.
 */
@Component
class ProgressReadAdapter implements ProgressReadPort {

  private final GetStudentCourseHistoryQuery getStudentCourseHistoryQuery;

  ProgressReadAdapter(GetStudentCourseHistoryQuery getStudentCourseHistoryQuery) {
    this.getStudentCourseHistoryQuery = getStudentCourseHistoryQuery;
  }

  @Override
  public Set<UUID> passedCourseIds(UUID ownerId, UUID programId) {
    return getStudentCourseHistoryQuery.passedCourseIds(ownerId, programId);
  }
}
