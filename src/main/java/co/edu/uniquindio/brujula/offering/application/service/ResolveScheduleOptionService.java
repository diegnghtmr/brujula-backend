package co.edu.uniquindio.brujula.offering.application.service;

import co.edu.uniquindio.brujula.offering.application.dto.ScheduleOptionResolutionView;
import co.edu.uniquindio.brujula.offering.application.dto.ScheduleSessionView;
import co.edu.uniquindio.brujula.offering.application.port.in.ResolveScheduleOptionQuery;
import co.edu.uniquindio.brujula.offering.application.port.out.LoadOfferingPort;
import co.edu.uniquindio.brujula.offering.application.port.out.LoadScheduleOptionPort;
import co.edu.uniquindio.brujula.offering.domain.CourseOffering;
import co.edu.uniquindio.brujula.offering.domain.CourseOfferingId;
import co.edu.uniquindio.brujula.offering.domain.OfferingNotFoundException;
import co.edu.uniquindio.brujula.offering.domain.ScheduleOption;
import co.edu.uniquindio.brujula.offering.domain.ScheduleOptionId;
import co.edu.uniquindio.brujula.offering.domain.ScheduleOptionNotFoundException;
import co.edu.uniquindio.brujula.offering.domain.ScheduleSession;
import java.util.List;
import java.util.UUID;

/**
 * Use case: resolve a schedule option (including parent offering context) for the simulator.
 *
 * <p>Framework-free. Satisfies: OFF-004.
 */
public class ResolveScheduleOptionService implements ResolveScheduleOptionQuery {

  private final LoadScheduleOptionPort loadScheduleOptionPort;
  private final LoadOfferingPort loadOfferingPort;

  public ResolveScheduleOptionService(
      LoadScheduleOptionPort loadScheduleOptionPort, LoadOfferingPort loadOfferingPort) {
    this.loadScheduleOptionPort = loadScheduleOptionPort;
    this.loadOfferingPort = loadOfferingPort;
  }

  @Override
  public ScheduleOptionResolutionView resolveScheduleOption(UUID scheduleOptionId) {
    ScheduleOption option =
        loadScheduleOptionPort
            .findById(new ScheduleOptionId(scheduleOptionId))
            .orElseThrow(
                () -> new ScheduleOptionNotFoundException(new ScheduleOptionId(scheduleOptionId)));

    CourseOffering offering =
        loadOfferingPort
            .findById(new CourseOfferingId(option.getOfferingId()))
            .orElseThrow(
                () -> new OfferingNotFoundException(new CourseOfferingId(option.getOfferingId())));

    List<ScheduleSessionView> sessionViews =
        option.getSessions().stream().map(this::toSessionView).toList();

    return new ScheduleOptionResolutionView(
        option.getId().value(),
        offering.getId().value(),
        offering.getCourseId(),
        offering.getCourseName(),
        offering.getProgramId(),
        offering.getPeriodId(),
        option.getLabel(),
        option.getJourneyCode(),
        option.weeklyHours(),
        offering.getStatus().name(),
        offering.getVersion(),
        sessionViews);
  }

  private ScheduleSessionView toSessionView(ScheduleSession session) {
    return new ScheduleSessionView(
        session.id(), session.day().name(), session.startsAt(), session.endsAt());
  }
}
