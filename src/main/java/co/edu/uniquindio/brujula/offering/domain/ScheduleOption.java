package co.edu.uniquindio.brujula.offering.domain;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Domain entity representing a schedule option for a course offering.
 *
 * <p>Contains one or more {@link ScheduleSession} entries. Weekly hours are computed
 * by {@link WeeklyHoursCalculator} — never stored.
 */
public class ScheduleOption {

  private final ScheduleOptionId id;
  private final UUID offeringId;
  private final String label;
  private final String journeyCode;
  private final List<ScheduleSession> sessions;
  private final long version;

  private ScheduleOption(
      ScheduleOptionId id,
      UUID offeringId,
      String label,
      String journeyCode,
      List<ScheduleSession> sessions,
      long version) {
    this.id = id;
    this.offeringId = offeringId;
    this.label = label;
    this.journeyCode = journeyCode;
    this.sessions = Collections.unmodifiableList(sessions);
    this.version = version;
  }

  /** Public factory for mapper reconstitution — mirrors progress Subscription.reconstitute(). */
  public static ScheduleOption reconstitute(
      ScheduleOptionId id,
      UUID offeringId,
      String label,
      String journeyCode,
      List<ScheduleSession> sessions,
      long version) {
    Objects.requireNonNull(id, "id must not be null");
    Objects.requireNonNull(offeringId, "offeringId must not be null");
    Objects.requireNonNull(label, "label must not be null");
    Objects.requireNonNull(journeyCode, "journeyCode must not be null");
    Objects.requireNonNull(sessions, "sessions must not be null");
    return new ScheduleOption(id, offeringId, label, journeyCode, sessions, version);
  }

  /** Computed weekly hours (sum of session durations). */
  public double weeklyHours() {
    return WeeklyHoursCalculator.calculate(sessions);
  }

  public ScheduleOptionId getId() {
    return id;
  }

  public UUID getOfferingId() {
    return offeringId;
  }

  public String getLabel() {
    return label;
  }

  public String getJourneyCode() {
    return journeyCode;
  }

  public List<ScheduleSession> getSessions() {
    return sessions;
  }

  public long getVersion() {
    return version;
  }
}
