package co.edu.uniquindio.brujula.progress.application.service;

import co.edu.uniquindio.brujula.curriculum.application.dto.PlanMallaView;
import co.edu.uniquindio.brujula.curriculum.application.port.in.AssemblePlanMallaQuery;
import co.edu.uniquindio.brujula.programcatalog.application.dto.JourneyRef;
import co.edu.uniquindio.brujula.programcatalog.application.port.in.ListJourneysQuery;
import co.edu.uniquindio.brujula.progress.application.dto.CurriculumCourseData;
import co.edu.uniquindio.brujula.progress.application.dto.CurriculumSemesterData;
import co.edu.uniquindio.brujula.progress.application.dto.CurriculumViewData;
import co.edu.uniquindio.brujula.progress.application.port.in.GetSubscriptionCurriculumQuery;
import co.edu.uniquindio.brujula.progress.application.port.out.LoadCourseAttemptPort;
import co.edu.uniquindio.brujula.progress.application.port.out.LoadSubscriptionPort;
import co.edu.uniquindio.brujula.progress.domain.AttemptStatus;
import co.edu.uniquindio.brujula.progress.domain.CourseAttempt;
import co.edu.uniquindio.brujula.progress.domain.CourseAvailability;
import co.edu.uniquindio.brujula.progress.domain.CourseAvailabilityService;
import co.edu.uniquindio.brujula.progress.domain.CourseProgressStatus;
import co.edu.uniquindio.brujula.progress.domain.CourseRequirementEdge;
import co.edu.uniquindio.brujula.progress.domain.EffectiveAttemptResolver;
import co.edu.uniquindio.brujula.progress.domain.PlanCourse;
import co.edu.uniquindio.brujula.progress.domain.Subscription;
import co.edu.uniquindio.brujula.progress.domain.SubscriptionNotFoundException;
import co.edu.uniquindio.brujula.progress.domain.UnmetRequirement;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Application service: assembles the curriculum view with per-course availability computation.
 *
 * <p>Implements AVL-1. Bulk-loads the plan malla via the curriculum published port (ONE call),
 * bulk-loads attempts (ONE call), resolves effective statuses via {@link EffectiveAttemptResolver},
 * computes availability via {@link CourseAvailabilityService}, then maps to application DTOs. Zero
 * N+1 queries. BOLA-guarded.
 *
 * <p>Resolves the human-readable journey name via {@link ListJourneysQuery} so that {@link
 * CurriculumViewData#referenceJourneyName()} contains "Jornada Diurna"/"Jornada Nocturna" instead
 * of the raw code ("DIURNA"/"NOCTURNA").
 *
 * <p>No Spring/framework imports — wired via {@link
 * co.edu.uniquindio.brujula.progress.infrastructure.configuration.ProgressConfiguration}.
 */
public class GetSubscriptionCurriculumService implements GetSubscriptionCurriculumQuery {

  private final LoadSubscriptionPort loadSubscriptionPort;
  private final LoadCourseAttemptPort loadCourseAttemptPort;
  private final AssemblePlanMallaQuery assemblePlanMallaQuery;
  private final ListJourneysQuery listJourneysQuery;
  private final EffectiveAttemptResolver effectiveAttemptResolver;
  private final CourseAvailabilityService courseAvailabilityService;

  public GetSubscriptionCurriculumService(
      LoadSubscriptionPort loadSubscriptionPort,
      LoadCourseAttemptPort loadCourseAttemptPort,
      AssemblePlanMallaQuery assemblePlanMallaQuery,
      ListJourneysQuery listJourneysQuery) {
    this.loadSubscriptionPort = loadSubscriptionPort;
    this.loadCourseAttemptPort = loadCourseAttemptPort;
    this.assemblePlanMallaQuery = assemblePlanMallaQuery;
    this.listJourneysQuery = listJourneysQuery;
    this.effectiveAttemptResolver = new EffectiveAttemptResolver();
    this.courseAvailabilityService = new CourseAvailabilityService();
  }

  @Override
  public CurriculumViewData getSubscriptionCurriculum(UUID subscriptionId, UUID userId) {
    // Ownership guard (BOLA)
    Subscription subscription =
        loadSubscriptionPort
            .loadById(subscriptionId)
            .filter(s -> s.getUserId().equals(userId))
            .orElseThrow(() -> new SubscriptionNotFoundException(subscriptionId));

    // --- Bulk-load 1: Plan malla from curriculum (ONE call, no N+1) ---
    PlanMallaView malla =
        assemblePlanMallaQuery.assemblePlanMalla(
            subscription.getPlanId(), subscription.getReferenceJourneyCode());

    // --- Bulk-load 2: Attempts across ALL the student's active subscriptions for this program ---
    // (shared academic history: a grade belongs to the program, not the journey)
    List<CourseAttempt> attempts =
        loadCourseAttemptPort.loadByUserIdAndProgramId(
            subscription.getUserId(), subscription.getProgramId());

    // Build attempt groups by courseId for effective-status resolution
    Map<UUID, List<CourseAttempt>> attemptsByCourse = new HashMap<>();
    for (CourseAttempt attempt : attempts) {
      attemptsByCourse.computeIfAbsent(attempt.getCourseId(), k -> new ArrayList<>()).add(attempt);
    }

    // Extract plan course IDs for off-plan detection
    Map<UUID, PlanMallaView.CourseSummary> planCourseMap = new HashMap<>();
    List<PlanCourse> planCourses = new ArrayList<>();
    List<CourseRequirementEdge> requirementEdges = new ArrayList<>();

    for (PlanMallaView.SemesterBucket bucket : malla.semesterBuckets()) {
      for (PlanMallaView.CourseSummary cs : bucket.courses()) {
        planCourseMap.put(cs.id(), cs);
        planCourses.add(
            new PlanCourse(
                cs.id(), cs.name(), cs.credits(), cs.type(), bucket.recommendedSemester()));

        // Collect requirement edges
        if (cs.requirements() != null) {
          for (PlanMallaView.RequirementSummary req : cs.requirements()) {
            requirementEdges.add(
                new CourseRequirementEdge(
                    cs.id(),
                    req.requiredCourseId(),
                    req.type(),
                    req.confidence(),
                    req.inferred(),
                    req.requiredCredits()));
          }
        }
      }
    }

    // Pool membership (PROP-001 design D1/§6): ACTIVE DEEPENING electives with no semester row,
    // supplied by the curriculum module. No hardcoded course ids — purely structural.
    Set<UUID> poolMemberCourseIds =
        malla.poolCourses().stream()
            .map(PlanMallaView.CourseSummary::id)
            .collect(Collectors.toSet());
    Map<UUID, PlanMallaView.CourseSummary> poolCourseById =
        malla.poolCourses().stream()
            .collect(Collectors.toMap(PlanMallaView.CourseSummary::id, cs -> cs));

    // Resolve effective declared status per course
    Map<UUID, CourseProgressStatus> effectiveByCourse = new HashMap<>();
    for (Map.Entry<UUID, List<CourseAttempt>> entry : attemptsByCourse.entrySet()) {
      Optional<AttemptStatus> effective = effectiveAttemptResolver.resolve(entry.getValue());
      effective.ifPresent(
          status -> {
            CourseProgressStatus progressStatus =
                switch (status) {
                  case PASSED -> CourseProgressStatus.PASSED;
                  case FAILED -> CourseProgressStatus.FAILED;
                  case CANCELLED -> CourseProgressStatus.CANCELLED;
                };
            effectiveByCourse.put(entry.getKey(), progressStatus);
          });
    }

    // Compute availability per course (pure domain service) — including the CONFIGURABLE
    // pool-gate (e.g. the profundización thesis gate: distinct passed pool members >= threshold)
    Map<UUID, CourseAvailability> availabilityMap =
        courseAvailabilityService.compute(
            planCourses, requirementEdges, effectiveByCourse, poolMemberCourseIds);

    // Numbered display-slot mapping (PROP-001 design D5, REQ-6): the k-th DISTINCT passed pool
    // elective (ordered by periodId, then registeredAt of its earliest PASSED attempt) fills the
    // k-th numbered display slot — a DEEPENING course that IS rendered in the malla (i.e. has a
    // semester row), ordered by (recommendedSemester, officialCode). No hardcoded course ids.
    Map<UUID, String> filledElectiveNameBySlotCourseId =
        buildFilledElectiveNameBySlot(malla, attempts, poolMemberCourseIds, poolCourseById);

    // Build response semester buckets
    List<CurriculumSemesterData> semesters = new ArrayList<>();
    for (PlanMallaView.SemesterBucket bucket : malla.semesterBuckets()) {
      List<CurriculumCourseData> courseDtos = new ArrayList<>();
      for (PlanMallaView.CourseSummary cs : bucket.courses()) {
        CourseAvailability avail = availabilityMap.get(cs.id());
        String status =
            avail != null ? avail.status().name() : CourseProgressStatus.AVAILABLE.name();
        List<String> unmet =
            avail != null
                ? avail.unmetRequirements().stream()
                    .map(u -> describeUnmet(u, planCourseMap))
                    .toList()
                : List.of();

        String filledElectiveName = filledElectiveNameBySlotCourseId.get(cs.id());
        if (filledElectiveName != null) {
          // The slot's own status defers to the filled elective's PASSED outcome
          status = CourseProgressStatus.PASSED.name();
        }

        courseDtos.add(
            new CurriculumCourseData(
                cs.id(),
                cs.officialCode(),
                cs.name(),
                cs.credits(),
                cs.type(),
                status,
                unmet,
                false, // plan courses are never off-plan
                filledElectiveName));
      }
      semesters.add(new CurriculumSemesterData(bucket.recommendedSemester(), courseDtos));
    }

    // Resolve human-readable journey name from programcatalog
    String journeyCode = subscription.getReferenceJourneyCode();
    String journeyName = resolveJourneyName(journeyCode);

    return new CurriculumViewData(subscriptionId, journeyCode, journeyName, semesters);
  }

  // -------------------------------------------------------------------------
  // Numbered display-slot mapping (PROP-001 design D5, REQ-6)
  // -------------------------------------------------------------------------

  /**
   * Maps the k-th distinct passed pool elective onto the k-th numbered display slot.
   *
   * <p>Ordering of passed pool electives: by the (periodId, registeredAt) of each course's earliest
   * PASSED attempt — ties broken by registeredAt. Ordering of display slots: DEEPENING courses that
   * ARE rendered in the malla (i.e. have a semester row), by (recommendedSemester, officialCode) —
   * matching the natural "Profundización I/II/III/IV" reading order without hardcoding any specific
   * course id.
   *
   * @return map of slot courseId → filled elective name; only contains entries for slots that have
   *     a mapped elective (may be smaller than the slot count, or empty)
   */
  private Map<UUID, String> buildFilledElectiveNameBySlot(
      PlanMallaView malla,
      List<CourseAttempt> attempts,
      Set<UUID> poolMemberCourseIds,
      Map<UUID, PlanMallaView.CourseSummary> poolCourseById) {

    if (poolMemberCourseIds.isEmpty()) {
      return Map.of();
    }

    // Earliest PASSED attempt per pool-member course (for ordering)
    Map<UUID, CourseAttempt> earliestPassedAttemptByCourse = new HashMap<>();
    Comparator<CourseAttempt> byPeriodThenRegisteredAt =
        Comparator.comparing(CourseAttempt::getPeriodId)
            .thenComparing(CourseAttempt::getRegisteredAt);
    for (CourseAttempt attempt : attempts) {
      if (attempt.getStatus() != AttemptStatus.PASSED) {
        continue;
      }
      if (!poolMemberCourseIds.contains(attempt.getCourseId())) {
        continue;
      }
      earliestPassedAttemptByCourse.merge(
          attempt.getCourseId(),
          attempt,
          (existing, candidate) ->
              byPeriodThenRegisteredAt.compare(candidate, existing) < 0 ? candidate : existing);
    }

    List<UUID> orderedPassedPoolElectives =
        earliestPassedAttemptByCourse.values().stream()
            .sorted(byPeriodThenRegisteredAt)
            .map(CourseAttempt::getCourseId)
            .toList();

    if (orderedPassedPoolElectives.isEmpty()) {
      return Map.of();
    }

    // Numbered display slots: DEEPENING courses rendered in the malla, ordered by
    // (recommendedSemester, officialCode)
    record SlotCandidate(int recommendedSemester, PlanMallaView.CourseSummary course) {}
    List<PlanMallaView.CourseSummary> slotAnchors =
        malla.semesterBuckets().stream()
            .flatMap(
                bucket ->
                    bucket.courses().stream()
                        .map(cs -> new SlotCandidate(bucket.recommendedSemester(), cs)))
            .filter(sc -> "DEEPENING".equals(sc.course().type()))
            .sorted(
                Comparator.comparingInt(SlotCandidate::recommendedSemester)
                    .thenComparing(
                        sc -> sc.course().officialCode() != null ? sc.course().officialCode() : ""))
            .map(SlotCandidate::course)
            .toList();

    Map<UUID, String> filledElectiveNameBySlotCourseId = new HashMap<>();
    int slotCount = Math.min(orderedPassedPoolElectives.size(), slotAnchors.size());
    for (int i = 0; i < slotCount; i++) {
      UUID electiveId = orderedPassedPoolElectives.get(i);
      PlanMallaView.CourseSummary elective = poolCourseById.get(electiveId);
      if (elective != null) {
        filledElectiveNameBySlotCourseId.put(slotAnchors.get(i).id(), elective.name());
      }
    }
    return filledElectiveNameBySlotCourseId;
  }

  // -------------------------------------------------------------------------
  // Resolve human-readable journey name from programcatalog
  // -------------------------------------------------------------------------

  /**
   * Looks up the human-readable journey name for the given journey code. Falls back to the code
   * itself if the catalog lookup fails or the code is not found (defensive: avoids a hard 500 for a
   * non-critical display field).
   */
  private String resolveJourneyName(String journeyCode) {
    try {
      List<JourneyRef> journeys = listJourneysQuery.listJourneys();
      return journeys.stream()
          .filter(j -> journeyCode.equalsIgnoreCase(j.code()))
          .map(JourneyRef::name)
          .findFirst()
          .orElse(journeyCode); // fallback to code if not found
    } catch (Exception e) {
      return journeyCode; // defensive fallback
    }
  }

  private String describeUnmet(
      UnmetRequirement u, Map<UUID, PlanMallaView.CourseSummary> planCourseMap) {
    String prefix = u.advisory() ? "[Recomendado] " : "";
    if (u.requiredCourseId() == null) {
      // Pool-gate requirement (PROP-001 design D1, e.g. CONFIGURABLE thesis gate) — no single
      // required course; describe the gate generically instead of a specific prerequisite name.
      return prefix + "Cumplir con el requisito de electivas de profundización";
    }
    PlanMallaView.CourseSummary cs = planCourseMap.get(u.requiredCourseId());
    String courseName = cs != null ? cs.name() : u.requiredCourseId().toString();
    return prefix + courseName;
  }
}
