package co.edu.uniquindio.brujula.offering.domain;

/**
 * Thrown when an offering state-machine transition is not permitted by the legal-transition table.
 *
 * <p>Maps to HTTP 409 Conflict. Satisfies: OFFW-005, OFFW-008.
 */
public class IllegalOfferingTransitionException extends RuntimeException {

  private final CourseOfferingId offeringId;
  private final OfferingStatus from;
  private final OfferingStatus to;

  public IllegalOfferingTransitionException(
      CourseOfferingId offeringId, OfferingStatus from, OfferingStatus to) {
    super(
        String.format(
            "Illegal offering transition: %s → %s for offering %s", from, to, offeringId.value()));
    this.offeringId = offeringId;
    this.from = from;
    this.to = to;
  }

  public CourseOfferingId getOfferingId() {
    return offeringId;
  }

  public OfferingStatus getFrom() {
    return from;
  }

  public OfferingStatus getTo() {
    return to;
  }
}
