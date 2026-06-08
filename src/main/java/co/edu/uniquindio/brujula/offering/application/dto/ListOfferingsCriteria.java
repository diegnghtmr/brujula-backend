package co.edu.uniquindio.brujula.offering.application.dto;

import java.util.Objects;
import java.util.UUID;

/**
 * Filter and pagination criteria for listing offerings.
 *
 * @param periodId  required — filter offerings by period
 * @param programId optional — filter by program
 * @param courseId  optional — filter by course (reserved for future use)
 * @param status    optional — filter by status (reserved for future use)
 * @param page      zero-based page number (≥ 0)
 * @param size      page size (1–100)
 */
public record ListOfferingsCriteria(
    UUID periodId,
    UUID programId,
    UUID courseId,
    String status,
    int page,
    int size) {

  public ListOfferingsCriteria {
    Objects.requireNonNull(periodId, "periodId must not be null");
    if (page < 0) {
      throw new IllegalArgumentException("page must be >= 0");
    }
    if (size < 1 || size > 100) {
      throw new IllegalArgumentException("size must be between 1 and 100");
    }
  }
}
