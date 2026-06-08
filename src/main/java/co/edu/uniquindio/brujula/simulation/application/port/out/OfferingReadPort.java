package co.edu.uniquindio.brujula.simulation.application.port.out;

import co.edu.uniquindio.brujula.simulation.application.dto.ScheduleOptionResolutionView;

import java.util.UUID;

/**
 * Output port: read offering data from the offering module (ADR-F4-005).
 *
 * <p>WU3: {@link #resolvePeriodName(UUID)} — returns human-readable period name.
 * WU4a: {@link #resolveScheduleOption(UUID)} — resolves full schedule option context.
 *        {@link #currentOfferingVersion(UUID)} — returns current offering version for staleness.
 */
public interface OfferingReadPort {

  /**
   * Resolve the human-readable period name for a given period UUID.
   *
   * @param periodId the period UUID
   * @return period name string (e.g. "2026-2")
   * @throws co.edu.uniquindio.brujula.offering.domain.PeriodNotFoundException if not found
   */
  String resolvePeriodName(UUID periodId);

  /**
   * Resolve a schedule option by its UUID, returning all context needed for validation.
   *
   * @param scheduleOptionId the schedule option UUID
   * @return resolution view with courseId, programId, sessions, version, etc.
   * @throws co.edu.uniquindio.brujula.offering.domain.ScheduleOptionNotFoundException if not found
   */
  ScheduleOptionResolutionView resolveScheduleOption(UUID scheduleOptionId);

  /**
   * Return the current version of an offering (for staleness detection, STALE-001).
   *
   * @param offeringId the offering UUID
   * @return current offering version
   * @throws co.edu.uniquindio.brujula.offering.domain.OfferingNotFoundException if not found
   */
  long currentOfferingVersion(UUID offeringId);
}
