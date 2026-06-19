package co.edu.uniquindio.brujula.progress.application.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Application-layer DTO for a per-period estimated average.
 *
 * <p>This DTO is in the application package so REST adapters can reference it without importing
 * domain types. Maps from {@link co.edu.uniquindio.brujula.progress.domain.PeriodAverageData}.
 *
 * @param periodId period UUID
 * @param periodName human-readable period name (e.g. "2023-1"); nullable when not resolvable
 * @param estimatedAverage mean grade for graded attempts in this period
 */
public record PeriodAverageView(UUID periodId, String periodName, BigDecimal estimatedAverage) {}
