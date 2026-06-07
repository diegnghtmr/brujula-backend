package co.edu.uniquindio.brujula.progress.application.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Application-layer DTO for a per-period estimated average.
 *
 * <p>ARQ-001: this DTO is in the application package so REST adapters can reference it without
 * importing domain types. Maps from {@link co.edu.uniquindio.brujula.progress.domain.PeriodAverageData}.
 *
 * @param periodId period UUID
 * @param estimatedAverage mean grade for graded attempts in this period
 */
public record PeriodAverageView(UUID periodId, BigDecimal estimatedAverage) {
}
