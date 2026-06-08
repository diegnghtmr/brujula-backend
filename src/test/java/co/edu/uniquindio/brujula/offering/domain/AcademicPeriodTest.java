package co.edu.uniquindio.brujula.offering.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * TDD unit tests for {@link AcademicPeriod} write factory methods.
 *
 * <p>Phase 1 — domain write methods (OFFW-001, OFFW-002).
 * Pure Java, zero Spring/JPA.
 */
class AcademicPeriodTest {

  private final AcademicPeriodId anyId = AcademicPeriodId.of(UUID.randomUUID());
  private final LocalDate start = LocalDate.of(2026, 7, 1);
  private final LocalDate end = LocalDate.of(2026, 12, 15);

  // -------------------------------------------------------------------------
  // AcademicPeriod.create factory
  // -------------------------------------------------------------------------

  @Test
  void create_sets_all_fields_and_version_zero() {
    AcademicPeriod period = AcademicPeriod.create(anyId, "2026-2", start, end, PeriodStatus.UPCOMING);

    assertThat(period.getId()).isEqualTo(anyId);
    assertThat(period.getName()).isEqualTo("2026-2");
    assertThat(period.getStartDate()).isEqualTo(start);
    assertThat(period.getEndDate()).isEqualTo(end);
    assertThat(period.getStatus()).isEqualTo(PeriodStatus.UPCOMING);
    assertThat(period.getVersion()).isEqualTo(0L);
  }

  @Test
  void create_defaults_to_UPCOMING_when_status_is_null() {
    AcademicPeriod period = AcademicPeriod.create(anyId, "2026-2", start, end, null);

    assertThat(period.getStatus()).isEqualTo(PeriodStatus.UPCOMING);
  }

  @Test
  void create_rejects_null_id() {
    assertThatThrownBy(() -> AcademicPeriod.create(null, "2026-2", start, end, PeriodStatus.UPCOMING))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void create_rejects_null_name() {
    assertThatThrownBy(() -> AcademicPeriod.create(anyId, null, start, end, PeriodStatus.UPCOMING))
        .isInstanceOf(NullPointerException.class);
  }

  // -------------------------------------------------------------------------
  // AcademicPeriod.update
  // -------------------------------------------------------------------------

  @Test
  void update_applies_all_non_null_fields() {
    AcademicPeriod original = AcademicPeriod.create(anyId, "2026-2", start, end, PeriodStatus.UPCOMING);

    LocalDate newStart = LocalDate.of(2026, 8, 1);
    LocalDate newEnd = LocalDate.of(2026, 12, 31);
    AcademicPeriod updated = original.update(newStart, newEnd, PeriodStatus.ACTIVE);

    assertThat(updated.getStartDate()).isEqualTo(newStart);
    assertThat(updated.getEndDate()).isEqualTo(newEnd);
    assertThat(updated.getStatus()).isEqualTo(PeriodStatus.ACTIVE);
    // Unchanged fields preserved
    assertThat(updated.getId()).isEqualTo(anyId);
    assertThat(updated.getName()).isEqualTo("2026-2");
    assertThat(updated.getVersion()).isEqualTo(original.getVersion());
  }

  @Test
  void update_preserves_original_values_for_null_params() {
    AcademicPeriod original = AcademicPeriod.create(anyId, "2026-2", start, end, PeriodStatus.ACTIVE);

    AcademicPeriod updated = original.update(null, null, null);

    assertThat(updated.getStartDate()).isEqualTo(start);
    assertThat(updated.getEndDate()).isEqualTo(end);
    assertThat(updated.getStatus()).isEqualTo(PeriodStatus.ACTIVE);
  }

  @Test
  void update_returns_new_instance_not_same_reference() {
    AcademicPeriod original = AcademicPeriod.create(anyId, "2026-2", start, end, PeriodStatus.UPCOMING);
    AcademicPeriod updated = original.update(null, null, PeriodStatus.ACTIVE);

    assertThat(updated).isNotSameAs(original);
  }
}
