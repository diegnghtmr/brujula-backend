package co.edu.uniquindio.brujula.progress.application.dto;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Application-layer DTO: the full curriculum view for a subscription.
 *
 * <p>Returned by {@link
 * co.edu.uniquindio.brujula.progress.application.port.in.GetSubscriptionCurriculumQuery}. No domain
 * type imports (ARQ-001).
 *
 * @param subscriptionId the subscription UUID
 * @param referenceJourneyCode journey code (e.g. "DIURNA")
 * @param referenceJourneyName human-readable journey name
 * @param semesters ordered list of semester buckets (ascending by recommendedSemester)
 */
public record CurriculumViewData(
    UUID subscriptionId,
    String referenceJourneyCode,
    String referenceJourneyName,
    List<CurriculumSemesterData> semesters) {

  public CurriculumViewData {
    Objects.requireNonNull(subscriptionId, "subscriptionId must not be null");
    Objects.requireNonNull(referenceJourneyCode, "referenceJourneyCode must not be null");
    Objects.requireNonNull(semesters, "semesters must not be null");
  }
}
