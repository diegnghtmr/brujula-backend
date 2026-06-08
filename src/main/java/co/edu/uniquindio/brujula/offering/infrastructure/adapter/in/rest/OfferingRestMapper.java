package co.edu.uniquindio.brujula.offering.infrastructure.adapter.in.rest;

import co.edu.uniquindio.brujula.generated.model.AcademicPeriod;
import co.edu.uniquindio.brujula.generated.model.Offering;
import co.edu.uniquindio.brujula.generated.model.OfferingCreateRequest;
import co.edu.uniquindio.brujula.generated.model.OfferingDetail;
import co.edu.uniquindio.brujula.generated.model.OfferingPage;
import co.edu.uniquindio.brujula.generated.model.OfferingStatus;
import co.edu.uniquindio.brujula.generated.model.OfferingUpdateRequest;
import co.edu.uniquindio.brujula.generated.model.PeriodStatus;
import co.edu.uniquindio.brujula.generated.model.PublishOfferingRequest;
import co.edu.uniquindio.brujula.generated.model.ScheduleOption;
import co.edu.uniquindio.brujula.generated.model.ScheduleOptionCreateRequest;
import co.edu.uniquindio.brujula.generated.model.ScheduleSession;
import co.edu.uniquindio.brujula.generated.model.Weekday;
import co.edu.uniquindio.brujula.offering.application.dto.AcademicPeriodView;
import co.edu.uniquindio.brujula.offering.application.dto.CreateOfferingCommand;
import co.edu.uniquindio.brujula.offering.application.dto.OfferingDetailView;
import co.edu.uniquindio.brujula.offering.application.dto.OfferingPageView;
import co.edu.uniquindio.brujula.offering.application.dto.OfferingSummaryView;
import co.edu.uniquindio.brujula.offering.application.dto.PublishOfferingCommand;
import co.edu.uniquindio.brujula.offering.application.dto.ScheduleOptionView;
import co.edu.uniquindio.brujula.offering.application.dto.ScheduleSessionView;
import co.edu.uniquindio.brujula.offering.application.dto.UpdateOfferingCommand;

import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

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
    if (view.intensityWarning() != null) {
      detail.setIntensityWarning(view.intensityWarning());
    }
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

  /**
   * Map a REST create-offering request to the application command.
   *
   * <p>ARQ-001: only generated model + application DTOs referenced here.
   */
  CreateOfferingCommand fromRequest(
      UUID idempotencyKey,
      UUID programId,
      OfferingCreateRequest request,
      String requestHash,
      UUID actorId) {
    List<CreateOfferingCommand.ScheduleOptionDraft> optionDrafts =
        request.getScheduleOptions() == null
            ? Collections.emptyList()
            : request.getScheduleOptions().stream()
                .map(this::toOptionDraft)
                .toList();
    return new CreateOfferingCommand(
        idempotencyKey,
        actorId,
        programId,
        request.getCourseId(),
        request.getPeriodId(),
        request.getNotes(),
        optionDrafts,
        requestHash);
  }

  private CreateOfferingCommand.ScheduleOptionDraft toOptionDraft(
      ScheduleOptionCreateRequest req) {
    List<CreateOfferingCommand.SessionDraft> sessions =
        req.getSessions() == null
            ? Collections.emptyList()
            : req.getSessions().stream()
                .map(s -> new CreateOfferingCommand.SessionDraft(
                    s.getDay().name(),
                    LocalTime.parse(s.getStartsAt()),
                    LocalTime.parse(s.getEndsAt())))
                .toList();
    return new CreateOfferingCommand.ScheduleOptionDraft(
        req.getLabel(), req.getJourneyCode(), sessions);
  }

  /**
   * Map a REST update-offering request to the application command.
   *
   * <p>ARQ-001: only generated model + application DTOs referenced here.
   */
  UpdateOfferingCommand fromUpdateRequest(
      UUID offeringId, OfferingUpdateRequest request, UUID actorId) {
    long expectedVersion = request.getVersion() != null ? request.getVersion() : 0L;
    return new UpdateOfferingCommand(offeringId, actorId, request.getNotes(), expectedVersion);
  }

  /**
   * Map a REST publish-offering request to the application command.
   *
   * <p>ARQ-001: only generated model + application DTOs referenced here.
   */
  PublishOfferingCommand fromPublishRequest(
      UUID idempotencyKey,
      UUID offeringId,
      PublishOfferingRequest request,
      String requestHash,
      UUID actorId) {
    long expectedVersion = request.getVersion() != null ? request.getVersion() : 0L;
    boolean acknowledgeIntensityWarning =
        request.getAcknowledgeIntensityWarning() != null
            && request.getAcknowledgeIntensityWarning();
    String targetStatus = request.getTargetStatus() != null
        ? request.getTargetStatus().getValue()
        : null;
    return new PublishOfferingCommand(
        idempotencyKey,
        offeringId,
        actorId,
        targetStatus,
        expectedVersion,
        acknowledgeIntensityWarning,
        requestHash);
  }
}
