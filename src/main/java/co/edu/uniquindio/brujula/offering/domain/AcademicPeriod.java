package co.edu.uniquindio.brujula.offering.domain;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Domain entity representing an academic period (e.g., "2026-2").
 *
 * <p>Instances are reconstituted from persistence by the mapper via the package-private
 * {@link #reconstitute} factory method.
 */
public class AcademicPeriod {

  private final AcademicPeriodId id;
  private final String name;
  private final LocalDate startDate;
  private final LocalDate endDate;
  private final PeriodStatus status;
  private final long version;

  private AcademicPeriod(
      AcademicPeriodId id,
      String name,
      LocalDate startDate,
      LocalDate endDate,
      PeriodStatus status,
      long version) {
    this.id = id;
    this.name = name;
    this.startDate = startDate;
    this.endDate = endDate;
    this.status = status;
    this.version = version;
  }

  /**
   * Public factory for mapper reconstitution — mirrors progress Subscription.reconstitute().
   */
  public static AcademicPeriod reconstitute(
      AcademicPeriodId id,
      String name,
      LocalDate startDate,
      LocalDate endDate,
      PeriodStatus status,
      long version) {
    Objects.requireNonNull(id, "id must not be null");
    Objects.requireNonNull(name, "name must not be null");
    Objects.requireNonNull(status, "status must not be null");
    return new AcademicPeriod(id, name, startDate, endDate, status, version);
  }

  public AcademicPeriodId getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public LocalDate getStartDate() {
    return startDate;
  }

  public LocalDate getEndDate() {
    return endDate;
  }

  public PeriodStatus getStatus() {
    return status;
  }

  public long getVersion() {
    return version;
  }
}
