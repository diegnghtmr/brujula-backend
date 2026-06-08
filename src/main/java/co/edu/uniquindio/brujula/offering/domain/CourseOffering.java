package co.edu.uniquindio.brujula.offering.domain;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Domain entity representing a course offering within an academic period.
 *
 * <p>A CourseOffering aggregates one or more {@link ScheduleOption} instances and is
 * identified by an opaque {@link CourseOfferingId}. Instances are reconstituted from
 * persistence via the package-private {@link #reconstitute} factory.
 */
public class CourseOffering {

  private final CourseOfferingId id;
  /** Opaque reference to curriculum::Course (cross-schema, no FK). */
  private final UUID courseId;
  /** Denormalized course name snapshot — avoids runtime curriculum coupling. */
  private final String courseName;
  /** Opaque reference to programcatalog::Program (cross-schema, no FK). */
  private final UUID programId;
  /** Opaque reference to the academic period this offering belongs to. */
  private final UUID periodId;
  private final OfferingStatus status;
  private final String notes;
  private final List<ScheduleOption> scheduleOptions;
  private final long version;

  private CourseOffering(
      CourseOfferingId id,
      UUID courseId,
      String courseName,
      UUID programId,
      UUID periodId,
      OfferingStatus status,
      String notes,
      List<ScheduleOption> scheduleOptions,
      long version) {
    this.id = id;
    this.courseId = courseId;
    this.courseName = courseName;
    this.programId = programId;
    this.periodId = periodId;
    this.status = status;
    this.notes = notes;
    this.scheduleOptions = Collections.unmodifiableList(scheduleOptions);
    this.version = version;
  }

  /**
   * Write factory: creates a new offering in DRAFT status at version 0.
   *
   * <p>Inline schedule options are attached atomically; null/empty options allowed
   * (publishability is enforced only at publish time, not at create time per OFFW-003).
   *
   * <p>Satisfies: OFFW-003.
   */
  public static CourseOffering create(
      CourseOfferingId id,
      UUID courseId,
      String courseName,
      UUID programId,
      UUID periodId,
      String notes,
      List<ScheduleOption> options) {
    Objects.requireNonNull(id, "id must not be null");
    Objects.requireNonNull(courseId, "courseId must not be null");
    Objects.requireNonNull(courseName, "courseName must not be null");
    Objects.requireNonNull(programId, "programId must not be null");
    Objects.requireNonNull(periodId, "periodId must not be null");
    List<ScheduleOption> safeOptions = options != null ? options : java.util.Collections.emptyList();
    return new CourseOffering(id, courseId, courseName, programId, periodId,
        OfferingStatus.DRAFT, notes, safeOptions, 0L);
  }

  /** Public factory for mapper reconstitution — mirrors progress Subscription.reconstitute(). */
  public static CourseOffering reconstitute(
      CourseOfferingId id,
      UUID courseId,
      String courseName,
      UUID programId,
      UUID periodId,
      OfferingStatus status,
      String notes,
      List<ScheduleOption> scheduleOptions,
      long version) {
    Objects.requireNonNull(id, "id must not be null");
    Objects.requireNonNull(courseId, "courseId must not be null");
    Objects.requireNonNull(courseName, "courseName must not be null");
    Objects.requireNonNull(programId, "programId must not be null");
    Objects.requireNonNull(periodId, "periodId must not be null");
    Objects.requireNonNull(status, "status must not be null");
    Objects.requireNonNull(scheduleOptions, "scheduleOptions must not be null");
    return new CourseOffering(id, courseId, courseName, programId, periodId, status, notes, scheduleOptions, version);
  }

  /**
   * Returns a new instance with the given notes (all other fields unchanged).
   *
   * <p>The returned instance carries the same version as this one; the @Version increment
   * happens at JPA flush time (staleness activation invariant, OFFW-004, OFFW-006).
   *
   * <p>Satisfies: OFFW-004.
   */
  public CourseOffering updateNotes(String notes) {
    return new CourseOffering(id, courseId, courseName, programId, periodId,
        status, notes, scheduleOptions, version);
  }

  /**
   * Transitions this offering to the given {@code target} status and returns a new instance.
   *
   * <p>Preconditions (both checked in order):
   * <ol>
   *   <li>The transition must be legal per {@link OfferingStatus#canTransitionTo}; otherwise
   *       {@link IllegalOfferingTransitionException} is thrown (→ HTTP 409).
   *   <li>When {@code target} is {@code PRELIMINARY} or {@code FINAL} the offering must have at
   *       least one schedule option; otherwise {@link OfferingNotPublishableException} is thrown
   *       (→ HTTP 422). Transitioning to {@code ARCHIVED} does NOT require any options.
   * </ol>
   *
   * <p>The returned instance carries the same version as this one; the @Version increment
   * happens at JPA flush time (staleness activation invariant, OFFW-005, OFFW-006).
   *
   * <p>Satisfies: OFFW-005.
   */
  public CourseOffering publishTo(OfferingStatus target) {
    if (!status.canTransitionTo(target)) {
      throw new IllegalOfferingTransitionException(id, status, target);
    }
    requireSchedulableForPublish(target);
    return new CourseOffering(id, courseId, courseName, programId, periodId,
        target, notes, scheduleOptions, version);
  }

  /**
   * Guards publication to PRELIMINARY or FINAL: the offering must have at least one
   * schedule option.
   *
   * @throws OfferingNotPublishableException when {@code target} is PRELIMINARY or FINAL and
   *         the offering has no schedule options.
   */
  private void requireSchedulableForPublish(OfferingStatus target) {
    if ((target == OfferingStatus.PRELIMINARY || target == OfferingStatus.FINAL)
        && scheduleOptions.isEmpty()) {
      throw new OfferingNotPublishableException(id);
    }
  }

  /** True when this offering is visible to students (PRELIMINARY or FINAL). */
  public boolean isPublished() {
    return status.isPublished();
  }

  public CourseOfferingId getId() {
    return id;
  }

  public UUID getCourseId() {
    return courseId;
  }

  public String getCourseName() {
    return courseName;
  }

  public UUID getProgramId() {
    return programId;
  }

  public UUID getPeriodId() {
    return periodId;
  }

  public OfferingStatus getStatus() {
    return status;
  }

  public String getNotes() {
    return notes;
  }

  public List<ScheduleOption> getScheduleOptions() {
    return scheduleOptions;
  }

  public long getVersion() {
    return version;
  }
}
