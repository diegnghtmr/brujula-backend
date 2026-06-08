package co.edu.uniquindio.brujula.offering.infrastructure.adapter.in.rest;

import co.edu.uniquindio.brujula.generated.api.OfferingsApi;
import co.edu.uniquindio.brujula.generated.model.AcademicPeriod;
import co.edu.uniquindio.brujula.generated.model.OfferingCreateRequest;
import co.edu.uniquindio.brujula.generated.model.OfferingDetail;
import co.edu.uniquindio.brujula.generated.model.OfferingPage;
import co.edu.uniquindio.brujula.generated.model.OfferingStatus;
import co.edu.uniquindio.brujula.generated.model.OfferingUpdateRequest;
import co.edu.uniquindio.brujula.generated.model.PeriodStatus;
import co.edu.uniquindio.brujula.generated.model.PublishOfferingRequest;
import co.edu.uniquindio.brujula.generated.model.ScheduleOption;
import co.edu.uniquindio.brujula.generated.model.ScheduleOptionCreateRequest;
import co.edu.uniquindio.brujula.generated.model.ScheduleOptionUpdateRequest;
import co.edu.uniquindio.brujula.offering.application.dto.ListOfferingsCriteria;
import co.edu.uniquindio.brujula.offering.application.dto.OfferingDetailView;
import co.edu.uniquindio.brujula.offering.application.dto.OfferingPageView;
import co.edu.uniquindio.brujula.offering.application.port.in.GetOfferingQuery;
import co.edu.uniquindio.brujula.offering.application.port.in.ListOfferingsQuery;
import co.edu.uniquindio.brujula.offering.application.port.in.ListPeriodsQuery;

import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * REST adapter: offering catalogue endpoints ({@code /periods}, {@code /offerings/**}).
 *
 * <p>Implements the generated {@link OfferingsApi} contract-first interface (ARQ-001).
 * Package-private per ARQ-006b. Read endpoints (GET /periods, GET /offerings, GET /offerings/{id})
 * are accessible by STUDENT role. Staff write endpoints are stubbed for WU2+.
 *
 * <p>Satisfies: OFF-001, OFF-002, OFF-003, API-001.
 */
@RestController
class OfferingController implements OfferingsApi {

  private final ListPeriodsQuery listPeriodsQuery;
  private final ListOfferingsQuery listOfferingsQuery;
  private final GetOfferingQuery getOfferingQuery;
  private final OfferingRestMapper restMapper;

  OfferingController(
      ListPeriodsQuery listPeriodsQuery,
      ListOfferingsQuery listOfferingsQuery,
      GetOfferingQuery getOfferingQuery) {
    this.listPeriodsQuery = listPeriodsQuery;
    this.listOfferingsQuery = listOfferingsQuery;
    this.getOfferingQuery = getOfferingQuery;
    this.restMapper = new OfferingRestMapper();
  }

  // -------------------------------------------------------------------------
  // GET /periods — list academic periods (student-readable)
  // -------------------------------------------------------------------------

  @Override
  @PreAuthorize("hasAnyRole('STUDENT', 'STAFF', 'ADMIN')")
  public ResponseEntity<List<AcademicPeriod>> listPeriods(
      @Nullable PeriodStatus status) {
    // status filter mapped from generated PeriodStatus enum (null = all non-archived)
    String statusFilter = status != null ? status.name() : null;
    List<AcademicPeriod> periods = listPeriodsQuery.listPeriods(statusFilter).stream()
        .map(restMapper::toContract)
        .toList();
    return ResponseEntity.ok(periods);
  }

  // -------------------------------------------------------------------------
  // GET /offerings — list published offerings (student-readable)
  // -------------------------------------------------------------------------

  @Override
  @PreAuthorize("hasAnyRole('STUDENT', 'STAFF', 'ADMIN')")
  public ResponseEntity<OfferingPage> listOfferings(
      UUID periodId,
      @Nullable UUID programId,
      @Nullable UUID courseId,
      @Nullable OfferingStatus status,
      Integer page,
      Integer size,
      @Nullable List<String> sort) {
    ListOfferingsCriteria criteria = new ListOfferingsCriteria(
        periodId,
        programId,
        null,   // courseId filter — reserved for future use
        status != null ? status.name() : null,
        page == null ? 0 : page,
        size == null ? 20 : size);
    OfferingPageView pageView = listOfferingsQuery.listOfferings(criteria);
    return ResponseEntity.ok(restMapper.toContractPage(pageView));
  }

  // -------------------------------------------------------------------------
  // GET /offerings/{offeringId} — get offering detail (student-readable)
  // -------------------------------------------------------------------------

  @Override
  @PreAuthorize("hasAnyRole('STUDENT', 'STAFF', 'ADMIN')")
  public ResponseEntity<OfferingDetail> getOffering(UUID offeringId) {
    OfferingDetailView detail = getOfferingQuery.getOffering(offeringId);
    return ResponseEntity.ok(restMapper.toContractDetail(detail));
  }

  // -------------------------------------------------------------------------
  // Staff write endpoints — NOT IMPLEMENTED in WU1 (WU2+)
  // -------------------------------------------------------------------------

  @Override
  public ResponseEntity<OfferingDetail> createOffering(
      UUID idempotencyKey, String X_XSRF_TOKEN, UUID programId,
      OfferingCreateRequest offeringCreateRequest) {
    throw new UnsupportedOperationException("createOffering not implemented in WU1");
  }

  @Override
  public ResponseEntity<ScheduleOption> createScheduleOption(
      String X_XSRF_TOKEN, UUID offeringId,
      ScheduleOptionCreateRequest scheduleOptionCreateRequest) {
    throw new UnsupportedOperationException("createScheduleOption not implemented in WU1");
  }

  @Override
  public ResponseEntity<Void> deleteScheduleOption(String X_XSRF_TOKEN, UUID optionId) {
    throw new UnsupportedOperationException("deleteScheduleOption not implemented in WU1");
  }

  @Override
  public ResponseEntity<OfferingDetail> publishOffering(
      UUID idempotencyKey, String X_XSRF_TOKEN, UUID offeringId,
      PublishOfferingRequest publishOfferingRequest) {
    throw new UnsupportedOperationException("publishOffering not implemented in WU1");
  }

  @Override
  public ResponseEntity<OfferingDetail> updateOffering(
      String X_XSRF_TOKEN, UUID offeringId,
      OfferingUpdateRequest offeringUpdateRequest) {
    throw new UnsupportedOperationException("updateOffering not implemented in WU1");
  }

  @Override
  public ResponseEntity<ScheduleOption> updateScheduleOption(
      String X_XSRF_TOKEN, UUID optionId,
      ScheduleOptionUpdateRequest scheduleOptionUpdateRequest) {
    throw new UnsupportedOperationException("updateScheduleOption not implemented in WU1");
  }
}
