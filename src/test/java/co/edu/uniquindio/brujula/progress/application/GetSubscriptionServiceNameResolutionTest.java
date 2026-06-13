package co.edu.uniquindio.brujula.progress.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import co.edu.uniquindio.brujula.programcatalog.application.dto.JourneyRef;
import co.edu.uniquindio.brujula.programcatalog.application.dto.ProgramView;
import co.edu.uniquindio.brujula.programcatalog.application.dto.StudyPlanView;
import co.edu.uniquindio.brujula.programcatalog.application.port.in.GetProgramQuery;
import co.edu.uniquindio.brujula.programcatalog.application.port.in.GetStudyPlanQuery;
import co.edu.uniquindio.brujula.programcatalog.application.port.in.ListJourneysQuery;
import co.edu.uniquindio.brujula.progress.application.dto.SubscriptionView;
import co.edu.uniquindio.brujula.progress.application.port.out.LoadSubscriptionPort;
import co.edu.uniquindio.brujula.progress.application.service.GetSubscriptionService;
import co.edu.uniquindio.brujula.progress.domain.Subscription;
import co.edu.uniquindio.brujula.progress.domain.SubscriptionId;
import co.edu.uniquindio.brujula.progress.domain.SubscriptionStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Asserts that {@link GetSubscriptionService} resolves programName, planName, and
 * referenceJourneyName from catalog ports.
 *
 * <p>Mirrors the pattern established in {@code GetSubscriptionProgressServiceTest}.
 */
class GetSubscriptionServiceNameResolutionTest {

  private LoadSubscriptionPort loadSubscriptionPort;
  private GetProgramQuery getProgramQuery;
  private GetStudyPlanQuery getStudyPlanQuery;
  private ListJourneysQuery listJourneysQuery;
  private GetSubscriptionService sut;

  private static final UUID USER_ID = UUID.randomUUID();
  private static final UUID SUB_ID = UUID.randomUUID();
  private static final UUID PROGRAM_ID = UUID.randomUUID();
  private static final UUID PLAN_ID = UUID.randomUUID();
  private static final UUID JOURNEY_ID = UUID.randomUUID();

  private static final String EXPECTED_PROGRAM_NAME = "Ingeniería de Sistemas y Computación";
  private static final String EXPECTED_PLAN_NAME = "Plan de Estudios 2020";
  private static final String EXPECTED_JOURNEY_NAME = "Jornada Diurna";
  private static final String JOURNEY_CODE = "DIURNA";

  @BeforeEach
  void setUp() {
    loadSubscriptionPort = mock(LoadSubscriptionPort.class);
    getProgramQuery = mock(GetProgramQuery.class);
    getStudyPlanQuery = mock(GetStudyPlanQuery.class);
    listJourneysQuery = mock(ListJourneysQuery.class);

    // Stub catalog responses
    when(getProgramQuery.getProgram(PROGRAM_ID))
        .thenReturn(
            new ProgramView(PROGRAM_ID, EXPECTED_PROGRAM_NAME, "Engineering", "ACTIVE", 0L));
    when(getStudyPlanQuery.getStudyPlan(PLAN_ID, "ADMIN"))
        .thenReturn(
            new StudyPlanView(PLAN_ID, PROGRAM_ID, EXPECTED_PLAN_NAME, "ACTIVE", 240, 2020, 0L));
    when(listJourneysQuery.listJourneys())
        .thenReturn(
            List.of(
                new JourneyRef(JOURNEY_ID, JOURNEY_CODE, EXPECTED_JOURNEY_NAME),
                new JourneyRef(UUID.randomUUID(), "NOCTURNA", "Jornada Nocturna")));

    sut =
        new GetSubscriptionService(
            loadSubscriptionPort, getProgramQuery, getStudyPlanQuery, listJourneysQuery);
  }

  // -------------------------------------------------------------------------
  // getSubscription resolves all three names
  // -------------------------------------------------------------------------

  @Test
  void get_subscription_resolves_program_name_from_catalog() {
    Subscription sub = buildSubscription();
    when(loadSubscriptionPort.loadById(SUB_ID)).thenReturn(Optional.of(sub));

    SubscriptionView view = sut.getSubscription(SUB_ID, USER_ID);

    assertThat(view.programName())
        .as("programName must be resolved from catalog, not null/N/A")
        .isEqualTo(EXPECTED_PROGRAM_NAME);
  }

  @Test
  void get_subscription_resolves_plan_name_from_catalog() {
    Subscription sub = buildSubscription();
    when(loadSubscriptionPort.loadById(SUB_ID)).thenReturn(Optional.of(sub));

    SubscriptionView view = sut.getSubscription(SUB_ID, USER_ID);

    assertThat(view.planName())
        .as("planName must be resolved from catalog, not null/N/A")
        .isEqualTo(EXPECTED_PLAN_NAME);
  }

  @Test
  void get_subscription_resolves_journey_name_from_catalog() {
    Subscription sub = buildSubscription();
    when(loadSubscriptionPort.loadById(SUB_ID)).thenReturn(Optional.of(sub));

    SubscriptionView view = sut.getSubscription(SUB_ID, USER_ID);

    assertThat(view.referenceJourneyName())
        .as("referenceJourneyName must be resolved from catalog, not null/N/A")
        .isEqualTo(EXPECTED_JOURNEY_NAME);
  }

  // -------------------------------------------------------------------------
  // listMySubscriptions resolves all three names
  // -------------------------------------------------------------------------

  @Test
  void list_subscriptions_resolves_program_name_for_all_entries() {
    Subscription sub = buildSubscription();
    when(loadSubscriptionPort.loadActiveByUserId(USER_ID)).thenReturn(List.of(sub));

    List<SubscriptionView> views = sut.listMySubscriptions(USER_ID);

    assertThat(views).hasSize(1);
    assertThat(views.get(0).programName())
        .as("programName in list must be resolved from catalog")
        .isEqualTo(EXPECTED_PROGRAM_NAME);
  }

  @Test
  void list_subscriptions_resolves_plan_name_for_all_entries() {
    Subscription sub = buildSubscription();
    when(loadSubscriptionPort.loadActiveByUserId(USER_ID)).thenReturn(List.of(sub));

    List<SubscriptionView> views = sut.listMySubscriptions(USER_ID);

    assertThat(views).hasSize(1);
    assertThat(views.get(0).planName())
        .as("planName in list must be resolved from catalog")
        .isEqualTo(EXPECTED_PLAN_NAME);
  }

  @Test
  void list_subscriptions_resolves_journey_name_for_all_entries() {
    Subscription sub = buildSubscription();
    when(loadSubscriptionPort.loadActiveByUserId(USER_ID)).thenReturn(List.of(sub));

    List<SubscriptionView> views = sut.listMySubscriptions(USER_ID);

    assertThat(views).hasSize(1);
    assertThat(views.get(0).referenceJourneyName())
        .as("referenceJourneyName in list must be resolved from catalog")
        .isEqualTo(EXPECTED_JOURNEY_NAME);
  }

  // -------------------------------------------------------------------------
  // Graceful fallback when catalog lookup fails
  // -------------------------------------------------------------------------

  @Test
  void get_subscription_falls_back_to_id_string_when_program_lookup_fails() {
    when(getProgramQuery.getProgram(PROGRAM_ID))
        .thenThrow(new RuntimeException("catalog unavailable"));
    Subscription sub = buildSubscription();
    when(loadSubscriptionPort.loadById(SUB_ID)).thenReturn(Optional.of(sub));

    SubscriptionView view = sut.getSubscription(SUB_ID, USER_ID);

    assertThat(view.programName())
        .as("programName must fall back gracefully (not null, not throw)")
        .isNotNull()
        .isNotEqualTo("N/A");
  }

  @Test
  void get_subscription_falls_back_to_id_string_when_plan_lookup_fails() {
    // Stub the plan query to throw — mirrors the program and journey fallback tests.
    // NAME_RESOLUTION_ROLE ("ADMIN") is used so archived plans still resolve; when the port
    // throws we expect planName to fall back to PLAN_ID.toString() rather than propagating.
    when(getStudyPlanQuery.getStudyPlan(PLAN_ID, "ADMIN"))
        .thenThrow(new RuntimeException("catalog unavailable"));
    Subscription sub = buildSubscription();
    when(loadSubscriptionPort.loadById(SUB_ID)).thenReturn(Optional.of(sub));

    SubscriptionView view = sut.getSubscription(SUB_ID, USER_ID);

    assertThat(view.planName())
        .as("planName must fall back to PLAN_ID string when plan lookup throws")
        .isNotNull()
        .isEqualTo(PLAN_ID.toString());
  }

  @Test
  void get_subscription_falls_back_to_code_when_journey_lookup_fails() {
    when(listJourneysQuery.listJourneys()).thenThrow(new RuntimeException("catalog unavailable"));
    Subscription sub = buildSubscription();
    when(loadSubscriptionPort.loadById(SUB_ID)).thenReturn(Optional.of(sub));

    SubscriptionView view = sut.getSubscription(SUB_ID, USER_ID);

    assertThat(view.referenceJourneyName())
        .as("referenceJourneyName must fall back to journey code when catalog fails")
        .isNotNull()
        .isEqualTo(JOURNEY_CODE);
  }

  // -------------------------------------------------------------------------
  // Helpers
  // -------------------------------------------------------------------------

  private Subscription buildSubscription() {
    return Subscription.reconstitute(
        new SubscriptionId(SUB_ID),
        USER_ID,
        PROGRAM_ID,
        PLAN_ID,
        JOURNEY_CODE,
        SubscriptionStatus.ACTIVE,
        Instant.now(),
        0L);
  }
}
