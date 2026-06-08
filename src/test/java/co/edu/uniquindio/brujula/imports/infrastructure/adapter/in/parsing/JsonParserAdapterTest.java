package co.edu.uniquindio.brujula.imports.infrastructure.adapter.in.parsing;

import static org.assertj.core.api.Assertions.assertThat;

import co.edu.uniquindio.brujula.imports.application.port.out.FileParserPort;
import co.edu.uniquindio.brujula.imports.domain.model.ImportKind;
import co.edu.uniquindio.brujula.imports.domain.model.ImportRow;
import tools.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link JsonParserAdapter} (T3.1).
 *
 * <p>Covers:
 * - valid JSON input returns rows
 * - malformed JSON raises parse error
 */
class JsonParserAdapterTest {

  private JsonParserAdapter adapter;

  @BeforeEach
  void setUp() {
    adapter = new JsonParserAdapter(new ObjectMapper());
  }

  /** T3.1: valid JSON array returns rows with correct cell map. */
  @Test
  void valid_json_input_returns_rows() {
    String json =
        """
        [
          {"course_name": "Cálculo I", "official_code": "MAT101", "credits": "3",
           "type": "OBLIGATORIA", "weekly_hours": "4", "recommended_semester": "1",
           "can_be_enabled": "true", "journey": "J1", "program": "P1", "plan": "PL1"}
        ]
        """;
    byte[] bytes = json.getBytes(StandardCharsets.UTF_8);

    FileParserPort.ParseResult result = adapter.parse(bytes, ImportKind.COURSES);

    assertThat(result.hasStructuralIssues()).isFalse();
    assertThat(result.rows()).hasSize(1);
    ImportRow row = result.rows().get(0);
    assertThat(row.cells()).containsEntry("course_name", "Cálculo I");
    assertThat(row.cells()).containsEntry("credits", "3");
  }

  /** T3.1: valid JSON with multiple rows returns all rows with 0-based index. */
  @Test
  void valid_json_multiple_rows_returns_correct_indices() {
    String json =
        """
        [
          {"course_name": "Cálculo I"},
          {"course_name": "Álgebra Lineal"}
        ]
        """;
    byte[] bytes = json.getBytes(StandardCharsets.UTF_8);

    FileParserPort.ParseResult result = adapter.parse(bytes, ImportKind.COURSES);

    assertThat(result.rows()).hasSize(2);
    assertThat(result.rows().get(0).index()).isEqualTo(0);
    assertThat(result.rows().get(1).index()).isEqualTo(1);
  }

  /** T3.1: malformed JSON raises parse error structural issue. */
  @Test
  void malformed_json_raises_parse_error() {
    byte[] bytes = "{not valid json".getBytes(StandardCharsets.UTF_8);

    FileParserPort.ParseResult result = adapter.parse(bytes, ImportKind.COURSES);

    assertThat(result.hasStructuralIssues()).isTrue();
    assertThat(result.structuralIssues())
        .anySatisfy(issue -> assertThat(issue.code()).isEqualTo("INVALID_FORMAT"));
  }

  /** T3.1: empty byte array raises EMPTY_FILE. */
  @Test
  void empty_bytes_raises_empty_file() {
    FileParserPort.ParseResult result = adapter.parse(new byte[0], ImportKind.COURSES);

    assertThat(result.hasStructuralIssues()).isTrue();
    assertThat(result.structuralIssues())
        .anySatisfy(issue -> assertThat(issue.code()).isEqualTo("EMPTY_FILE"));
  }

  /** T3.1: empty JSON array raises EMPTY_FILE. */
  @Test
  void empty_json_array_raises_empty_file() {
    byte[] bytes = "[]".getBytes(StandardCharsets.UTF_8);

    FileParserPort.ParseResult result = adapter.parse(bytes, ImportKind.COURSES);

    assertThat(result.hasStructuralIssues()).isTrue();
    assertThat(result.structuralIssues())
        .anySatisfy(issue -> assertThat(issue.code()).isEqualTo("EMPTY_FILE"));
  }

  /** supports() returns true for application/json + .json extension. */
  @Test
  void supports_json_content_type_and_extension() {
    assertThat(adapter.supports("application/json", "data.json")).isTrue();
  }

  /** supports() returns false for .xlsx extension. */
  @Test
  void supports_returns_false_for_xlsx_extension() {
    assertThat(
            adapter.supports(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "data.xlsx"))
        .isFalse();
  }
}
