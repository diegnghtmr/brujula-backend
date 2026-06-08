package co.edu.uniquindio.brujula.simulation.infrastructure.adapter.in.rest;

import co.edu.uniquindio.brujula.generated.model.OfferingStatus;
import co.edu.uniquindio.brujula.generated.model.ProgramCreditSummary;
import co.edu.uniquindio.brujula.generated.model.Simulation;
import co.edu.uniquindio.brujula.generated.model.SimulationDetail;
import co.edu.uniquindio.brujula.generated.model.SimulationPage;
import co.edu.uniquindio.brujula.generated.model.SimulationSelection;
import co.edu.uniquindio.brujula.generated.model.SimulationStatus;
import co.edu.uniquindio.brujula.generated.model.SimulationValidation;
import co.edu.uniquindio.brujula.generated.model.ValidationNotice;
import co.edu.uniquindio.brujula.generated.model.Weekday;
import co.edu.uniquindio.brujula.generated.model.WeeklyScheduleEntry;
import co.edu.uniquindio.brujula.simulation.application.dto.SimulationDetailView;
import co.edu.uniquindio.brujula.simulation.application.dto.SimulationPageView;
import co.edu.uniquindio.brujula.simulation.application.dto.SimulationSelectionView;
import co.edu.uniquindio.brujula.simulation.application.dto.SimulationView;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Maps between simulation application-layer views and generated API models.
 *
 * <p>Package-private per ARQ-006b.
 */
@Component
class SimulationRestMapper {

  Simulation toDto(SimulationView view) {
    OffsetDateTime createdAt = view.createdAt() != null
        ? view.createdAt().atOffset(ZoneOffset.UTC)
        : OffsetDateTime.now(ZoneOffset.UTC);
    OffsetDateTime updatedAt = view.updatedAt() != null
        ? view.updatedAt().atOffset(ZoneOffset.UTC)
        : OffsetDateTime.now(ZoneOffset.UTC);

    SimulationStatus status = parseStatus(view.status());

    return new Simulation(
        view.id(),
        view.name(),
        view.periodId(),
        view.periodName(),
        view.programIds(),
        status,
        createdAt,
        updatedAt,
        view.version());
  }

  SimulationPage toPageDto(SimulationPageView pageView) {
    List<Simulation> content = pageView.content().stream().map(this::toDto).toList();
    return new SimulationPage(
        pageView.page(),
        pageView.size(),
        pageView.totalElements(),
        pageView.totalPages(),
        content);
  }

  /**
   * Map a {@link SimulationSelectionView} to the generated {@link SimulationSelection} API model.
   */
  SimulationSelection toSelectionDto(SimulationSelectionView view) {
    List<co.edu.uniquindio.brujula.generated.model.ScheduleSession> sessions =
        view.sessions().stream()
            .map(s -> {
              co.edu.uniquindio.brujula.generated.model.ScheduleSession session =
                  new co.edu.uniquindio.brujula.generated.model.ScheduleSession();
              session.setDay(parseWeekday(s.day()));
              session.setStartsAt(s.startsAt());
              session.setEndsAt(s.endsAt());
              return session;
            })
            .toList();

    OfferingStatus offeringStatus = parseOfferingStatus(view.offeringStatus());

    SimulationSelection result = new SimulationSelection(
        view.id(),
        view.offeringId(),
        view.scheduleOptionId(),
        view.courseId(),
        view.courseName(),
        view.programId(),
        view.credits(),
        view.journeyCode(),
        offeringStatus,
        sessions);

    result.setSelectedOfferingVersion(view.selectedOfferingVersion());

    // Map warnings (WU4a: always empty)
    List<ValidationNotice> warningModels = view.warnings() != null
        ? view.warnings().stream()
            .map(w -> {
              ValidationNotice vn = new ValidationNotice();
              vn.setCode(w.code());
              vn.setMessage(w.message());
              return vn;
            }).toList()
        : new ArrayList<>();
    result.setWarnings(warningModels);

    return result;
  }

  /**
   * Map a {@link SimulationDetailView} to the generated {@link SimulationDetail} API model.
   * Includes selections, creditSummary, weeklySchedule, validation, totalWeeklyHours.
   */
  SimulationDetail toDetailDto(SimulationDetailView view) {
    OffsetDateTime createdAt = view.createdAt() != null
        ? view.createdAt().atOffset(ZoneOffset.UTC)
        : OffsetDateTime.now(ZoneOffset.UTC);
    OffsetDateTime updatedAt = view.updatedAt() != null
        ? view.updatedAt().atOffset(ZoneOffset.UTC)
        : OffsetDateTime.now(ZoneOffset.UTC);

    SimulationStatus status = parseStatus(view.statusName());

    // Map selections
    List<SimulationSelection> selections = view.selections() != null
        ? view.selections().stream().map(this::toSelectionDto).toList()
        : new ArrayList<>();

    // Map credit summary — field names match generated ProgramCreditSummary EXACTLY
    List<ProgramCreditSummary> creditSummary = view.creditSummary() != null
        ? view.creditSummary().stream()
            .map(cs -> new ProgramCreditSummary(
                cs.programId(),
                cs.programName(),
                cs.selectedCredits(),
                cs.creditLimit(),
                cs.withinLimit()))
            .toList()
        : new ArrayList<>();

    // Map weekly schedule — field names match generated WeeklyScheduleEntry EXACTLY
    List<WeeklyScheduleEntry> weeklySchedule = view.weeklySchedule() != null
        ? view.weeklySchedule().stream()
            .map(we -> {
              WeeklyScheduleEntry entry = new WeeklyScheduleEntry(
                  parseWeekday(we.day()),
                  we.startsAt(),
                  we.endsAt(),
                  we.courseName(),
                  we.programId());
              entry.setSelectionId(we.selectionId());
              entry.setInClash(we.inClash());
              return entry;
            })
            .toList()
        : new ArrayList<>();

    // Map validation
    SimulationValidation validation;
    if (view.validation() != null) {
      List<ValidationNotice> blocking = view.validation().blocking() != null
          ? view.validation().blocking().stream()
              .map(n -> {
                ValidationNotice vn = new ValidationNotice();
                vn.setCode(n.code());
                vn.setMessage(n.message());
                return vn;
              }).toList()
          : new ArrayList<>();
      List<ValidationNotice> warnings = view.validation().warnings() != null
          ? view.validation().warnings().stream()
              .map(n -> {
                ValidationNotice vn = new ValidationNotice();
                vn.setCode(n.code());
                vn.setMessage(n.message());
                return vn;
              }).toList()
          : new ArrayList<>();
      validation = new SimulationValidation(blocking, warnings);
    } else {
      validation = new SimulationValidation(new ArrayList<>(), new ArrayList<>());
    }

    SimulationDetail detail = new SimulationDetail(
        view.id(),
        view.name(),
        view.periodId(),
        view.periodName(),
        view.programIds(),
        status,
        createdAt,
        updatedAt,
        view.version(),
        selections,
        creditSummary,
        weeklySchedule,
        validation);

    detail.setTotalWeeklyHours(view.totalWeeklyHours());
    return detail;
  }

  private SimulationStatus parseStatus(String status) {
    if (status == null) return SimulationStatus.CURRENT;
    return switch (status) {
      case "OUTDATED" -> SimulationStatus.OUTDATED;
      default -> SimulationStatus.CURRENT;
    };
  }

  private Weekday parseWeekday(String day) {
    if (day == null) return null;
    try {
      return Weekday.valueOf(day.toUpperCase());
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  private OfferingStatus parseOfferingStatus(String status) {
    if (status == null) return OfferingStatus.PRELIMINARY;
    try {
      return OfferingStatus.valueOf(status.toUpperCase());
    } catch (IllegalArgumentException e) {
      return OfferingStatus.PRELIMINARY;
    }
  }
}
