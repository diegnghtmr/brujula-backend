package co.edu.uniquindio.brujula.offering.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * TDD unit tests for {@link OfferingStatus#canTransitionTo(OfferingStatus)}.
 *
 * <p>Exhaustive coverage of all legal and illegal transitions per the spec OFFW-005 table.
 * Zero Spring — pure domain.
 */
class OfferingStatusTransitionTest {

  // ---- Legal transitions ----

  @Test
  void draft_can_transition_to_preliminary() {
    assertThat(OfferingStatus.DRAFT.canTransitionTo(OfferingStatus.PRELIMINARY)).isTrue();
  }

  @Test
  void draft_can_transition_to_final() {
    assertThat(OfferingStatus.DRAFT.canTransitionTo(OfferingStatus.FINAL)).isTrue();
  }

  @Test
  void preliminary_can_transition_to_final() {
    assertThat(OfferingStatus.PRELIMINARY.canTransitionTo(OfferingStatus.FINAL)).isTrue();
  }

  @Test
  void preliminary_can_transition_to_archived() {
    assertThat(OfferingStatus.PRELIMINARY.canTransitionTo(OfferingStatus.ARCHIVED)).isTrue();
  }

  @Test
  void final_can_transition_to_archived() {
    assertThat(OfferingStatus.FINAL.canTransitionTo(OfferingStatus.ARCHIVED)).isTrue();
  }

  // ---- DRAFT legal: also to ARCHIVED (design says DRAFT→ARCHIVED allowed) ----

  @Test
  void draft_can_transition_to_archived() {
    assertThat(OfferingStatus.DRAFT.canTransitionTo(OfferingStatus.ARCHIVED)).isTrue();
  }

  // ---- Same-state transitions — all illegal ----

  @Test
  void draft_to_draft_is_illegal() {
    assertThat(OfferingStatus.DRAFT.canTransitionTo(OfferingStatus.DRAFT)).isFalse();
  }

  @Test
  void preliminary_to_preliminary_is_illegal() {
    assertThat(OfferingStatus.PRELIMINARY.canTransitionTo(OfferingStatus.PRELIMINARY)).isFalse();
  }

  @Test
  void final_to_final_is_illegal() {
    assertThat(OfferingStatus.FINAL.canTransitionTo(OfferingStatus.FINAL)).isFalse();
  }

  @Test
  void archived_to_archived_is_illegal() {
    assertThat(OfferingStatus.ARCHIVED.canTransitionTo(OfferingStatus.ARCHIVED)).isFalse();
  }

  // ---- ARCHIVED is terminal — no outbound transitions ----

  @Test
  void archived_to_draft_is_illegal() {
    assertThat(OfferingStatus.ARCHIVED.canTransitionTo(OfferingStatus.DRAFT)).isFalse();
  }

  @Test
  void archived_to_preliminary_is_illegal() {
    assertThat(OfferingStatus.ARCHIVED.canTransitionTo(OfferingStatus.PRELIMINARY)).isFalse();
  }

  @Test
  void archived_to_final_is_illegal() {
    assertThat(OfferingStatus.ARCHIVED.canTransitionTo(OfferingStatus.FINAL)).isFalse();
  }

  // ---- Other illegal backward/skipped transitions ----

  @Test
  void preliminary_to_draft_is_illegal() {
    assertThat(OfferingStatus.PRELIMINARY.canTransitionTo(OfferingStatus.DRAFT)).isFalse();
  }

  @Test
  void final_to_draft_is_illegal() {
    assertThat(OfferingStatus.FINAL.canTransitionTo(OfferingStatus.DRAFT)).isFalse();
  }

  @Test
  void final_to_preliminary_is_illegal() {
    assertThat(OfferingStatus.FINAL.canTransitionTo(OfferingStatus.PRELIMINARY)).isFalse();
  }
}
