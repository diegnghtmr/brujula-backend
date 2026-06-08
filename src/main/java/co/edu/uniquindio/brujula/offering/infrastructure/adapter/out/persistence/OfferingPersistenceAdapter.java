package co.edu.uniquindio.brujula.offering.infrastructure.adapter.out.persistence;

import co.edu.uniquindio.brujula.offering.application.dto.ListOfferingsCriteria;
import co.edu.uniquindio.brujula.offering.application.dto.OfferingPageView;
import co.edu.uniquindio.brujula.offering.application.dto.OfferingSummaryView;
import co.edu.uniquindio.brujula.offering.application.port.out.LoadOfferingPort;
import co.edu.uniquindio.brujula.offering.application.port.out.LoadPeriodPort;
import co.edu.uniquindio.brujula.offering.application.port.out.LoadScheduleOptionPort;
import co.edu.uniquindio.brujula.offering.domain.AcademicPeriod;
import co.edu.uniquindio.brujula.offering.domain.AcademicPeriodId;
import co.edu.uniquindio.brujula.offering.domain.CourseOffering;
import co.edu.uniquindio.brujula.offering.domain.CourseOfferingId;
import co.edu.uniquindio.brujula.offering.domain.ScheduleOption;
import co.edu.uniquindio.brujula.offering.domain.ScheduleOptionId;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Driven adapter: JPA-backed persistence for the offering module read-side.
 *
 * <p>Implements {@link LoadPeriodPort}, {@link LoadOfferingPort}, and
 * {@link LoadScheduleOptionPort}. Package-private per ARQ-006b.
 */
@Component
@Transactional(readOnly = true)
class OfferingPersistenceAdapter
    implements LoadPeriodPort, LoadOfferingPort, LoadScheduleOptionPort {

  private final AcademicPeriodJpaRepository periodRepository;
  private final CourseOfferingJpaRepository offeringRepository;
  private final ScheduleOptionJpaRepository scheduleOptionRepository;
  private final PeriodPersistenceMapper periodMapper;
  private final OfferingPersistenceMapper offeringMapper;

  OfferingPersistenceAdapter(
      AcademicPeriodJpaRepository periodRepository,
      CourseOfferingJpaRepository offeringRepository,
      ScheduleOptionJpaRepository scheduleOptionRepository) {
    this.periodRepository = periodRepository;
    this.offeringRepository = offeringRepository;
    this.scheduleOptionRepository = scheduleOptionRepository;
    this.periodMapper = new PeriodPersistenceMapper();
    this.offeringMapper = new OfferingPersistenceMapper();
  }

  // ---- LoadPeriodPort ----

  @Override
  public List<AcademicPeriod> findAllNonArchived() {
    return periodRepository.findByStatusNotOrderByStartDateDesc("ARCHIVED").stream()
        .map(periodMapper::toDomain)
        .toList();
  }

  @Override
  public Optional<AcademicPeriod> findById(AcademicPeriodId id) {
    return periodRepository.findById(id.value())
        .map(periodMapper::toDomain);
  }

  // ---- LoadOfferingPort ----

  @Override
  public OfferingPageView findPublished(ListOfferingsCriteria criteria) {
    PageRequest pageRequest = PageRequest.of(criteria.page(), criteria.size());
    Page<CourseOfferingJpaEntity> page = offeringRepository.findPublished(
        criteria.periodId(),
        criteria.programId(),
        pageRequest);

    List<OfferingSummaryView> content = page.getContent().stream()
        .map(e -> new OfferingSummaryView(
            e.getId(),
            e.getCourseId(),
            e.getCourseName(),
            e.getProgramId(),
            e.getPeriodId(),
            e.getStatus(),
            e.getNotes(),
            e.getVersion()))
        .toList();

    return new OfferingPageView(
        content,
        page.getNumber(),
        page.getSize(),
        page.getTotalElements());
  }

  @Override
  public Optional<CourseOffering> findById(CourseOfferingId id) {
    return offeringRepository.findByIdWithOptions(id.value())
        .map(offeringMapper::toDomain);
  }

  // ---- LoadScheduleOptionPort ----

  @Override
  public Optional<ScheduleOption> findById(ScheduleOptionId id) {
    return scheduleOptionRepository.findByIdWithSessionsAndOffering(id.value())
        .map(entity -> {
          // Sessions eagerly loaded; offering eagerly loaded for offeringId reference
          return ScheduleOption.reconstitute(
              ScheduleOptionId.of(entity.getId()),
              entity.getOffering().getId(),
              entity.getLabel(),
              entity.getJourneyCode(),
              entity.getSessions().stream()
                  .map(s -> new co.edu.uniquindio.brujula.offering.domain.ScheduleSession(
                      s.getId(),
                      co.edu.uniquindio.brujula.offering.domain.Weekday.valueOf(s.getDay()),
                      s.getStartsAt(),
                      s.getEndsAt()))
                  .toList(),
              entity.getVersion());
        });
  }
}
