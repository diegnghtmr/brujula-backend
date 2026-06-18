package co.edu.uniquindio.brujula.offering.infrastructure.adapter.out.persistence;

import co.edu.uniquindio.brujula.offering.application.dto.ListOfferingsCriteria;
import co.edu.uniquindio.brujula.offering.application.dto.OfferingPageView;
import co.edu.uniquindio.brujula.offering.application.dto.OfferingSummaryView;
import co.edu.uniquindio.brujula.offering.application.port.out.LoadOfferingPort;
import co.edu.uniquindio.brujula.offering.application.port.out.LoadPeriodPort;
import co.edu.uniquindio.brujula.offering.application.port.out.LoadScheduleOptionPort;
import co.edu.uniquindio.brujula.offering.application.port.out.SaveOfferingPort;
import co.edu.uniquindio.brujula.offering.application.port.out.SavePeriodPort;
import co.edu.uniquindio.brujula.offering.domain.AcademicPeriod;
import co.edu.uniquindio.brujula.offering.domain.AcademicPeriodId;
import co.edu.uniquindio.brujula.offering.domain.CourseOffering;
import co.edu.uniquindio.brujula.offering.domain.CourseOfferingId;
import co.edu.uniquindio.brujula.offering.domain.ScheduleOption;
import co.edu.uniquindio.brujula.offering.domain.ScheduleOptionId;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Driven adapter: JPA-backed persistence for the offering module — both read-side and write-side.
 *
 * <p>Implements {@link LoadPeriodPort}, {@link LoadOfferingPort}, {@link LoadScheduleOptionPort},
 * {@link SavePeriodPort}, and {@link SaveOfferingPort}. Package-private.
 *
 * <p>Transaction strategy: class-level {@code @Transactional} (read-write) so write methods inherit
 * a writable transaction; each read method explicitly overrides with
 * {@code @Transactional(readOnly=true)} for performance (avoids dirty-checking overhead).
 */
@Component
@Transactional
class OfferingPersistenceAdapter
    implements LoadPeriodPort,
        LoadOfferingPort,
        LoadScheduleOptionPort,
        SavePeriodPort,
        SaveOfferingPort {

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
  @Transactional(readOnly = true)
  public List<AcademicPeriod> findAllNonArchived() {
    return periodRepository.findByStatusNotOrderByStartDateDesc("ARCHIVED").stream()
        .map(periodMapper::toDomain)
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<AcademicPeriod> findById(AcademicPeriodId id) {
    return periodRepository.findById(id.value()).map(periodMapper::toDomain);
  }

  // ---- SavePeriodPort ----

  @Override
  public AcademicPeriod save(AcademicPeriod period) {
    AcademicPeriodJpaEntity entity = periodMapper.toJpaEntity(period);
    AcademicPeriodJpaEntity managed = periodRepository.save(entity);
    periodRepository.flush();
    return periodMapper.toDomain(managed);
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<AcademicPeriod> findCurrentById(AcademicPeriodId id) {
    return periodRepository.findById(id.value()).map(periodMapper::toDomain);
  }

  // ---- SaveOfferingPort ----

  @Override
  public CourseOffering save(CourseOffering offering) {
    CourseOfferingJpaEntity entity = offeringMapper.toJpaEntity(offering);
    CourseOfferingJpaEntity managed = offeringRepository.save(entity);
    offeringRepository.flush();
    return offeringMapper.toDomain(managed);
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<CourseOffering> findCurrentById(CourseOfferingId id) {
    return offeringRepository.findByIdWithOptions(id.value()).map(offeringMapper::toDomain);
  }

  // ---- LoadOfferingPort ----

  @Override
  @Transactional(readOnly = true)
  public OfferingPageView findForStaff(ListOfferingsCriteria criteria) {
    PageRequest pageRequest = PageRequest.of(criteria.page(), criteria.size());
    // Pass status filter if present (e.g. DRAFT); null means all non-ARCHIVED
    String statusFilter = criteria.status();
    boolean journeyFilterDisabled = criteria.isJourneyFilterDisabled();
    Collection<String> journeyCodes =
        journeyFilterDisabled ? List.of("_placeholder_") : criteria.journeyCodes();
    Page<CourseOfferingJpaEntity> page =
        offeringRepository.findForStaff(
            criteria.periodId(),
            criteria.programId(),
            statusFilter,
            journeyFilterDisabled,
            journeyCodes,
            pageRequest);

    List<OfferingSummaryView> content =
        page.getContent().stream()
            .map(
                e ->
                    new OfferingSummaryView(
                        e.getId(),
                        e.getCourseId(),
                        e.getCourseName(),
                        e.getProgramId(),
                        e.getPeriodId(),
                        e.getStatus(),
                        e.getNotes(),
                        e.getVersion()))
            .toList();

    return new OfferingPageView(content, page.getNumber(), page.getSize(), page.getTotalElements());
  }

  @Override
  @Transactional(readOnly = true)
  public OfferingPageView findPublished(ListOfferingsCriteria criteria) {
    PageRequest pageRequest = PageRequest.of(criteria.page(), criteria.size());
    boolean journeyFilterDisabled = criteria.isJourneyFilterDisabled();
    Collection<String> journeyCodes =
        journeyFilterDisabled ? List.of("_placeholder_") : criteria.journeyCodes();
    Page<CourseOfferingJpaEntity> page =
        offeringRepository.findPublished(
            criteria.periodId(),
            criteria.programId(),
            journeyFilterDisabled,
            journeyCodes,
            pageRequest);

    List<OfferingSummaryView> content =
        page.getContent().stream()
            .map(
                e ->
                    new OfferingSummaryView(
                        e.getId(),
                        e.getCourseId(),
                        e.getCourseName(),
                        e.getProgramId(),
                        e.getPeriodId(),
                        e.getStatus(),
                        e.getNotes(),
                        e.getVersion()))
            .toList();

    return new OfferingPageView(content, page.getNumber(), page.getSize(), page.getTotalElements());
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<CourseOffering> findById(CourseOfferingId id) {
    return offeringRepository.findByIdWithOptions(id.value()).map(offeringMapper::toDomain);
  }

  // ---- LoadScheduleOptionPort ----

  @Override
  @Transactional(readOnly = true)
  public Optional<ScheduleOption> findById(ScheduleOptionId id) {
    return scheduleOptionRepository
        .findByIdWithSessionsAndOffering(id.value())
        .map(
            entity -> {
              // Sessions eagerly loaded; offering eagerly loaded for offeringId reference
              return ScheduleOption.reconstitute(
                  ScheduleOptionId.of(entity.getId()),
                  entity.getOffering().getId(),
                  entity.getLabel(),
                  entity.getJourneyCode(),
                  entity.getSessions().stream()
                      .map(
                          s ->
                              new co.edu.uniquindio.brujula.offering.domain.ScheduleSession(
                                  s.getId(),
                                  co.edu.uniquindio.brujula.offering.domain.Weekday.valueOf(
                                      s.getDay()),
                                  s.getStartsAt(),
                                  s.getEndsAt()))
                      .toList(),
                  entity.getVersion());
            });
  }
}
