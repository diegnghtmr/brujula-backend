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
 * <p>Framework-free. Satisfies: SUB-2, CROSS-2.
 *
 * <p>WU3-FIX: resolves programName, planName, and referenceJourneyName from programcatalog
 * published ports (mirrors the FIX-W2 pattern in {@link GetSubscriptionProgressService}). The
 * programcatalog ports are injected directly following the precedent established in {@code
 * GetSubscriptionProgressService} and {@code GetSubscriptionCurriculumService}.
 */
public class GetSubscriptionService implements ListMySubscriptionsQuery, GetSubscriptionQuery {

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
    return loadSubscriptionPort.loadActiveByUserId(userId).stream().map(this::toView).toList();
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

    return toView(sub);
  }

  private SubscriptionView toView(Subscription sub) {
    String programName = resolveProgramName(sub.getProgramId());
    String planName = resolvePlanName(sub.getPlanId());
    String journeyName = resolveJourneyName(sub.getReferenceJourneyCode());

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
  // WU3-FIX: catalog name resolvers (mirrors FIX-W2 in GetSubscriptionProgressService)
  // -------------------------------------------------------------------------

  /**
   * Resolves the human-readable program name from the programcatalog published port. Falls back to
   * the programId string representation if the program cannot be found (defensive: avoids a hard
   * 500 for a non-critical display field).
   */
  private String resolveProgramName(UUID programId) {
    try {
      return getProgramQuery.getProgram(programId).name();
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
      return getStudyPlanQuery.getStudyPlan(planId, "ADMIN").name();
    } catch (Exception e) {
      return planId.toString();
    }
  }

  /**
   * Resolves the human-readable journey name for the given journey code. Falls back to the code
   * itself if the catalog lookup fails or the code is not found (mirrors FIX-S1 in
   * GetSubscriptionCurriculumService).
   */
  private String resolveJourneyName(String journeyCode) {
    try {
      List<JourneyRef> journeys = listJourneysQuery.listJourneys();
      return journeys.stream()
          .filter(j -> journeyCode.equalsIgnoreCase(j.code()))
          .map(JourneyRef::name)
          .findFirst()
          .orElse(journeyCode);
    } catch (Exception e) {
      return journeyCode;
    }
  }
}
