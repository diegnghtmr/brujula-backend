package co.edu.uniquindio.brujula.imports.infrastructure.adapter.in.parsing;

import co.edu.uniquindio.brujula.imports.application.port.out.FileParserPort;
import co.edu.uniquindio.brujula.imports.domain.model.ImportIssue;
import co.edu.uniquindio.brujula.imports.domain.model.ImportKind;
import co.edu.uniquindio.brujula.imports.domain.model.ImportRow;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/**
 * JSON parser adapter — reads JSON import files using Jackson 3.
 *
 * <p>Expected JSON format: a top-level array of objects, where each object maps column header names
 * to string values. Example:
 *
 * <pre>
 * [
 *   { "course_name": "Cálculo I", "official_code": "MAT101", "credits": "3", ... },
 *   ...
 * ]
 * </pre>
 *
 * <p>All values are read as strings (consistent with XLSX cell representation). Numeric and boolean
 * JSON values are coerced to their string representation.
 *
 * <p>Uses {@code tools.jackson} (Jackson 3) — the default Spring Boot 4 ObjectMapper. Do NOT use
 * {@code com.fasterxml.jackson} (Jackson 2) here, as Spring Boot 4 auto-configures only the Jackson
 * 3 bean.
 *
 * <p>Infrastructure adapter — package-private class, registered via {@code @Component}. Callers use
 * {@link FileParserPort}.
 */
@Component
class JsonParserAdapter implements FileParserPort {

  private final ObjectMapper objectMapper;

  JsonParserAdapter(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  public boolean supports(String contentType, String filename) {
    if (contentType == null || filename == null) return false;
    boolean mimeOk = contentType.trim().toLowerCase().contains("json");
    boolean extOk = filename.toLowerCase().endsWith(".json");
    return mimeOk && extOk;
  }

  @Override
  public ParseResult parse(byte[] fileBytes, ImportKind kind) {
    if (fileBytes == null || fileBytes.length == 0) {
      return new ParseResult(
          List.of(),
          List.of(ImportIssue.error(0, null, "EMPTY_FILE", "The uploaded file is empty.")));
    }

    try {
      List<Map<String, Object>> rawRows =
          objectMapper.readValue(fileBytes, new TypeReference<List<Map<String, Object>>>() {});

      if (rawRows == null || rawRows.isEmpty()) {
        return new ParseResult(
            List.of(),
            List.of(
                ImportIssue.error(0, null, "EMPTY_FILE", "The JSON file contains no data rows.")));
      }

      List<ImportRow> rows = new ArrayList<>();
      for (int i = 0; i < rawRows.size(); i++) {
        Map<String, Object> raw = rawRows.get(i);
        if (raw == null) continue;
        Map<String, String> cells = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : raw.entrySet()) {
          String key = entry.getKey() != null ? entry.getKey().trim().toLowerCase() : "";
          String value = entry.getValue() != null ? String.valueOf(entry.getValue()) : "";
          cells.put(key, value);
        }
        rows.add(new ImportRow(i, cells));
      }

      if (rows.isEmpty()) {
        return new ParseResult(
            List.of(),
            List.of(
                ImportIssue.error(0, null, "EMPTY_FILE", "The JSON file contains no data rows.")));
      }

      return new ParseResult(rows, List.of());

    } catch (JacksonException e) {
      return new ParseResult(
          List.of(),
          List.of(
              ImportIssue.error(
                  0, null, "INVALID_FORMAT", "The file is not valid JSON: " + e.getMessage())));
    }
  }
}
