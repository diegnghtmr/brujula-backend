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
}
