package co.edu.uniquindio.brujula.offering.application.port.out;

import co.edu.uniquindio.brujula.offering.domain.ScheduleOption;
import co.edu.uniquindio.brujula.offering.domain.ScheduleOptionId;

import java.util.Optional;

/** Secondary port: load schedule option data from persistence. */
public interface LoadScheduleOptionPort {

  /** Returns the schedule option with the given ID, or empty if not found. */
  Optional<ScheduleOption> findById(ScheduleOptionId id);
}
