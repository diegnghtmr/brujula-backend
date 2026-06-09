package co.edu.uniquindio.brujula.imports.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import co.edu.uniquindio.brujula.imports.domain.model.ImportIssue;
import co.edu.uniquindio.brujula.imports.domain.model.ImportRow;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link RequirementsValidator} (T3.2).
 *
 * <p>Covers per-row validation and intra-batch cycle detection (CIRCULAR_SEQUENCE).
 */
class RequirementsValidatorTest {

  private RequirementsValidator validator;

  @BeforeEach
  void setUp() {
    validator = new RequirementsValidator();
  }

  /** T3.2 SC-002-B (STRICT TDD CRITICAL): intra-batch cycle detected → CIRCULAR_SEQUENCE. */
  @Test
  void cycle_produces_circular_sequence_error() {
    // Row 0: B requires A (A → B edge)
    // Row 1: A requires B (B → A edge) — creates cycle
    List<ImportRow> rows =
        List.of(
            new ImportRow(
                0,
                Map.of(
                    "destination_course", "B",
                    "requirement_type", "COURSE_PASSED",
                    "required_course_or_value", "A",
                    "simultaneous_allowed", "false")),
            new ImportRow(
                1,
                Map.of(
                    "destination_course", "A",
                    "requirement_type", "COURSE_PASSED",
                    "required_course_or_value", "B",
                    "simultaneous_allowed", "false")));

    Map<Integer, List<ImportIssue>> issueMap = validator.validateAll(rows);

    // At least one row should have CIRCULAR_SEQUENCE
    boolean hasCircularSequence =
        issueMap.values().stream()
            .flatMap(List::stream)
            .anyMatch(issue -> "CIRCULAR_SEQUENCE".equals(issue.code()));
    assertThat(hasCircularSequence).isTrue();
  }

  /** T3.2 SC-007-B: intra-batch cycle detected across rows. */
  @Test
  void intra_batch_cycle_detected_across_rows() {
    // Chain: A→B, B→C, C→A (three-node cycle)
    List<ImportRow> rows =
        List.of(
            new ImportRow(
                0,
                Map.of(
                    "destination_course", "B",
                    "requirement_type", "SEQUENCE",
                    "required_course_or_value", "A",
                    "simultaneous_allowed", "false")),
            new ImportRow(
                1,
                Map.of(
                    "destination_course", "C",
                    "requirement_type", "SEQUENCE",
                    "required_course_or_value", "B",
                    "simultaneous_allowed", "false")),
            new ImportRow(
                2,
                Map.of(
                    "destination_course", "A",
                    "requirement_type", "SEQUENCE",
                    "required_course_or_value", "C",
                    "simultaneous_allowed", "false")));

    Map<Integer, List<ImportIssue>> issueMap = validator.validateAll(rows);

    boolean hasCircularSequence =
        issueMap.values().stream()
            .flatMap(List::stream)
            .anyMatch(issue -> "CIRCULAR_SEQUENCE".equals(issue.code()));
    assertThat(hasCircularSequence).isTrue();
  }

  /** T3.2 SC-007-C: destination course missing produces DESTINATION_COURSE_NOT_FOUND. */
  @Test
  void destination_course_not_found_produces_error() {
    ImportRow row =
        new ImportRow(
            0,
            Map.of(
                "destination_course", "",
                "requirement_type", "COURSE_PASSED",
                "required_course_or_value", "MAT101",
                "simultaneous_allowed", "false"));

    Map<Integer, List<ImportIssue>> issueMap = validator.validateAll(List.of(row));

    assertThat(issueMap).containsKey(0);
    assertThat(issueMap.get(0))
        .anySatisfy(issue -> assertThat(issue.code()).isEqualTo("DESTINATION_COURSE_NOT_FOUND"));
  }

  /** T3.2: valid requirement row produces no issues. */
  @Test
  void valid_row_produces_no_issues() {
    ImportRow row =
        new ImportRow(
            0,
            Map.of(
                "destination_course", "MAT201",
                "requirement_type", "COURSE_PASSED",
                "required_course_or_value", "MAT101",
                "simultaneous_allowed", "false"));

    Map<Integer, List<ImportIssue>> issueMap = validator.validateAll(List.of(row));

    assertThat(issueMap).doesNotContainKey(0);
  }

  /** T3.2: invalid requirement_type produces INVALID_REQUIREMENT_TYPE. */
  @Test
  void invalid_requirement_type_produces_error() {
    ImportRow row =
        new ImportRow(
            0,
            Map.of(
                "destination_course", "MAT201",
                "requirement_type", "UNKNOWN_TYPE",
                "required_course_or_value", "MAT101",
                "simultaneous_allowed", "false"));

    Map<Integer, List<ImportIssue>> issueMap = validator.validateAll(List.of(row));

    assertThat(issueMap).containsKey(0);
    assertThat(issueMap.get(0))
        .anySatisfy(issue -> assertThat(issue.code()).isEqualTo("INVALID_REQUIREMENT_TYPE"));
  }

  /**
   * T3.2 SC-007-B (a): self-loop A requires A is flagged as CIRCULAR_SEQUENCE.
   *
   * <p>Uses two rows (one self-loop + one unrelated) so that the batch size exceeds the
   * minimum-two-rows threshold that activates cycle detection in {@code detectIntrabatchCycles}.
   * The self-loop row (A→A) is the cycle-introducing edge: the DFS visits A, marks it GRAY, then
   * sees successor A is GRAY → back-edge detected.
   */
  @Test
  void self_loop_produces_circular_sequence_error() {
    List<ImportRow> rows =
        List.of(
            // Row 0: A requires A — self-loop
            new ImportRow(
                0,
                Map.of(
                    "destination_course", "A",
                    "requirement_type", "COURSE_PASSED",
                    "required_course_or_value", "A",
                    "simultaneous_allowed", "false")),
            // Row 1: unrelated valid row to satisfy the size >= 2 threshold
            new ImportRow(
                1,
                Map.of(
                    "destination_course", "C",
                    "requirement_type", "COURSE_PASSED",
                    "required_course_or_value", "B",
                    "simultaneous_allowed", "false")));

    Map<Integer, List<ImportIssue>> issueMap = validator.validateAll(rows);

    boolean hasCircularSequence =
        issueMap.values().stream()
            .flatMap(List::stream)
            .anyMatch(issue -> "CIRCULAR_SEQUENCE".equals(issue.code()));
    assertThat(hasCircularSequence).isTrue();
  }

  /**
   * T3.2 SC-007-B (b): diamond/DAG A→B, A→C, B→D, C→D does NOT produce CIRCULAR_SEQUENCE. This
   * proves the detector has no false positives on shared-dependency graphs.
   */
  @Test
  void diamond_dag_does_not_produce_circular_sequence_error() {
    // dest=B requires A (edge A→B)
    // dest=C requires A (edge A→C)
    // dest=D requires B (edge B→D)
    // dest=D requires C (edge C→D) — duplicate (D,C) pair shares dest with (D,B) but not duplicate
    // NOTE: (D,B) and (D,C) have different required_course, so no duplicate error
    List<ImportRow> rows =
        List.of(
            new ImportRow(
                0,
                Map.of(
                    "destination_course", "B",
                    "requirement_type", "COURSE_PASSED",
                    "required_course_or_value", "A",
                    "simultaneous_allowed", "false")),
            new ImportRow(
                1,
                Map.of(
                    "destination_course", "C",
                    "requirement_type", "COURSE_PASSED",
                    "required_course_or_value", "A",
                    "simultaneous_allowed", "false")),
            new ImportRow(
                2,
                Map.of(
                    "destination_course", "D",
                    "requirement_type", "COURSE_PASSED",
                    "required_course_or_value", "B",
                    "simultaneous_allowed", "false")),
            new ImportRow(
                3,
                Map.of(
                    "destination_course", "D",
                    "requirement_type", "SEQUENCE",
                    "required_course_or_value", "C",
                    "simultaneous_allowed", "false")));

    Map<Integer, List<ImportIssue>> issueMap = validator.validateAll(rows);

    boolean hasCircularSequence =
        issueMap.values().stream()
            .flatMap(List::stream)
            .anyMatch(issue -> "CIRCULAR_SEQUENCE".equals(issue.code()));
    assertThat(hasCircularSequence).isFalse();
  }

  /**
   * T3.2 SC-007-B (c): disconnected graph where only the second component has a cycle → flagged.
   * Component 1: X→Y (acyclic). Component 2: P→Q, Q→P (cycle).
   */
  @Test
  void disconnected_graph_cycle_in_second_component_is_flagged() {
    List<ImportRow> rows =
        List.of(
            new ImportRow(
                0,
                Map.of(
                    "destination_course", "Y",
                    "requirement_type", "COURSE_PASSED",
                    "required_course_or_value", "X",
                    "simultaneous_allowed", "false")),
            new ImportRow(
                1,
                Map.of(
                    "destination_course", "Q",
                    "requirement_type", "COURSE_PASSED",
                    "required_course_or_value", "P",
                    "simultaneous_allowed", "false")),
            new ImportRow(
                2,
                Map.of(
                    "destination_course", "P",
                    "requirement_type", "COURSE_PASSED",
                    "required_course_or_value", "Q",
                    "simultaneous_allowed", "false")));

    Map<Integer, List<ImportIssue>> issueMap = validator.validateAll(rows);

    boolean hasCircularSequence =
        issueMap.values().stream()
            .flatMap(List::stream)
            .anyMatch(issue -> "CIRCULAR_SEQUENCE".equals(issue.code()));
    assertThat(hasCircularSequence).isTrue();
  }

  /** T3.2 SC-007-B (d): four-node cycle A→B→C→D→A is flagged as CIRCULAR_SEQUENCE. */
  @Test
  void four_node_cycle_produces_circular_sequence_error() {
    List<ImportRow> rows =
        List.of(
            new ImportRow(
                0,
                Map.of(
                    "destination_course", "B",
                    "requirement_type", "SEQUENCE",
                    "required_course_or_value", "A",
                    "simultaneous_allowed", "false")),
            new ImportRow(
                1,
                Map.of(
                    "destination_course", "C",
                    "requirement_type", "SEQUENCE",
                    "required_course_or_value", "B",
                    "simultaneous_allowed", "false")),
            new ImportRow(
                2,
                Map.of(
                    "destination_course", "D",
                    "requirement_type", "SEQUENCE",
                    "required_course_or_value", "C",
                    "simultaneous_allowed", "false")),
            new ImportRow(
                3,
                Map.of(
                    "destination_course", "A",
                    "requirement_type", "SEQUENCE",
                    "required_course_or_value", "D",
                    "simultaneous_allowed", "false")));

    Map<Integer, List<ImportIssue>> issueMap = validator.validateAll(rows);

    boolean hasCircularSequence =
        issueMap.values().stream()
            .flatMap(List::stream)
            .anyMatch(issue -> "CIRCULAR_SEQUENCE".equals(issue.code()));
    assertThat(hasCircularSequence).isTrue();
  }

  /** T3.2: duplicate (destination, required) pair produces DUPLICATE_REQUIREMENT_IN_BATCH. */
  @Test
  void duplicate_pair_produces_duplicate_requirement_in_batch() {
    List<ImportRow> rows =
        List.of(
            new ImportRow(
                0,
                Map.of(
                    "destination_course", "MAT201",
                    "requirement_type", "COURSE_PASSED",
                    "required_course_or_value", "MAT101",
                    "simultaneous_allowed", "false")),
            new ImportRow(
                1,
                Map.of(
                    "destination_course", "MAT201",
                    "requirement_type", "COURSE_PASSED",
                    "required_course_or_value", "MAT101",
                    "simultaneous_allowed", "false")));

    Map<Integer, List<ImportIssue>> issueMap = validator.validateAll(rows);

    // Row 1 should have the duplicate error
    assertThat(issueMap).containsKey(1);
    assertThat(issueMap.get(1))
        .anySatisfy(issue -> assertThat(issue.code()).isEqualTo("DUPLICATE_REQUIREMENT_IN_BATCH"));
  }
}
