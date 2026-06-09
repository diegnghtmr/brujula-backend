package co.edu.uniquindio.brujula.offering.domain;

/** Thrown when a requested {@link AcademicPeriod} does not exist. */
public class PeriodNotFoundException extends RuntimeException {

  public PeriodNotFoundException(AcademicPeriodId id) {
    super("Academic period not found: " + id.value());
  }

  public PeriodNotFoundException(String message) {
    super(message);
  }
}
