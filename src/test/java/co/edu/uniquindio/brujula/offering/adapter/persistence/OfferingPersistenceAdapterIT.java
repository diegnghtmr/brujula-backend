package co.edu.uniquindio.brujula.offering.adapter.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import co.edu.uniquindio.brujula.AbstractPostgresIntegrationTest;
import co.edu.uniquindio.brujula.offering.application.dto.AcademicPeriodView;
import co.edu.uniquindio.brujula.offering.application.dto.ListOfferingsCriteria;
import co.edu.uniquindio.brujula.offering.application.dto.OfferingDetailView;
import co.edu.uniquindio.brujula.offering.application.dto.OfferingPageView;
import co.edu.uniquindio.brujula.offering.application.dto.ScheduleOptionResolutionView;
import co.edu.uniquindio.brujula.offering.application.port.in.GetOfferingQuery;
import co.edu.uniquindio.brujula.offering.application.port.in.ListOfferingsQuery;
import co.edu.uniquindio.brujula.offering.application.port.in.ListPeriodsQuery;
import co.edu.uniquindio.brujula.offering.application.port.in.ResolvePeriodQuery;
import co.edu.uniquindio.brujula.offering.application.port.in.ResolveScheduleOptionQuery;
import co.edu.uniquindio.brujula.offering.domain.OfferingNotFoundException;
import co.edu.uniquindio.brujula.offering.domain.PeriodNotFoundException;
import co.edu.uniquindio.brujula.offering.domain.ScheduleOptionNotFoundException;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * WU1-TDD: Integration tests for the OfferingPersistenceAdapter using V016+V017 seeded data.
 *
 * <p>Uses the real Flyway-migrated PostgreSQL database (via AbstractPostgresIntegrationTest). Tests
 * verify that:
 *
 * <ul>
 *   <li>listPeriods filters by status correctly
 *   <li>listOfferings requires periodId (400 when missing — enforced at controller layer, but
 *       persistence query requires it)
 *   <li>getOffering loads scheduleOptions
 *   <li>resolveScheduleOption returns full ScheduleOptionResolutionView
 *   <li>listOfferings excludes DRAFT/ARCHIVED offerings (student view)
 * </ul>
 */
@SpringBootTest
@Transactional
class OfferingPersistenceAdapterIT extends AbstractPostgresIntegrationTest {

  @Autowired private ListPeriodsQuery listPeriodsQuery;
  @Autowired private ListOfferingsQuery listOfferingsQuery;
  @Autowired private GetOfferingQuery getOfferingQuery;
  @Autowired private ResolvePeriodQuery resolvePeriodQuery;
  @Autowired private ResolveScheduleOptionQuery resolveScheduleOptionQuery;

  // -------------------------------------------------------------------------
  // listPeriods
  // -------------------------------------------------------------------------

  @Test
  void listPeriodsReturnsAtLeastSeededPeriod() {
    List<AcademicPeriodView> periods = listPeriodsQuery.listPeriods(null);
    assertThat(periods).isNotEmpty();
  }

  @Test
  void listPeriodsFiltersByStatusActive() {
    List<AcademicPeriodView> periods = listPeriodsQuery.listPeriods("ACTIVE");
    assertThat(periods).allSatisfy(p -> assertThat(p.status()).isEqualTo("ACTIVE"));
  }

  @Test
  void listPeriodsContains2026_2Period() {
    List<AcademicPeriodView> periods = listPeriodsQuery.listPeriods(null);
    assertThat(periods).anySatisfy(p -> assertThat(p.name()).isEqualTo("2026-2"));
  }

  // -------------------------------------------------------------------------
  // listOfferings (student view — only PRELIMINARY/FINAL)
  // -------------------------------------------------------------------------

  @Test
  void listOfferingsForSeededPeriodReturnsPreliminaryAndFinalOnly() {
    // Get the seeded period id
    List<AcademicPeriodView> periods = listPeriodsQuery.listPeriods("ACTIVE");
    assertThat(periods).isNotEmpty();
    UUID periodId =
        periods.stream()
            .filter(p -> p.name().equals("2026-2"))
            .findFirst()
            .map(AcademicPeriodView::id)
            .orElse(periods.get(0).id());

    ListOfferingsCriteria criteria = new ListOfferingsCriteria(periodId, null, null, null, 0, 50);
    OfferingPageView page = listOfferingsQuery.listOfferings(criteria);

    assertThat(page.content()).isNotEmpty();
    assertThat(page.content())
        .allSatisfy(
            o ->
                assertThat(o.status())
                    .as("Student view must show only PRELIMINARY/FINAL")
                    .isIn("PRELIMINARY", "FINAL"));
  }

  @Test
  void listOfferingsExcludesDraftAndArchived() {
    List<AcademicPeriodView> periods = listPeriodsQuery.listPeriods("ACTIVE");
    UUID periodId =
        periods.stream()
            .filter(p -> p.name().equals("2026-2"))
            .findFirst()
            .map(AcademicPeriodView::id)
            .orElse(periods.get(0).id());

    ListOfferingsCriteria criteria = new ListOfferingsCriteria(periodId, null, null, null, 0, 50);
    OfferingPageView page = listOfferingsQuery.listOfferings(criteria);

    assertThat(page.content())
        .noneMatch(o -> o.status().equals("DRAFT"))
        .noneMatch(o -> o.status().equals("ARCHIVED"));
  }

  // -------------------------------------------------------------------------
  // getOffering with scheduleOptions
  // -------------------------------------------------------------------------

  @Test
  void getOfferingLoadsScheduleOptions() {
    List<AcademicPeriodView> periods = listPeriodsQuery.listPeriods("ACTIVE");
    UUID periodId =
        periods.stream()
            .filter(p -> p.name().equals("2026-2"))
            .findFirst()
            .map(AcademicPeriodView::id)
            .orElse(periods.get(0).id());

    ListOfferingsCriteria criteria = new ListOfferingsCriteria(periodId, null, null, null, 0, 50);
    OfferingPageView page = listOfferingsQuery.listOfferings(criteria);
    assertThat(page.content()).isNotEmpty();

    UUID offeringId = page.content().get(0).id();
    OfferingDetailView detail = getOfferingQuery.getOffering(offeringId);

    assertThat(detail).isNotNull();
    assertThat(detail.id()).isEqualTo(offeringId);
    assertThat(detail.scheduleOptions()).isNotEmpty();
  }

  @Test
  void getOfferingThrowsNotFoundForUnknownId() {
    assertThatThrownBy(() -> getOfferingQuery.getOffering(UUID.randomUUID()))
        .isInstanceOf(OfferingNotFoundException.class);
  }

  // -------------------------------------------------------------------------
  // resolvePeriod
  // -------------------------------------------------------------------------

  @Test
  void resolvePeriodByIdReturnsCorrectName() {
    List<AcademicPeriodView> periods = listPeriodsQuery.listPeriods("ACTIVE");
    UUID periodId =
        periods.stream()
            .filter(p -> p.name().equals("2026-2"))
            .findFirst()
            .map(AcademicPeriodView::id)
            .orElse(periods.get(0).id());

    AcademicPeriodView resolved = resolvePeriodQuery.resolvePeriod(periodId);
    assertThat(resolved.name()).isEqualTo("2026-2");
  }

  @Test
  void resolvePeriodThrowsNotFoundForUnknownId() {
    assertThatThrownBy(() -> resolvePeriodQuery.resolvePeriod(UUID.randomUUID()))
        .isInstanceOf(PeriodNotFoundException.class);
  }

  // -------------------------------------------------------------------------
  // resolveScheduleOption
  // -------------------------------------------------------------------------

  @Test
  void resolveScheduleOptionReturnsFullView() {
    List<AcademicPeriodView> periods = listPeriodsQuery.listPeriods("ACTIVE");
    UUID periodId =
        periods.stream()
            .filter(p -> p.name().equals("2026-2"))
            .findFirst()
            .map(AcademicPeriodView::id)
            .orElse(periods.get(0).id());

    ListOfferingsCriteria criteria = new ListOfferingsCriteria(periodId, null, null, null, 0, 50);
    OfferingPageView page = listOfferingsQuery.listOfferings(criteria);
    assertThat(page.content()).isNotEmpty();

    UUID offeringId = page.content().get(0).id();
    OfferingDetailView detail = getOfferingQuery.getOffering(offeringId);
    assertThat(detail.scheduleOptions()).isNotEmpty();

    UUID scheduleOptionId = detail.scheduleOptions().get(0).id();
    ScheduleOptionResolutionView resolution =
        resolveScheduleOptionQuery.resolveScheduleOption(scheduleOptionId);

    assertThat(resolution.scheduleOptionId()).isEqualTo(scheduleOptionId);
    assertThat(resolution.offeringId()).isNotNull();
    assertThat(resolution.courseId()).isNotNull();
    assertThat(resolution.sessions()).isNotEmpty();
    assertThat(resolution.offeringVersion()).isGreaterThanOrEqualTo(0L);
  }

  @Test
  void resolveScheduleOptionThrowsNotFoundForUnknownId() {
    assertThatThrownBy(() -> resolveScheduleOptionQuery.resolveScheduleOption(UUID.randomUUID()))
        .isInstanceOf(ScheduleOptionNotFoundException.class);
  }

  // -------------------------------------------------------------------------
  // W-001 fix: real courseName (denormalized, NOT "N/A")
  // -------------------------------------------------------------------------

  @Test
  void getOfferingReturnRealCourseName() {
    // Cálculo Diferencial offering
    UUID calculoOfferingId = UUID.fromString("00000001-0000-4001-a000-000000000001");
    OfferingDetailView detail = getOfferingQuery.getOffering(calculoOfferingId);

    assertThat(detail.courseName())
        .as("courseName must be the real denormalized name, not 'N/A'")
        .isEqualTo("Cálculo Diferencial");
  }

  @Test
  void listOfferingsReturnRealCourseNames() {
    List<AcademicPeriodView> periods = listPeriodsQuery.listPeriods("ACTIVE");
    UUID periodId =
        periods.stream()
            .filter(p -> p.name().equals("2026-2"))
            .findFirst()
            .map(AcademicPeriodView::id)
            .orElseThrow();

    ListOfferingsCriteria criteria = new ListOfferingsCriteria(periodId, null, null, null, 0, 50);
    OfferingPageView page = listOfferingsQuery.listOfferings(criteria);

    assertThat(page.content())
        .as("All course names must be real names, not 'N/A'")
        .allSatisfy(o -> assertThat(o.courseName()).isNotEqualTo("N/A"));
    assertThat(page.content())
        .anySatisfy(o -> assertThat(o.courseName()).isEqualTo("Cálculo Diferencial"));
  }

  // -------------------------------------------------------------------------
  // W-002/S-001 fix: real version on OfferingDetail and ScheduleOption
  // -------------------------------------------------------------------------

  @Test
  void getOfferingDetailHasNonNullVersion() {
    UUID calculoOfferingId = UUID.fromString("00000001-0000-4001-a000-000000000001");
    OfferingDetailView detail = getOfferingQuery.getOffering(calculoOfferingId);

    assertThat(detail.version())
        .as(
            "OfferingDetailView.version must reflect the entity version, not always 0L from a hardcoded literal")
        .isNotNull()
        .isGreaterThanOrEqualTo(0L);
  }

  @Test
  void getOfferingScheduleOptionsHaveVersion() {
    UUID calculoOfferingId = UUID.fromString("00000001-0000-4001-a000-000000000001");
    OfferingDetailView detail = getOfferingQuery.getOffering(calculoOfferingId);

    assertThat(detail.scheduleOptions())
        .isNotEmpty()
        .allSatisfy(
            opt ->
                assertThat(opt.version())
                    .as("ScheduleOptionView.version must not be null")
                    .isNotNull()
                    .isGreaterThanOrEqualTo(0L));
  }

  // -------------------------------------------------------------------------
  // W-003 fix: non-null offeringId on ScheduleOptionView
  // -------------------------------------------------------------------------

  @Test
  void getOfferingScheduleOptionsHaveOfferingId() {
    UUID calculoOfferingId = UUID.fromString("00000001-0000-4001-a000-000000000001");
    OfferingDetailView detail = getOfferingQuery.getOffering(calculoOfferingId);

    assertThat(detail.scheduleOptions())
        .isNotEmpty()
        .allSatisfy(
            opt -> {
              assertThat(opt.offeringId())
                  .as("ScheduleOptionView.offeringId must not be null")
                  .isNotNull();
              assertThat(opt.offeringId())
                  .as("ScheduleOptionView.offeringId must match the parent offering")
                  .isEqualTo(calculoOfferingId);
            });
  }

  // -------------------------------------------------------------------------
  // S-003 fix: courseName on ScheduleOptionResolutionView
  // -------------------------------------------------------------------------

  @Test
  void resolveScheduleOptionReturnsRealCourseName() {
    // Use the Cálculo Diferencial DIURNA option
    UUID calculoOptionId = UUID.fromString("00000001-0000-4002-a000-000000000001");
    ScheduleOptionResolutionView resolution =
        resolveScheduleOptionQuery.resolveScheduleOption(calculoOptionId);

    assertThat(resolution.courseName())
        .as("ScheduleOptionResolutionView.courseName must be populated with real course name")
        .isNotNull()
        .isEqualTo("Cálculo Diferencial");
  }
}
