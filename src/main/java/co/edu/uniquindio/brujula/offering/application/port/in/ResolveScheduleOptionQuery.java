package co.edu.uniquindio.brujula.offering.application.port.in;

import co.edu.uniquindio.brujula.offering.application.dto.ScheduleOptionResolutionView;
import java.util.UUID;

/** Primary port: resolve a schedule option by ID (used by simulator). */
public interface ResolveScheduleOptionQuery {

  /**
   * Returns the full view of a schedule option, including parent offering context.
   *
   * @param scheduleOptionId the schedule option UUID
   * @return schedule option resolution view
   * @throws co.edu.uniquindio.brujula.offering.domain.ScheduleOptionNotFoundException if not found
   */
  ScheduleOptionResolutionView resolveScheduleOption(UUID scheduleOptionId);
}
