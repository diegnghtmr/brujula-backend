package co.edu.uniquindio.brujula.offering.application.service;

import co.edu.uniquindio.brujula.offering.application.dto.ListOfferingsCriteria;
import co.edu.uniquindio.brujula.offering.application.dto.OfferingDetailView;
import co.edu.uniquindio.brujula.offering.application.dto.OfferingPageView;
import co.edu.uniquindio.brujula.offering.application.dto.ScheduleOptionView;
import co.edu.uniquindio.brujula.offering.application.dto.ScheduleSessionView;
import co.edu.uniquindio.brujula.offering.application.port.in.GetOfferingQuery;
import co.edu.uniquindio.brujula.offering.application.port.in.ListOfferingsQuery;
import co.edu.uniquindio.brujula.offering.application.port.out.LoadOfferingPort;
import co.edu.uniquindio.brujula.offering.domain.CourseOffering;
import co.edu.uniquindio.brujula.offering.domain.CourseOfferingId;
import co.edu.uniquindio.brujula.offering.domain.OfferingNotFoundException;
import co.edu.uniquindio.brujula.offering.domain.ScheduleOption;
import co.edu.uniquindio.brujula.offering.domain.ScheduleSession;
import java.util.List;
import java.util.UUID;

/**
 * Use case: list published offerings and retrieve full offering detail.
 *
 * <p>Framework-free. Satisfies: OFF-002, OFF-003.
 */
public class GetOfferingService implements ListOfferingsQuery, GetOfferingQuery {

  private final LoadOfferingPort loadOfferingPort;

  public GetOfferingService(LoadOfferingPort loadOfferingPort) {
    this.loadOfferingPort = loadOfferingPort;
  }

  @Override
  public OfferingPageView listOfferings(ListOfferingsCriteria criteria) {
    // OFFW-007: privileged callers (STAFF/ADMIN) can see DRAFT offerings;
    // STUDENT callers only see published (PRELIMINARY/FINAL) offerings.
    if (criteria.isPrivilegedCaller()) {
      return loadOfferingPort.findForStaff(criteria);
    }
    // For STUDENT requesting DRAFT: findPublished will return only PRELIMINARY/FINAL,
    // which is an empty result — correct per spec (not a 403).
    return loadOfferingPort.findPublished(criteria);
  }

  @Override
  public OfferingDetailView getOffering(UUID offeringId) {
    CourseOffering offering =
        loadOfferingPort
            .findById(new CourseOfferingId(offeringId))
            .filter(CourseOffering::isPublished)
            .orElseThrow(() -> new OfferingNotFoundException(new CourseOfferingId(offeringId)));

    return toDetailView(offering);
  }

  OfferingDetailView toDetailView(CourseOffering offering) {
    List<ScheduleOptionView> optionViews =
        offering.getScheduleOptions().stream()
            .map(option -> toOptionView(option, offering.getId().value()))
            .toList();
    return new OfferingDetailView(
        offering.getId().value(),
        offering.getCourseId(),
        offering.getCourseName(),
        offering.getProgramId(),
        offering.getPeriodId(),
        offering.getStatus().name(),
        offering.getNotes(),
        offering.getVersion(),
        optionViews);
  }

  private ScheduleOptionView toOptionView(ScheduleOption option, UUID offeringId) {
    List<ScheduleSessionView> sessionViews =
        option.getSessions().stream().map(this::toSessionView).toList();
    return new ScheduleOptionView(
        option.getId().value(),
        offeringId,
        option.getLabel(),
        option.getJourneyCode(),
        option.weeklyHours(),
        option.getVersion(),
        sessionViews);
  }

  private ScheduleSessionView toSessionView(ScheduleSession session) {
    return new ScheduleSessionView(
        session.id(), session.day().name(), session.startsAt(), session.endsAt());
  }
}
