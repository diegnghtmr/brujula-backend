package co.edu.uniquindio.brujula.offering.adapter.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import co.edu.uniquindio.brujula.AbstractPostgresIntegrationTest;
import co.edu.uniquindio.brujula.offering.application.dto.ListOfferingsCriteria;
import co.edu.uniquindio.brujula.offering.application.dto.OfferingPageView;
import co.edu.uniquindio.brujula.offering.application.port.in.ListOfferingsQuery;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * IT: journey filter for listOfferings (student path — findPublished).
 *
 * <p>Uses V022 seeded data (period 2026-2, id 00000001-0000-4000-a000-000000000001). In V022 every
 * offering belongs to exactly one journey:
 *
 * <ul>
 *   <li>102 offerings are DIURNA-only (journey_code = 'DIURNA').
 *   <li>83 offerings are NOCTURNA-only (journey_code = 'NOCTURNA').
 *   <li>Total published offerings = 185.
 * </ul>
 *
 * <p>Known representative UUIDs:
 *
 * <ul>
 *   <li>DIURNA: 02200001-0000-4001-a000-000000000001 (first DIURNA row, page 0 under any sort)
 *   <li>NOCTURNA: 02200103-0000-4001-a000-000000000103 (first NOCTURNA row)
 * </ul>
 *
 * The test uses totalElements counts (reliable even across pagination) and spot-checks
 * representative IDs on page 0 where they are guaranteed to appear.
 */
@SpringBootTest
@Transactional
class OfferingJourneyFilterIT extends AbstractPostgresIntegrationTest {

  private static final UUID PERIOD_ID = UUID.fromString("00000001-0000-4000-a000-000000000001");

  /** First DIURNA-only offering inserted by V022. */
  private static final UUID DIURNA_ONLY_OFFERING =
      UUID.fromString("02200001-0000-4001-a000-000000000001");

  /** First NOCTURNA-only offering inserted by V022. */
  private static final UUID NOCTURNA_ONLY_OFFERING =
      UUID.fromString("02200103-0000-4001-a000-000000000103");

  @Autowired private ListOfferingsQuery listOfferingsQuery;

  // -------------------------------------------------------------------------
  // No journey filter — baseline (existing behavior must not change)
  // -------------------------------------------------------------------------

  @Test
  void noJourneyFilter_totalElementsIncludesBothJourneys() {
    // No journey filter: all published offerings are returned (185 in V022)
    ListOfferingsCriteria criteria =
        new ListOfferingsCriteria(PERIOD_ID, null, null, null, 0, 100, null, null);
    OfferingPageView page = listOfferingsQuery.listOfferings(criteria);

    // 185 total published offerings in V022 (102 DIURNA + 83 NOCTURNA)
    assertThat(page.totalElements())
        .as("Without journey filter, all published offerings must be returned")
        .isGreaterThanOrEqualTo(185L);
  }

  @Test
  void noJourneyFilter_firstPageContainsDiurnaOnlyOffering() {
    ListOfferingsCriteria criteria =
        new ListOfferingsCriteria(PERIOD_ID, null, null, null, 0, 100, null, null);
    OfferingPageView page = listOfferingsQuery.listOfferings(criteria);

    List<UUID> ids = page.content().stream().map(s -> s.id()).toList();
    assertThat(ids)
        .as("Without journey filter, DIURNA-only offering must appear on page 0 size 100")
        .contains(DIURNA_ONLY_OFFERING);
  }

  // -------------------------------------------------------------------------
  // DIURNA filter
  // -------------------------------------------------------------------------

  @Test
  void diurnaFilter_totalElementsMatchesDiurnaOnlyCount() {
    ListOfferingsCriteria criteria =
        new ListOfferingsCriteria(PERIOD_ID, null, null, null, 0, 100, null, List.of("DIURNA"));
    OfferingPageView page = listOfferingsQuery.listOfferings(criteria);

    // V022 has exactly 102 DIURNA-only offerings
    assertThat(page.totalElements())
        .as("DIURNA filter must return only DIURNA offerings (102 in V022)")
        .isEqualTo(102L);
  }

  @Test
  void diurnaFilter_firstPageContainsDiurnaOffering() {
    ListOfferingsCriteria criteria =
        new ListOfferingsCriteria(PERIOD_ID, null, null, null, 0, 100, null, List.of("DIURNA"));
    OfferingPageView page = listOfferingsQuery.listOfferings(criteria);

    List<UUID> ids = page.content().stream().map(s -> s.id()).toList();
    assertThat(ids)
        .as("DIURNA filter page 0 must contain the known DIURNA-only offering")
        .contains(DIURNA_ONLY_OFFERING);
  }

  @Test
  void diurnaFilter_doesNotIncludeNocturnaOnlyOffering() {
    // The first NOCTURNA offering is NOT in any DIURNA page — verify by checking totalElements
    // and confirming the NOCTURNA-only ID is absent on page 0
    ListOfferingsCriteria criteria =
        new ListOfferingsCriteria(PERIOD_ID, null, null, null, 0, 100, null, List.of("DIURNA"));
    OfferingPageView page = listOfferingsQuery.listOfferings(criteria);

    List<UUID> ids = page.content().stream().map(s -> s.id()).toList();
    assertThat(ids)
        .as("DIURNA filter must NOT contain the known NOCTURNA-only offering")
        .doesNotContain(NOCTURNA_ONLY_OFFERING);
  }

  // -------------------------------------------------------------------------
  // NOCTURNA filter
  // -------------------------------------------------------------------------

  @Test
  void nocturnaFilter_totalElementsMatchesNocturnaOnlyCount() {
    ListOfferingsCriteria criteria =
        new ListOfferingsCriteria(PERIOD_ID, null, null, null, 0, 100, null, List.of("NOCTURNA"));
    OfferingPageView page = listOfferingsQuery.listOfferings(criteria);

    // V022 has exactly 83 NOCTURNA-only offerings
    assertThat(page.totalElements())
        .as("NOCTURNA filter must return only NOCTURNA offerings (83 in V022)")
        .isEqualTo(83L);
  }

  @Test
  void nocturnaFilter_firstPageContainsNocturnaOffering() {
    ListOfferingsCriteria criteria =
        new ListOfferingsCriteria(PERIOD_ID, null, null, null, 0, 100, null, List.of("NOCTURNA"));
    OfferingPageView page = listOfferingsQuery.listOfferings(criteria);

    List<UUID> ids = page.content().stream().map(s -> s.id()).toList();
    assertThat(ids)
        .as("NOCTURNA filter page 0 must contain the known NOCTURNA-only offering")
        .contains(NOCTURNA_ONLY_OFFERING);
  }

  @Test
  void nocturnaFilter_doesNotIncludeDiurnaOnlyOffering() {
    ListOfferingsCriteria criteria =
        new ListOfferingsCriteria(PERIOD_ID, null, null, null, 0, 100, null, List.of("NOCTURNA"));
    OfferingPageView page = listOfferingsQuery.listOfferings(criteria);

    List<UUID> ids = page.content().stream().map(s -> s.id()).toList();
    assertThat(ids)
        .as("NOCTURNA filter must NOT contain the known DIURNA-only offering")
        .doesNotContain(DIURNA_ONLY_OFFERING);
  }

  // -------------------------------------------------------------------------
  // Both journeys filter
  // -------------------------------------------------------------------------

  @Test
  void bothJourneysFilter_totalElementsMatchesCombinedCount() {
    ListOfferingsCriteria criteria =
        new ListOfferingsCriteria(
            PERIOD_ID, null, null, null, 0, 100, null, List.of("DIURNA", "NOCTURNA"));
    OfferingPageView page = listOfferingsQuery.listOfferings(criteria);

    // DIURNA(102) + NOCTURNA(83) = 185 total
    assertThat(page.totalElements())
        .as("DIURNA+NOCTURNA filter must return all published offerings (185 in V022)")
        .isGreaterThanOrEqualTo(185L);
  }
}
