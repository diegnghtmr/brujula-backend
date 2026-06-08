package co.edu.uniquindio.brujula.offering.infrastructure.adapter.in.rest;

import co.edu.uniquindio.brujula.generated.model.AcademicPeriod;
import co.edu.uniquindio.brujula.generated.model.Offering;
import co.edu.uniquindio.brujula.generated.model.OfferingDetail;
import co.edu.uniquindio.brujula.generated.model.OfferingPage;
import co.edu.uniquindio.brujula.generated.model.OfferingStatus;
import co.edu.uniquindio.brujula.generated.model.PeriodStatus;
import co.edu.uniquindio.brujula.generated.model.ScheduleOption;
import co.edu.uniquindio.brujula.generated.model.ScheduleSession;
import co.edu.uniquindio.brujula.generated.model.Weekday;
import co.edu.uniquindio.brujula.offering.application.dto.AcademicPeriodView;
import co.edu.uniquindio.brujula.offering.application.dto.OfferingDetailView;
import co.edu.uniquindio.brujula.offering.application.dto.OfferingPageView;
import co.edu.uniquindio.brujula.offering.application.dto.OfferingSummaryView;
import co.edu.uniquindio.brujula.offering.application.dto.ScheduleOptionView;
import co.edu.uniquindio.brujula.offering.application.dto.ScheduleSessionView;

import java.util.List;

/**
 * REST mapper: converts offering application DTOs to generated contract model objects.
 *
 * <p>Package-private per ARQ-006b. courseName is denormalized from the course_offering table.
 */
class OfferingRestMapper {

  AcademicPeriod toContract(AcademicPeriodView view) {
    AcademicPeriod model = new AcademicPeriod(
        view.id(),
        view.name(),
        PeriodStatus.valueOf(view.status()));
    model.setStartDate(view.startDate());
    model.setEndDate(view.endDate());
    model.setVersion(view.version());
    return model;
  }

  OfferingPage toContractPage(OfferingPageView view) {
    List<Offering> content = view.content().stream()
        .map(this::toContractSummary)
        .toList();
    int totalPages = view.size() > 0
        ? (int) Math.ceil((double) view.totalElements() / view.size())
        : 0;
    return new OfferingPage(
        view.page(),
        view.size(),
        view.totalElements(),
        totalPages,
        content);
  }

  OfferingDetail toContractDetail(OfferingDetailView view) {
    List<ScheduleOption> options = view.scheduleOptions().stream()
        .map(this::toContractOption)
        .toList();
    OfferingDetail detail = new OfferingDetail(
        view.id(),
        view.courseId(),
        view.courseName(),
        view.programId(),
        view.periodId(),
        OfferingStatus.valueOf(view.status()),
        view.version() != null ? view.version() : 0L,
        options);
    detail.setNotes(view.notes());
    return detail;
  }

  private Offering toContractSummary(OfferingSummaryView view) {
    Offering offering = new Offering(
        view.id(),
        view.courseId(),
        view.courseName(),
        view.programId(),
        view.periodId(),
        OfferingStatus.valueOf(view.status()),
        view.version() != null ? view.version() : 0L);
    offering.setNotes(view.notes());
    return offering;
  }

  private ScheduleOption toContractOption(ScheduleOptionView view) {
    List<ScheduleSession> sessions = view.sessions().stream()
        .map(this::toContractSession)
        .toList();
    return new ScheduleOption(
        view.id(),
        view.offeringId(),
        view.label(),
        view.journeyCode(),
        view.weeklyHours(),
        sessions,
        view.version() != null ? view.version() : 0L);
  }

  private ScheduleSession toContractSession(ScheduleSessionView view) {
    String startsAt = view.startsAt().toString(); // HH:mm[:ss]
    String endsAt = view.endsAt().toString();
    return new ScheduleSession(
        Weekday.valueOf(view.day()),
        startsAt,
        endsAt)
        .id(view.id());
  }
}
