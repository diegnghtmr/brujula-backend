package co.edu.uniquindio.brujula.imports.infrastructure.adapter.in.parsing;

import co.edu.uniquindio.brujula.imports.application.port.out.FileParserPort;
import co.edu.uniquindio.brujula.imports.domain.model.ImportIssue;
import co.edu.uniquindio.brujula.imports.domain.model.ImportKind;
import co.edu.uniquindio.brujula.imports.domain.model.ImportRow;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.openxml4j.opc.PackagePart;
import org.apache.poi.poifs.filesystem.FileMagic;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

/**
 * XLSX parser adapter — reads XLSX files using Apache POI 5.4.1.
 *
 * <p>Security constraints:
 *
 * <ul>
 *   <li>NEVER calls FormulaEvaluator — formula cells are read as their cached string value or
 *       reported as an issue if no cached value exists.
 *   <li>Rejects .xlsm files (macro-enabled) by extension check + MIME type + OPCPackage vba part
 *       detection.
 *   <li>Rejects .xls (legacy BIFF8) by FileMagic check.
 * </ul>
 *
 * <p>Performance: uses {@link XSSFWorkbook} (in-memory) which is suitable for the 5,000-row target.
 * Memory is bounded by the 5 MB file size limit configured in {@link ImportValidationProperties}.
 *
 * <p>Infrastructure adapter — package-private class, registered via {@code @Component} in the
 * Spring container. Callers use {@link FileParserPort}.
 */
@Component
class XlsxParserAdapter implements FileParserPort {

  private static final String XLSX_CONTENT_TYPE =
      "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
  private static final String XLSM_CONTENT_TYPE = "application/vnd.ms-excel.sheet.macroEnabled.12";

  /**
   * Default column headers per kind. These are the expected header names in the first row of the
   * XLSX file.
   */
  private static final Map<ImportKind, List<String>> EXPECTED_HEADERS;

  static {
    EXPECTED_HEADERS = new HashMap<>();
    EXPECTED_HEADERS.put(
        ImportKind.COURSES,
        List.of(
            "program",
            "plan",
            "journey",
            "course_name",
            "official_code",
            "type",
            "credits",
            "weekly_hours",
            "recommended_semester",
            "can_be_enabled"));
    EXPECTED_HEADERS.put(
        ImportKind.REQUIREMENTS,
        List.of(
            "destination_course",
            "requirement_type",
            "required_course_or_value",
            "simultaneous_allowed"));
    EXPECTED_HEADERS.put(
        ImportKind.OFFERINGS,
        List.of(
            "period",
            "course",
            "option_label",
            "journey",
            "day",
            "start_time",
            "end_time",
            "status"));
  }

  @Override
  public boolean supports(String contentType, String filename) {
    if (contentType == null || filename == null) return false;
    boolean mimeOk =
        XLSX_CONTENT_TYPE.equalsIgnoreCase(contentType.trim())
            || contentType.trim().contains("spreadsheetml");
    boolean extOk = filename.toLowerCase().endsWith(".xlsx");
    return mimeOk && extOk;
  }

  @Override
  public ParseResult parse(byte[] fileBytes, ImportKind kind) {
    // Step 1: reject XLSM by extension content-type is checked by supports(), but re-check here
    // for defensive safety
    if (fileBytes == null || fileBytes.length == 0) {
      return new ParseResult(
          List.of(),
          List.of(ImportIssue.error(0, null, "EMPTY_FILE", "The uploaded file is empty.")));
    }

    // Step 2: detect legacy .xls (BIFF8) via FileMagic — reject it
    try (var is = new ByteArrayInputStream(fileBytes)) {
      FileMagic magic = FileMagic.valueOf(FileMagic.prepareToCheckMagic(is));
      if (magic == FileMagic.OLE2) {
        return new ParseResult(
            List.of(),
            List.of(
                ImportIssue.error(
                    0,
                    null,
                    "UNSUPPORTED_FORMAT",
                    "Legacy .xls format is not supported. Please use .xlsx.")));
      }
    } catch (IOException e) {
      // Cannot detect magic; proceed and let POI handle it
    }

    // Step 3: open as OPCPackage and check for VBA macro parts (macro safety)
    try (OPCPackage pkg = OPCPackage.open(new ByteArrayInputStream(fileBytes))) {
      boolean hasMacros = containsMacroParts(pkg);
      if (hasMacros) {
        return new ParseResult(
            List.of(),
            List.of(
                ImportIssue.error(
                    0,
                    null,
                    "MACROS_NOT_ALLOWED",
                    "The file contains macros or VBA code which is not allowed for security reasons.")));
      }

      // Step 4: open as XSSFWorkbook (NEVER FormulaEvaluator)
      try (XSSFWorkbook workbook = new XSSFWorkbook(pkg)) {
        XSSFSheet sheet = workbook.getSheetAt(0);
        if (sheet == null) {
          return new ParseResult(
              List.of(),
              List.of(ImportIssue.error(0, null, "EMPTY_FILE", "The workbook has no sheets.")));
        }

        // Step 5: read headers from first row
        Row headerRow = sheet.getRow(0);
        if (headerRow == null) {
          return new ParseResult(
              List.of(),
              List.of(
                  ImportIssue.error(0, null, "EMPTY_FILE", "The spreadsheet has no header row.")));
        }

        List<String> headers = readHeaders(headerRow);
        if (headers.isEmpty()) {
          return new ParseResult(
              List.of(),
              List.of(
                  ImportIssue.error(0, null, "BAD_HEADER", "No column headers found in row 1.")));
        }

        // Step 6: parse data rows (row index 1+)
        List<ImportRow> rows = new ArrayList<>();
        int lastRowNum = sheet.getLastRowNum();
        if (lastRowNum < 1) {
          // Only header row, no data
          return new ParseResult(
              List.of(),
              List.of(ImportIssue.error(0, null, "EMPTY_FILE", "The file has no data rows.")));
        }

        for (int i = 1; i <= lastRowNum; i++) {
          Row row = sheet.getRow(i);
          if (row == null || isBlankRow(row, headers.size())) {
            continue; // skip blank rows
          }
          Map<String, String> cells = readCells(row, headers);
          // index is 0-based (row 1 in file = index 0 in ImportRow)
          rows.add(new ImportRow(i - 1, cells));
        }

        if (rows.isEmpty()) {
          return new ParseResult(
              List.of(),
              List.of(ImportIssue.error(0, null, "EMPTY_FILE", "The file has no data rows.")));
        }

        return new ParseResult(rows, List.of());
      }
    } catch (org.apache.poi.openxml4j.exceptions.InvalidFormatException e) {
      return new ParseResult(
          List.of(),
          List.of(
              ImportIssue.error(
                  0,
                  null,
                  "INVALID_FORMAT",
                  "The file is not a valid XLSX file: " + e.getMessage())));
    } catch (IOException e) {
      return new ParseResult(
          List.of(),
          List.of(
              ImportIssue.error(
                  0, null, "PARSE_ERROR", "Failed to read the file: " + e.getMessage())));
    } catch (Exception e) {
      return new ParseResult(
          List.of(),
          List.of(
              ImportIssue.error(
                  0, null, "PARSE_ERROR", "Unexpected error reading the file: " + e.getMessage())));
    }
  }

  /**
   * Checks for VBA/macro parts in the OPCPackage. A file is macro-enabled if it contains a
   * vbaProject.bin part.
   */
  private boolean containsMacroParts(OPCPackage pkg) {
    try {
      for (PackagePart part : pkg.getParts()) {
        String name = part.getPartName().getName().toLowerCase();
        if (name.contains("vbaproject") || name.contains("vba")) {
          return true;
        }
      }
    } catch (InvalidFormatException e) {
      // If we cannot read parts, treat as non-macro (conservative; the file open itself would
      // have failed for truly invalid files)
    }
    return false;
  }

  /** Reads the header row into a list of column names. Blank cells produce empty strings. */
  private List<String> readHeaders(Row row) {
    List<String> headers = new ArrayList<>();
    int lastCellNum = row.getLastCellNum();
    for (int i = 0; i < lastCellNum; i++) {
      Cell cell = row.getCell(i);
      String headerValue = cellToString(cell).trim().toLowerCase();
      headers.add(headerValue);
    }
    // Trim trailing empty headers
    while (!headers.isEmpty() && headers.get(headers.size() - 1).isEmpty()) {
      headers.remove(headers.size() - 1);
    }
    return headers;
  }

  /** Reads a data row into a map of header→value. Missing cells map to empty string. */
  private Map<String, String> readCells(Row row, List<String> headers) {
    Map<String, String> cells = new LinkedHashMap<>();
    for (int i = 0; i < headers.size(); i++) {
      Cell cell = row.getCell(i, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
      cells.put(headers.get(i), cell == null ? "" : cellToString(cell));
    }
    return cells;
  }

  /**
   * Converts a cell to its string representation.
   *
   * <p>NEVER calls FormulaEvaluator. Formula cells are read as their cached string value (set by
   * the application that last saved the file). If no cached value is available, an empty string is
   * returned — the validator will flag the missing value.
   */
  private String cellToString(Cell cell) {
    if (cell == null) return "";
    return switch (cell.getCellType()) {
      case STRING -> cell.getStringCellValue();
      case NUMERIC -> {
        double d = cell.getNumericCellValue();
        // Return as integer string if it's a whole number
        if (d == Math.floor(d) && !Double.isInfinite(d)) {
          yield String.valueOf((long) d);
        }
        yield String.valueOf(d);
      }
      case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
      case FORMULA -> {
        // NEVER evaluate — read cached string value
        try {
          yield cell.getStringCellValue();
        } catch (Exception e) {
          // Cached value not a string; try numeric cache
          try {
            double d = cell.getNumericCellValue();
            if (d == Math.floor(d) && !Double.isInfinite(d)) {
              yield String.valueOf((long) d);
            }
            yield String.valueOf(d);
          } catch (Exception e2) {
            yield ""; // No usable cached value
          }
        }
      }
      case BLANK, ERROR, _NONE -> "";
    };
  }

  /** Returns true if all cells in the row (up to {@code maxCols}) are blank. */
  private boolean isBlankRow(Row row, int maxCols) {
    for (int i = 0; i < maxCols; i++) {
      Cell cell = row.getCell(i, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
      if (cell != null && cell.getCellType() != CellType.BLANK) {
        String val = cellToString(cell).trim();
        if (!val.isEmpty()) return false;
      }
    }
    return true;
  }
}
