package co.edu.uniquindio.brujula.offering.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

/**
 * TDD unit tests for {@link CourseOffering} write factory and mutation methods.
 *
 * <p>Covers: create() starts DRAFT/version=0; updateNotes() replaces notes, all other fields
 * identical; updateNotes returns new instance (immutability check). Zero Spring — pure domain.
 */
class CourseOfferingWriteTest {

  private static final UUID COURSE_ID = UUID.randomUUID();
  private static final UUID PROGRAM_ID = UUID.randomUUID();
  private static final UUID PERIOD_ID = UUID.randomUUID();

  @Test
  void create_starts_with_draft_status() {
    CourseOffering offering = CourseOffering.create(
        CourseOfferingId.generate(),
        COURSE_ID,
        "Programación I",
        PROGRAM_ID,
        PERIOD_ID,
        "Initial notes",
        Collections.emptyList());

    assertThat(offering.getStatus()).isEqualTo(OfferingStatus.DRAFT);
  }

  @Test
  void create_starts_with_version_zero() {
    CourseOffering offering = CourseOffering.create(
        CourseOfferingId.generate(),
        COURSE_ID,
        "Programación I",
        PROGRAM_ID,
        PERIOD_ID,
        null,
        Collections.emptyList());

    assertThat(offering.getVersion()).isZero();
  }

  @Test
  void create_preserves_all_fields() {
    CourseOfferingId id = CourseOfferingId.generate();
    String courseName = "Cálculo Diferencial";
    String notes = "Notes here";

    CourseOffering offering = CourseOffering.create(
        id, COURSE_ID, courseName, PROGRAM_ID, PERIOD_ID, notes, Collections.emptyList());

    assertThat(offering.getId()).isEqualTo(id);
    assertThat(offering.getCourseId()).isEqualTo(COURSE_ID);
    assertThat(offering.getCourseName()).isEqualTo(courseName);
    assertThat(offering.getProgramId()).isEqualTo(PROGRAM_ID);
    assertThat(offering.getPeriodId()).isEqualTo(PERIOD_ID);
    assertThat(offering.getNotes()).isEqualTo(notes);
    assertThat(offering.getScheduleOptions()).isEmpty();
  }

  @Test
  void create_with_null_id_throws_npe() {
    assertThatNullPointerException().isThrownBy(() ->
        CourseOffering.create(null, COURSE_ID, "Course", PROGRAM_ID, PERIOD_ID, null, Collections.emptyList()));
  }

  @Test
  void create_with_null_courseId_throws_npe() {
    assertThatNullPointerException().isThrownBy(() ->
        CourseOffering.create(CourseOfferingId.generate(), null, "Course", PROGRAM_ID, PERIOD_ID, null, Collections.emptyList()));
  }

  @Test
  void updateNotes_returns_new_instance() {
    CourseOffering original = CourseOffering.create(
        CourseOfferingId.generate(),
        COURSE_ID, "Course", PROGRAM_ID, PERIOD_ID, "Old notes", Collections.emptyList());

    CourseOffering updated = original.updateNotes("New notes");

    assertThat(updated).isNotSameAs(original);
  }

  @Test
  void updateNotes_replaces_notes_field() {
    CourseOffering original = CourseOffering.create(
        CourseOfferingId.generate(),
        COURSE_ID, "Course", PROGRAM_ID, PERIOD_ID, "Old notes", Collections.emptyList());

    CourseOffering updated = original.updateNotes("New notes");

    assertThat(updated.getNotes()).isEqualTo("New notes");
    assertThat(original.getNotes()).isEqualTo("Old notes"); // original unchanged
  }

  @Test
  void updateNotes_preserves_all_other_fields() {
    CourseOfferingId id = CourseOfferingId.generate();
    CourseOffering original = CourseOffering.create(
        id, COURSE_ID, "Course", PROGRAM_ID, PERIOD_ID, "Old notes", Collections.emptyList());

    CourseOffering updated = original.updateNotes("New notes");

    assertThat(updated.getId()).isEqualTo(original.getId());
    assertThat(updated.getCourseId()).isEqualTo(original.getCourseId());
    assertThat(updated.getCourseName()).isEqualTo(original.getCourseName());
    assertThat(updated.getProgramId()).isEqualTo(original.getProgramId());
    assertThat(updated.getPeriodId()).isEqualTo(original.getPeriodId());
    assertThat(updated.getStatus()).isEqualTo(original.getStatus());
    assertThat(updated.getVersion()).isEqualTo(original.getVersion());
  }

  @Test
  void create_with_schedule_options_preserves_them() {
    ScheduleOptionId optionId = ScheduleOptionId.generate();
    CourseOfferingId offeringId = CourseOfferingId.generate();
    ScheduleOption option = ScheduleOption.reconstitute(
        optionId, offeringId.value(), "Group A", "DIURNA", Collections.emptyList(), 0L);

    CourseOffering offering = CourseOffering.create(
        offeringId, COURSE_ID, "Course", PROGRAM_ID, PERIOD_ID, null, List.of(option));

    assertThat(offering.getScheduleOptions()).hasSize(1);
    assertThat(offering.getScheduleOptions().get(0).getId()).isEqualTo(optionId);
  }
}
