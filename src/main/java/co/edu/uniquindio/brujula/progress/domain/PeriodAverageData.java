package co.edu.uniquindio.brujula.progress.domain;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/**
 * Average grade per academic period, computed by {@link ProgressSummaryCalculator}.
 *
 * <p>No Spring/JPA imports.
 *
 * @param periodId the opaque period UUID
 * @param estimatedAverage the arithmetic mean of PASSED/FAILED grades in this period
 */
public record PeriodAverageData(UUID periodId, BigDecimal estimatedAverage) {

  public PeriodAverageData {
    Objects.requireNonNull(periodId, "periodId must not be null");
    Objects.requireNonNull(estimatedAverage, "estimatedAverage must not be null");
  }
}
