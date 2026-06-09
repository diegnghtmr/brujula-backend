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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Application service: assembles the curriculum view with per-course availability computation.
 *
 * <p>Implements AVL-1. Bulk-loads the plan malla via the curriculum published port (ONE call),
 * bulk-loads attempts (ONE call), resolves effective statuses via {@link EffectiveAttemptResolver},
 * computes availability via {@link CourseAvailabilityService}, then maps to application DTOs. Zero
 * N+1 queries (ADR-F3-005). BOLA-guarded (CROSS-2).
 *
 * <p>FIX-S1: resolves the human-readable journey name via {@link ListJourneysQuery} so that {@link
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

    // --- Bulk-load 2: All attempts for this subscription (ONE call, no N+1) ---
    List<CourseAttempt> attempts = loadCourseAttemptPort.loadBySubscriptionId(subscriptionId);

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
                    cs.id(), req.requiredCourseId(), req.type(), req.confidence(), req.inferred()));
          }
        }
      }
    }

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

    // Compute availability per course (pure domain service)
    Map<UUID, CourseAvailability> availabilityMap =
        courseAvailabilityService.compute(planCourses, requirementEdges, effectiveByCourse);

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

        courseDtos.add(
            new CurriculumCourseData(
                cs.id(),
                cs.officialCode(),
                cs.name(),
                cs.credits(),
                cs.type(),
                status,
                unmet,
                false)); // plan courses are never off-plan
      }
      semesters.add(new CurriculumSemesterData(bucket.recommendedSemester(), courseDtos));
    }

    // FIX-S1: resolve human-readable journey name from programcatalog
    String journeyCode = subscription.getReferenceJourneyCode();
    String journeyName = resolveJourneyName(journeyCode);

    return new CurriculumViewData(subscriptionId, journeyCode, journeyName, semesters);
  }

  // -------------------------------------------------------------------------
  // FIX-S1: resolve human-readable journey name from programcatalog
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
    PlanMallaView.CourseSummary cs = planCourseMap.get(u.requiredCourseId());
    String courseName = cs != null ? cs.name() : u.requiredCourseId().toString();
    String prefix = u.advisory() ? "[Recomendado] " : "";
    return prefix + courseName;
  }
}
