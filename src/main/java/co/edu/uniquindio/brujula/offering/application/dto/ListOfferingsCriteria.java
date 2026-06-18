package co.edu.uniquindio.brujula.offering.application.dto;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Filter and pagination criteria for listing offerings.
 *
 * @param periodId required — filter offerings by period
 * @param programId optional — filter by program
 * @param courseId optional — filter by course (reserved for future use)
 * @param status optional — filter by status string
 * @param page zero-based page number (≥ 0)
 * @param size page size (1–100)
 * @param callerRole optional — role of the caller (STAFF/ADMIN see DRAFT; STUDENT does not)
 * @param journeyCodes optional — when present, only offerings with at least one schedule option
 *     whose journey_code is in this set are returned. Null or empty means no journey filter.
 */
public record ListOfferingsCriteria(
    UUID periodId,
    UUID programId,
    UUID courseId,
    String status,
    int page,
    int size,
    String callerRole,
    List<String> journeyCodes) {

  /** Backward-compatible constructor without callerRole and journeyCodes (defaults to null). */
  public ListOfferingsCriteria(
      UUID periodId, UUID programId, UUID courseId, String status, int page, int size) {
    this(periodId, programId, courseId, status, page, size, null, null);
  }

  /** Backward-compatible constructor without journeyCodes (defaults to null). */
  public ListOfferingsCriteria(
      UUID periodId,
      UUID programId,
      UUID courseId,
      String status,
      int page,
      int size,
      String callerRole) {
    this(periodId, programId, courseId, status, page, size, callerRole, null);
  }

  public ListOfferingsCriteria {
    Objects.requireNonNull(periodId, "periodId must not be null");
    if (page < 0) {
      throw new IllegalArgumentException("page must be >= 0");
    }
    if (size < 1 || size > 100) {
      throw new IllegalArgumentException("size must be between 1 and 100");
    }
  }

  /** Returns {@code true} when the caller has privileged (STAFF/ADMIN) access. */
  public boolean isPrivilegedCaller() {
    return "STAFF".equals(callerRole) || "ADMIN".equals(callerRole);
  }

  /** Returns {@code true} when no journey filter is active. */
  public boolean isJourneyFilterDisabled() {
    return journeyCodes == null || journeyCodes.isEmpty();
  }
}
