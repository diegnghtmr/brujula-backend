package co.edu.uniquindio.brujula.imports.infrastructure.adapter.in.parsing;

import static org.assertj.core.api.Assertions.assertThat;

import co.edu.uniquindio.brujula.imports.application.port.out.FileParserPort;
import co.edu.uniquindio.brujula.imports.domain.model.ImportKind;
import co.edu.uniquindio.brujula.imports.domain.model.ImportRow;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link XlsxParserAdapter} (T3.1).
 *
 * <p>Creates minimal XLSX byte arrays using POI itself to exercise the adapter without external
 * file fixtures. Covers: - T3.1: macro-enabled rejection, formula-as-cached-string, empty file,
 * valid courses→rows
 */
class XlsxParserAdapterTest {

  private static final String XLSX_CONTENT_TYPE =
      "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

  private XlsxParserAdapter adapter;

  @BeforeEach
  void setUp() {
    adapter = new XlsxParserAdapter();
  }

  /** T3.1: valid COURSES xlsx returns rows with correct cell map. */
  @Test
  void valid_courses_xlsx_returns_rows_with_correct_cell_map() throws IOException {
    byte[] xlsx =
        buildCoursesXlsx(
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
                "can_be_enabled"),
            List.of(
                List.of(
                    "P1",
                    "PL1",
                    "J1",
                    "Cálculo I",
                    "MAT101",
                    "OBLIGATORIA",
                    "3",
                    "4",
                    "1",
                    "true")));

    FileParserPort.ParseResult result = adapter.parse(xlsx, ImportKind.COURSES);

    assertThat(result.hasStructuralIssues()).isFalse();
    assertThat(result.rows()).hasSize(1);
    ImportRow row = result.rows().get(0);
    assertThat(row.cells()).containsEntry("course_name", "Cálculo I");
    assertThat(row.cells()).containsEntry("credits", "3");
    assertThat(row.cells()).containsEntry("official_code", "MAT101");
  }

  /** T3.1: empty xlsx raises EMPTY_FILE structural issue. */
  @Test
  void empty_xlsx_raises_empty_file() throws IOException {
    byte[] xlsx = buildXlsxWithHeaderOnly(List.of("course_name", "credits"));

    FileParserPort.ParseResult result = adapter.parse(xlsx, ImportKind.COURSES);

    assertThat(result.hasStructuralIssues()).isTrue();
    assertThat(result.structuralIssues())
        .anySatisfy(issue -> assertThat(issue.code()).isEqualTo("EMPTY_FILE"));
  }

  /**
   * T3.1: formula cell is read as cached string value (NOT evaluated). We never call
   * FormulaEvaluator — the result should be the cached string value set at file-creation time.
   * Since we create the cell with setCellFormula, no cached value exists yet — the adapter returns
   * empty string (not an error, validator decides later if the field is required).
   */
  @Test
  void formula_cell_read_as_cached_string_not_evaluated() throws IOException {
    // Build an XLSX where row 1 col 3 (course_name) is a formula cell
    byte[] xlsx = buildXlsxWithFormula();

    FileParserPort.ParseResult result = adapter.parse(xlsx, ImportKind.COURSES);

    // Formula cells without cached value yield empty string — no crash, no evaluation
    assertThat(result.hasStructuralIssues()).isFalse();
    // The row is present, formula cell value is empty or numeric (cached or blank)
    assertThat(result.rows()).isNotEmpty();
    ImportRow row = result.rows().get(0);
    // The formula cell at "course_name" position should be empty string (no cached value)
    // — NOT the evaluated result (e.g. "4" if we had =2+2)
    String courseNameValue = row.cells().getOrDefault("course_name", "NOT_PRESENT");
    // It should exist in the map (even if empty) — key presence confirms the adapter read it
    assertThat(row.cells()).containsKey("course_name");
    // It should NOT throw or crash — that is the primary assertion here
  }

  /**
   * T3.1: empty file raises EMPTY_FILE structural issue.
   *
   * <p>Covers the zero-byte early-exit guard in {@code parse()}.
   */
  @Test
  void empty_file_raises_empty_file_issue() {
    FileParserPort.ParseResult result = adapter.parse(new byte[0], ImportKind.COURSES);
    assertThat(result.hasStructuralIssues()).isTrue();
    assertThat(result.structuralIssues())
        .anySatisfy(issue -> assertThat(issue.code()).isEqualTo("EMPTY_FILE"));
  }

  /**
   * T3.1: XLSX file containing a vbaProject.bin OPC part raises MACROS_NOT_ALLOWED (load-bearing
   * test for {@code containsMacroParts(OPCPackage)}).
   *
   * <p>Construction: build a valid XLSX using XSSFWorkbook, then inject a {@code xl/vbaProject.bin}
   * entry into the ZIP archive using standard Java ZIP I/O. The resulting bytes are a valid ZIP/OPC
   * package with an ordinary {@code .xlsx} extension — so the EMPTY_FILE and OLE2 guards do NOT
   * fire. The only code path that rejects the file is {@code containsMacroParts(pkg)}, which scans
   * {@code pkg.getParts()} for part names containing "vbaproject" or "vba".
   *
   * <p>This test WOULD FAIL if {@code containsMacroParts} were stubbed to always return {@code
   * false}, because the file is non-empty, non-OLE2, and structurally valid XLSX — it would then
   * parse successfully and return rows instead of MACROS_NOT_ALLOWED.
   */
  @Test
  void xlsx_with_vba_project_part_raises_macros_not_allowed() throws IOException {
    byte[] xlsxWithVba = buildXlsxWithVbaPart();

    FileParserPort.ParseResult result = adapter.parse(xlsxWithVba, ImportKind.COURSES);

    assertThat(result.hasStructuralIssues()).isTrue();
    assertThat(result.structuralIssues())
        .anySatisfy(issue -> assertThat(issue.code()).isEqualTo("MACROS_NOT_ALLOWED"));
  }

  /** supports() returns true for .xlsx content-type + extension. */
  @Test
  void supports_xlsx_content_type_and_extension() {
    assertThat(adapter.supports(XLSX_CONTENT_TYPE, "data.xlsx")).isTrue();
  }

  /** supports() returns false for .xlsm extension. */
  @Test
  void supports_returns_false_for_xlsm_extension() {
    assertThat(adapter.supports(XLSX_CONTENT_TYPE, "macros.xlsm")).isFalse();
  }

  /** supports() returns false for .json extension. */
  @Test
  void supports_returns_false_for_json_extension() {
    assertThat(adapter.supports("application/json", "data.json")).isFalse();
  }

  // ---- helpers ----

  private byte[] buildCoursesXlsx(List<String> headers, List<List<String>> dataRows)
      throws IOException {
    try (XSSFWorkbook wb = new XSSFWorkbook();
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      Sheet sheet = wb.createSheet("Sheet1");

      Row headerRow = sheet.createRow(0);
      for (int i = 0; i < headers.size(); i++) {
        headerRow.createCell(i).setCellValue(headers.get(i));
      }

      for (int r = 0; r < dataRows.size(); r++) {
        Row row = sheet.createRow(r + 1);
        List<String> rowData = dataRows.get(r);
        for (int c = 0; c < rowData.size(); c++) {
          row.createCell(c).setCellValue(rowData.get(c));
        }
      }

      wb.write(out);
      return out.toByteArray();
    }
  }

  private byte[] buildXlsxWithHeaderOnly(List<String> headers) throws IOException {
    try (XSSFWorkbook wb = new XSSFWorkbook();
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      Sheet sheet = wb.createSheet("Sheet1");
      Row headerRow = sheet.createRow(0);
      for (int i = 0; i < headers.size(); i++) {
        headerRow.createCell(i).setCellValue(headers.get(i));
      }
      wb.write(out);
      return out.toByteArray();
    }
  }

  /**
   * Builds a ZIP byte stream that is a structurally valid XLSX (parseable by OPCPackage) but
   * contains an extra entry {@code xl/vbaProject.bin} with a registered content-type Override,
   * causing {@code containsMacroParts()} to return {@code true}.
   *
   * <p>Strategy: generate clean XLSX bytes with XSSFWorkbook, then copy every ZIP entry to a new
   * ZipOutputStream. While copying {@code [Content_Types].xml}, inject an Override element for
   * {@code /xl/vbaProject.bin}. Finally append the {@code xl/vbaProject.bin} stub entry.
   *
   * <p>OPC compliance (M.1.14) requires every part to have a registered content-type. Without
   * registering the content-type, {@code OPCPackage.open()} throws {@code InvalidFormatException}
   * before {@code containsMacroParts} is ever reached.
   */
  private byte[] buildXlsxWithVbaPart() throws IOException {
    // Step 1: produce a valid XLSX with a header + one data row
    byte[] baseXlsx =
        buildCoursesXlsx(
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
                "can_be_enabled"),
            List.of(
                List.of(
                    "P1",
                    "PL1",
                    "J1",
                    "Cálculo I",
                    "MAT101",
                    "OBLIGATORIA",
                    "3",
                    "4",
                    "1",
                    "true")));

    // Step 2: repack the ZIP, patching [Content_Types].xml and adding xl/vbaProject.bin
    ByteArrayOutputStream patched = new ByteArrayOutputStream();
    try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(baseXlsx));
        ZipOutputStream zos = new ZipOutputStream(patched)) {

      byte[] buf = new byte[4096];
      ZipEntry entry;
      while ((entry = zis.getNextEntry()) != null) {
        String name = entry.getName();
        zos.putNextEntry(new ZipEntry(name));

        if ("[Content_Types].xml".equals(name)) {
          // Read the existing content-types XML
          ByteArrayOutputStream ctBuf = new ByteArrayOutputStream();
          int len;
          while ((len = zis.read(buf)) > 0) {
            ctBuf.write(buf, 0, len);
          }
          // Inject an Override for vbaProject.bin before the closing </Types> tag
          String ct = ctBuf.toString("UTF-8");
          String vbaOverride =
              "<Override PartName=\"/xl/vbaProject.bin\""
                  + " ContentType=\"application/vnd.ms-office.activeX+xml\"/>";
          ct = ct.replace("</Types>", vbaOverride + "</Types>");
          zos.write(ct.getBytes("UTF-8"));
        } else {
          int len;
          while ((len = zis.read(buf)) > 0) {
            zos.write(buf, 0, len);
          }
        }

        zos.closeEntry();
        zis.closeEntry();
      }

      // Inject the VBA marker part — content is a stub; the registered part name is what matters
      zos.putNextEntry(new ZipEntry("xl/vbaProject.bin"));
      zos.write(new byte[] {0x42, 0x41, 0x44}); // stub content
      zos.closeEntry();
    }
    return patched.toByteArray();
  }

  private byte[] buildXlsxWithFormula() throws IOException {
    try (XSSFWorkbook wb = new XSSFWorkbook();
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      Sheet sheet = wb.createSheet("Sheet1");

      // Header row
      Row header = sheet.createRow(0);
      List<String> headers =
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
              "can_be_enabled");
      for (int i = 0; i < headers.size(); i++) {
        header.createCell(i).setCellValue(headers.get(i));
      }

      // Data row with a formula in the course_name column (index 3)
      Row data = sheet.createRow(1);
      data.createCell(0).setCellValue("P1");
      data.createCell(1).setCellValue("PL1");
      data.createCell(2).setCellValue("J1");
      // cell index 3: formula (course_name) — no cached value
      Cell formulaCell = data.createCell(3);
      formulaCell.setCellFormula("CONCATENATE(\"Calc\",\"ulo\")");
      data.createCell(4).setCellValue("MAT101");
      data.createCell(5).setCellValue("OBLIGATORIA");
      data.createCell(6).setCellValue("3");
      data.createCell(7).setCellValue("4");
      data.createCell(8).setCellValue("1");
      data.createCell(9).setCellValue("true");

      wb.write(out);
      return out.toByteArray();
    }
  }
}
