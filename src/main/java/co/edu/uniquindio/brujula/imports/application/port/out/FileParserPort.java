package co.edu.uniquindio.brujula.imports.application.port.out;

import co.edu.uniquindio.brujula.imports.domain.model.ImportIssue;
import co.edu.uniquindio.brujula.imports.domain.model.ImportKind;
import co.edu.uniquindio.brujula.imports.domain.model.ImportRow;
import java.util.List;

/**
 * Output port: parse a raw file into a list of data rows.
 *
 * <p>Implementations live in infrastructure (XlsxParserAdapter, JsonParserAdapter). The service
 * layer selects the implementation by content-type / file extension. Structural errors (bad
 * headers, macros, unparseable JSON) are returned as a non-empty {@link ParseResult#structuralIssues()}
 * list; callers treat this as a file-level rejection.
 *
 * <p>Framework-free interface (ARQ-002/003). Parser adapters are {@code @Component}-annotated
 * in infrastructure and selected at runtime by the service.
 */
public interface FileParserPort {

  /**
   * Returns {@code true} if this parser can handle the given content-type and filename combination.
   *
   * @param contentType the HTTP content-type of the uploaded file
   * @param filename the original filename
   * @return {@code true} if this parser supports the given format
   */
  boolean supports(String contentType, String filename);

  /**
   * Parse the raw file bytes into rows. The column headers expected depend on the {@code kind}.
   *
   * @param fileBytes raw file content
   * @param kind the import kind (COURSES, REQUIREMENTS, OFFERINGS) — used for header mapping
   * @return parse result: rows and/or structural issues
   */
  ParseResult parse(byte[] fileBytes, ImportKind kind);

  /**
   * Result of a file parse operation.
   *
   * @param rows the parsed data rows (empty if structural issues exist)
   * @param structuralIssues global file-level issues (e.g. MACROS_NOT_ALLOWED, EMPTY_FILE);
   *     row = 0 by convention for all structural issues
   */
  record ParseResult(List<ImportRow> rows, List<ImportIssue> structuralIssues) {

    public ParseResult {
      rows = rows != null ? List.copyOf(rows) : List.of();
      structuralIssues = structuralIssues != null ? List.copyOf(structuralIssues) : List.of();
    }

    /** Returns {@code true} if structural issues were detected (file cannot be safely processed). */
    public boolean hasStructuralIssues() {
      return !structuralIssues.isEmpty();
    }
  }
}
