package co.edu.uniquindio.brujula.simulation.infrastructure.adapter.out.offering;

import co.edu.uniquindio.brujula.offering.application.port.in.GetOfferingQuery;
import co.edu.uniquindio.brujula.offering.application.port.in.ResolvePeriodQuery;
import co.edu.uniquindio.brujula.offering.application.port.in.ResolveScheduleOptionQuery;
import co.edu.uniquindio.brujula.simulation.application.dto.ScheduleOptionResolutionView;
import co.edu.uniquindio.brujula.simulation.application.port.out.OfferingReadPort;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Anti-corruption adapter: bridges simulation's {@link OfferingReadPort} to offering's published
 * {@code offering::api} named interface (ADR-F4-005, INV-001).
 *
 * <p>WU3: {@link #resolvePeriodName(UUID)} implemented. WU4a: {@link #resolveScheduleOption(UUID)}
 * and {@link #currentOfferingVersion(UUID)} implemented. Mapping from offering::api-dto types to
 * sim-local types happens HERE (boundary mapping at adapter edge).
 */
@Component
class OfferingReadAdapter implements OfferingReadPort {

  private final ResolvePeriodQuery resolvePeriodQuery;
  private final ResolveScheduleOptionQuery resolveScheduleOptionQuery;
  private final GetOfferingQuery getOfferingQuery;

  OfferingReadAdapter(
      ResolvePeriodQuery resolvePeriodQuery,
      ResolveScheduleOptionQuery resolveScheduleOptionQuery,
      GetOfferingQuery getOfferingQuery) {
    this.resolvePeriodQuery = resolvePeriodQuery;
    this.resolveScheduleOptionQuery = resolveScheduleOptionQuery;
    this.getOfferingQuery = getOfferingQuery;
  }

  @Override
  public String resolvePeriodName(UUID periodId) {
    return resolvePeriodQuery.resolvePeriod(periodId).name();
  }

  @Override
  public ScheduleOptionResolutionView resolveScheduleOption(UUID scheduleOptionId) {
    // Resolve via offering::api — result is offering::api-dto type
    co.edu.uniquindio.brujula.offering.application.dto.ScheduleOptionResolutionView offeringView =
        resolveScheduleOptionQuery.resolveScheduleOption(scheduleOptionId);

    // Map sessions: offering ScheduleSessionView → sim-local SessionSlot (boundary purity, ARQ-002)
    List<ScheduleOptionResolutionView.SessionSlot> sessions =
        offeringView.sessions().stream()
            .map(
                s ->
                    new ScheduleOptionResolutionView.SessionSlot(s.day(), s.startsAt(), s.endsAt()))
            .toList();

    return new ScheduleOptionResolutionView(
        offeringView.scheduleOptionId(),
        offeringView.offeringId(),
        offeringView.courseId(),
        offeringView.courseName(),
        offeringView.programId(),
        offeringView.periodId(),
        offeringView.journeyCode(),
        offeringView.weeklyHours(),
        offeringView.offeringStatus(),
        offeringView.offeringVersion(),
        sessions);
  }

  @Override
  public long currentOfferingVersion(UUID offeringId) {
    Long version = getOfferingQuery.getOffering(offeringId).version();
    return version != null ? version : 0L;
  }
}
