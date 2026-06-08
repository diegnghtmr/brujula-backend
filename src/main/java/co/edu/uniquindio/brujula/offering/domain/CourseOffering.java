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
