package co.edu.uniquindio.brujula.offering.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * TDD unit tests for {@link CourseOffering#publishTo(OfferingStatus)}.
 *
 * <p>Covers: every legal transition produces new status; every illegal transition throws {@link
 * IllegalOfferingTransitionException}; PRELIMINARY/FINAL with zero options throws {@link
 * OfferingNotPublishableException}; ARCHIVED with zero options is OK; publishTo returns new
 * instance (immutability check). Zero Spring — pure domain.
 */
class CourseOfferingPublishTest {

  private static final UUID COURSE_ID = UUID.randomUUID();
  private static final UUID PROGRAM_ID = UUID.randomUUID();
  private static final UUID PERIOD_ID = UUID.randomUUID();

  // A minimal schedule option for offers that require one
  private static ScheduleOption oneOption() {
    return ScheduleOption.reconstitute(
        ScheduleOptionId.generate(), UUID.randomUUID(), "A", "DIURNA", Collections.emptyList(), 0L);
  }

  private CourseOffering offeringWithStatus(OfferingStatus status, List<ScheduleOption> options) {
    return CourseOffering.reconstitute(
        CourseOfferingId.generate(),
        COURSE_ID,
        "Course",
        PROGRAM_ID,
        PERIOD_ID,
        status,
        null,
        options,
        0L);
  }

  // -------------------------------------------------------------------------
  // Legal transitions
  // -------------------------------------------------------------------------

  @Test
  void draft_to_preliminary_with_option_succeeds() {
    CourseOffering offering = offeringWithStatus(OfferingStatus.DRAFT, List.of(oneOption()));
    CourseOffering result = offering.publishTo(OfferingStatus.PRELIMINARY);
    assertThat(result.getStatus()).isEqualTo(OfferingStatus.PRELIMINARY);
  }

  @Test
  void draft_to_final_with_option_succeeds() {
    CourseOffering offering = offeringWithStatus(OfferingStatus.DRAFT, List.of(oneOption()));
    CourseOffering result = offering.publishTo(OfferingStatus.FINAL);
    assertThat(result.getStatus()).isEqualTo(OfferingStatus.FINAL);
  }

  @Test
  void draft_to_archived_with_no_options_succeeds() {
    CourseOffering offering = offeringWithStatus(OfferingStatus.DRAFT, Collections.emptyList());
    CourseOffering result = offering.publishTo(OfferingStatus.ARCHIVED);
    assertThat(result.getStatus()).isEqualTo(OfferingStatus.ARCHIVED);
  }

  @Test
  void preliminary_to_final_with_option_succeeds() {
    CourseOffering offering = offeringWithStatus(OfferingStatus.PRELIMINARY, List.of(oneOption()));
    CourseOffering result = offering.publishTo(OfferingStatus.FINAL);
    assertThat(result.getStatus()).isEqualTo(OfferingStatus.FINAL);
  }

  @Test
  void preliminary_to_archived_with_option_succeeds() {
    CourseOffering offering = offeringWithStatus(OfferingStatus.PRELIMINARY, List.of(oneOption()));
    CourseOffering result = offering.publishTo(OfferingStatus.ARCHIVED);
    assertThat(result.getStatus()).isEqualTo(OfferingStatus.ARCHIVED);
  }

  @Test
  void final_to_archived_with_option_succeeds() {
    CourseOffering offering = offeringWithStatus(OfferingStatus.FINAL, List.of(oneOption()));
    CourseOffering result = offering.publishTo(OfferingStatus.ARCHIVED);
    assertThat(result.getStatus()).isEqualTo(OfferingStatus.ARCHIVED);
  }

  // -------------------------------------------------------------------------
  // Illegal transitions → IllegalOfferingTransitionException (409)
  // -------------------------------------------------------------------------

  @Test
  void archived_to_preliminary_throws_illegal_transition() {
    CourseOffering offering = offeringWithStatus(OfferingStatus.ARCHIVED, List.of(oneOption()));
    assertThatThrownBy(() -> offering.publishTo(OfferingStatus.PRELIMINARY))
        .isInstanceOf(IllegalOfferingTransitionException.class);
  }

  @Test
  void archived_to_final_throws_illegal_transition() {
    CourseOffering offering = offeringWithStatus(OfferingStatus.ARCHIVED, List.of(oneOption()));
    assertThatThrownBy(() -> offering.publishTo(OfferingStatus.FINAL))
        .isInstanceOf(IllegalOfferingTransitionException.class);
  }

  @Test
  void archived_to_archived_throws_illegal_transition() {
    CourseOffering offering = offeringWithStatus(OfferingStatus.ARCHIVED, List.of(oneOption()));
    assertThatThrownBy(() -> offering.publishTo(OfferingStatus.ARCHIVED))
        .isInstanceOf(IllegalOfferingTransitionException.class);
  }

  @Test
  void final_to_draft_throws_illegal_transition() {
    CourseOffering offering = offeringWithStatus(OfferingStatus.FINAL, List.of(oneOption()));
    assertThatThrownBy(() -> offering.publishTo(OfferingStatus.DRAFT))
        .isInstanceOf(IllegalOfferingTransitionException.class);
  }

  @Test
  void preliminary_to_draft_throws_illegal_transition() {
    CourseOffering offering = offeringWithStatus(OfferingStatus.PRELIMINARY, List.of(oneOption()));
    assertThatThrownBy(() -> offering.publishTo(OfferingStatus.DRAFT))
        .isInstanceOf(IllegalOfferingTransitionException.class);
  }

  @Test
  void draft_to_draft_throws_illegal_transition() {
    CourseOffering offering = offeringWithStatus(OfferingStatus.DRAFT, List.of(oneOption()));
    assertThatThrownBy(() -> offering.publishTo(OfferingStatus.DRAFT))
        .isInstanceOf(IllegalOfferingTransitionException.class);
  }

  // -------------------------------------------------------------------------
  // OfferingNotPublishableException (422) — zero schedule options for PRELIMINARY/FINAL
  // -------------------------------------------------------------------------

  @Test
  void draft_to_preliminary_without_options_throws_not_publishable() {
    CourseOffering offering = offeringWithStatus(OfferingStatus.DRAFT, Collections.emptyList());
    assertThatThrownBy(() -> offering.publishTo(OfferingStatus.PRELIMINARY))
        .isInstanceOf(OfferingNotPublishableException.class);
  }

  @Test
  void draft_to_final_without_options_throws_not_publishable() {
    CourseOffering offering = offeringWithStatus(OfferingStatus.DRAFT, Collections.emptyList());
    assertThatThrownBy(() -> offering.publishTo(OfferingStatus.FINAL))
        .isInstanceOf(OfferingNotPublishableException.class);
  }

  @Test
  void preliminary_to_final_without_options_throws_not_publishable() {
    CourseOffering offering =
        offeringWithStatus(OfferingStatus.PRELIMINARY, Collections.emptyList());
    assertThatThrownBy(() -> offering.publishTo(OfferingStatus.FINAL))
        .isInstanceOf(OfferingNotPublishableException.class);
  }

  // -------------------------------------------------------------------------
  // Immutability: publishTo returns new instance
  // -------------------------------------------------------------------------

  @Test
  void publishTo_returns_new_instance_not_same_ref() {
    CourseOffering offering = offeringWithStatus(OfferingStatus.DRAFT, List.of(oneOption()));
    CourseOffering result = offering.publishTo(OfferingStatus.PRELIMINARY);
    assertThat(result).isNotSameAs(offering);
  }

  @Test
  void publishTo_preserves_all_other_fields() {
    CourseOfferingId id = CourseOfferingId.generate();
    String notes = "Keep me";
    CourseOffering offering =
        CourseOffering.reconstitute(
            id,
            COURSE_ID,
            "CourseName",
            PROGRAM_ID,
            PERIOD_ID,
            OfferingStatus.DRAFT,
            notes,
            List.of(oneOption()),
            5L);

    CourseOffering result = offering.publishTo(OfferingStatus.PRELIMINARY);

    assertThat(result.getId()).isEqualTo(id);
    assertThat(result.getCourseId()).isEqualTo(COURSE_ID);
    assertThat(result.getCourseName()).isEqualTo("CourseName");
    assertThat(result.getProgramId()).isEqualTo(PROGRAM_ID);
    assertThat(result.getPeriodId()).isEqualTo(PERIOD_ID);
    assertThat(result.getNotes()).isEqualTo(notes);
    assertThat(result.getVersion()).isEqualTo(5L); // version carries; @Version bumps at flush
  }
}
