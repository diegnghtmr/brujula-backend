package co.edu.uniquindio.brujula.offering.domain;

/** Course offering lifecycle status. Maps to contract enum OfferingStatus. */
public enum OfferingStatus {
  DRAFT,
  PRELIMINARY,
  FINAL,
  ARCHIVED;

  /** Returns true when students and the simulator may view/select this offering. */
  public boolean isPublished() {
    return this == PRELIMINARY || this == FINAL;
  }

  /** Returns true when the offering has been archived. */
  public boolean isArchived() {
    return this == ARCHIVED;
  }

  /**
   * Returns {@code true} if a transition from this status to {@code target} is legal.
   *
   * <p>Legal transitions per OFFW-005:
   *
   * <ul>
   *   <li>DRAFT → PRELIMINARY ✓
   *   <li>DRAFT → FINAL ✓
   *   <li>DRAFT → ARCHIVED ✓
   *   <li>PRELIMINARY → FINAL ✓
   *   <li>PRELIMINARY → ARCHIVED ✓
   *   <li>FINAL → ARCHIVED ✓
   *   <li>ARCHIVED → * ✗ (terminal)
   *   <li>same-state → * ✗
   * </ul>
   */
  public boolean canTransitionTo(OfferingStatus target) {
    if (this == target) {
      return false;
    }
    return switch (this) {
      case DRAFT -> target == PRELIMINARY || target == FINAL || target == ARCHIVED;
      case PRELIMINARY -> target == FINAL || target == ARCHIVED;
      case FINAL -> target == ARCHIVED;
      case ARCHIVED -> false;
    };
  }
}
