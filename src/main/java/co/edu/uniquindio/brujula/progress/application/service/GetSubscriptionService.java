package co.edu.uniquindio.brujula.progress.application.service;

import co.edu.uniquindio.brujula.programcatalog.application.dto.JourneyRef;
import co.edu.uniquindio.brujula.programcatalog.application.port.in.GetProgramQuery;
import co.edu.uniquindio.brujula.programcatalog.application.port.in.GetStudyPlanQuery;
import co.edu.uniquindio.brujula.programcatalog.application.port.in.ListJourneysQuery;
import co.edu.uniquindio.brujula.progress.application.dto.SubscriptionView;
import co.edu.uniquindio.brujula.progress.application.port.in.GetSubscriptionQuery;
import co.edu.uniquindio.brujula.progress.application.port.in.ListMySubscriptionsQuery;
import co.edu.uniquindio.brujula.progress.application.port.out.LoadSubscriptionPort;
import co.edu.uniquindio.brujula.progress.domain.Subscription;
import co.edu.uniquindio.brujula.progress.domain.SubscriptionNotFoundException;
import java.util.List;
import java.util.UUID;

/**
 * Use case: list and get subscriptions with BOLA ownership guard.
 *
 * <p>Framework-free. Resolves programName, planName, and referenceJourneyName from programcatalog
 * published ports. The programcatalog ports are injected directly following the precedent
 * established in {@code GetSubscriptionProgressService} and {@code
 * GetSubscriptionCurriculumService}.
 */
public class GetSubscriptionService implements ListMySubscriptionsQuery, GetSubscriptionQuery {

  /**
   * Role used when calling {@link GetStudyPlanQuery#getStudyPlan} for display-name resolution only.
   *
   * <p>ADMIN scope is intentional: a STUDENT role would throw on non-ACTIVE plans, but we still
   * need to display the human-readable plan name even when the plan was archived after the student
   * subscribed. This is a read-only, display-only lookup — it does not grant any write privilege.
   */
  private static final String NAME_RESOLUTION_ROLE = "ADMIN";

  private final LoadSubscriptionPort loadSubscriptionPort;
  private final GetProgramQuery getProgramQuery;
  private final GetStudyPlanQuery getStudyPlanQuery;
  private final ListJourneysQuery listJourneysQuery;

  public GetSubscriptionService(
      LoadSubscriptionPort loadSubscriptionPort,
      GetProgramQuery getProgramQuery,
      GetStudyPlanQuery getStudyPlanQuery,
      ListJourneysQuery listJourneysQuery) {
    this.loadSubscriptionPort = loadSubscriptionPort;
    this.getProgramQuery = getProgramQuery;
    this.getStudyPlanQuery = getStudyPlanQuery;
    this.listJourneysQuery = listJourneysQuery;
  }

  @Override
  public List<SubscriptionView> listMySubscriptions(UUID userId) {
    // N+1 avoidance: fetch the journey list ONCE for the entire result set, then pass it into
    // each toView call so the list path makes a single listJourneys() call regardless of how
    // many subscriptions are returned.
    List<JourneyRef> allJourneys = fetchJourneysSafely();
    return loadSubscriptionPort.loadActiveByUserId(userId).stream()
        .map(sub -> toView(sub, allJourneys))
        .toList();
  }

  @Override
  public SubscriptionView getSubscription(UUID subscriptionId, UUID userId) {
    Subscription sub =
        loadSubscriptionPort
            .loadById(subscriptionId)
            .orElseThrow(() -> new SubscriptionNotFoundException(subscriptionId));

    // BOLA guard: 404 if owned by different user (ownership not revealed)
    if (!sub.getUserId().equals(userId)) {
      throw new SubscriptionNotFoundException(subscriptionId);
    }

    // Single-GET path: fetch journeys once for this single subscription.
    List<JourneyRef> allJourneys = fetchJourneysSafely();
    return toView(sub, allJourneys);
  }

  /**
   * Builds a {@link SubscriptionView} using a pre-fetched journey list so callers control when
   * {@link ListJourneysQuery#listJourneys()} is invoked (avoids per-row refetch in the list path).
   */
  private SubscriptionView toView(Subscription sub, List<JourneyRef> allJourneys) {
    String programName = resolveProgramName(sub.getProgramId());
    String planName = resolvePlanName(sub.getPlanId());
    String journeyName = resolveJourneyName(sub.getReferenceJourneyCode(), allJourneys);

    return new SubscriptionView(
        sub.getId().value(),
        sub.getProgramId(),
        programName,
        sub.getPlanId(),
        planName,
        sub.getReferenceJourneyCode(),
        journeyName,
        sub.getVersion(),
        sub.getCreatedAt());
  }

  // -------------------------------------------------------------------------
  // Catalog name resolvers
  // -------------------------------------------------------------------------

  /**
   * Resolves the human-readable program name from the programcatalog published port. Falls back to
   * the programId string representation if the program cannot be found (defensive: avoids a hard
   * 500 for a non-critical display field).
   */
  private String resolveProgramName(UUID programId) {
    try {
      var result = getProgramQuery.getProgram(programId);
      if (result == null) {
        return programId.toString();
      }
      return result.name();
    } catch (Exception e) {
      return programId.toString();
    }
  }

  /**
   * Resolves the human-readable plan name from the programcatalog published port. Falls back to the
   * planId string representation if the plan cannot be found.
   */
  private String resolvePlanName(UUID planId) {
    try {
      var result = getStudyPlanQuery.getStudyPlan(planId, NAME_RESOLUTION_ROLE);
      if (result == null) {
        return planId.toString();
      }
      return result.name();
    } catch (Exception e) {
      return planId.toString();
    }
  }

  /**
   * Fetches the full journey list from the catalog. Returns an empty list (not null) on failure so
   * callers always receive a safe-to-iterate list and the per-subscription fallback logic applies.
   */
  private List<JourneyRef> fetchJourneysSafely() {
    try {
      List<JourneyRef> result = listJourneysQuery.listJourneys();
      return result != null ? result : List.of();
    } catch (Exception e) {
      return List.of();
    }
  }

  /**
   * Resolves the human-readable journey name for the given journey code from a pre-fetched list.
   * Falls back to the code itself if the list is empty or the code is not found (mirrors
   * GetSubscriptionCurriculumService).
   */
  private String resolveJourneyName(String journeyCode, List<JourneyRef> allJourneys) {
    return allJourneys.stream()
        .filter(j -> journeyCode.equalsIgnoreCase(j.code()))
        .map(JourneyRef::name)
        .findFirst()
        .orElse(journeyCode);
  }
}
