package co.edu.uniquindio.brujula.offering.domain;

/**
 * Thrown when attempting to publish an offering to PRELIMINARY or FINAL without any schedule options.
 *
 * <p>Maps to HTTP 422 Unprocessable Entity. Satisfies: OFFW-005, OFFW-008.
 */
public class OfferingNotPublishableException extends RuntimeException {

  private final CourseOfferingId offeringId;

  public OfferingNotPublishableException(CourseOfferingId offeringId) {
    super(String.format(
        "Offering %s cannot be published: at least one schedule option is required",
        offeringId.value()));
    this.offeringId = offeringId;
  }

  public CourseOfferingId getOfferingId() {
    return offeringId;
  }
}
