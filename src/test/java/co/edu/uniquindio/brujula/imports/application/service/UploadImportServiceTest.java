package co.edu.uniquindio.brujula.imports.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.edu.uniquindio.brujula.audit.application.port.in.AuditRecorder;
import co.edu.uniquindio.brujula.imports.application.dto.ImportSummary;
import co.edu.uniquindio.brujula.imports.application.dto.UploadImportCommand;
import co.edu.uniquindio.brujula.imports.application.port.out.FileParserPort;
import co.edu.uniquindio.brujula.imports.application.port.out.SaveImportBatchPort;
import co.edu.uniquindio.brujula.imports.application.port.out.StaffAssignmentPort;
import co.edu.uniquindio.brujula.imports.domain.exception.FileSizeExceededException;
import co.edu.uniquindio.brujula.imports.domain.exception.ImportAccessDeniedException;
import co.edu.uniquindio.brujula.imports.domain.exception.MissingImportParameterException;
import co.edu.uniquindio.brujula.imports.domain.exception.UnsupportedFileTypeException;
import co.edu.uniquindio.brujula.imports.domain.model.ImportBatch;
import co.edu.uniquindio.brujula.imports.domain.model.ImportKind;
import co.edu.uniquindio.brujula.imports.domain.model.ImportRow;
import co.edu.uniquindio.brujula.sharedkernel.application.exception.IdempotencyKeyConflictException;
import co.edu.uniquindio.brujula.sharedkernel.application.port.out.IdempotencyStorePort;
import co.edu.uniquindio.brujula.sharedkernel.application.port.out.JsonCodecPort;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * Unit tests for {@link UploadImportService} (T3.3).
 *
 * <p>Covers idempotency, assignment guard (including WARNING-1 closure), file-type/size guard,
 * audit recording, and outcome (VALIDATED vs INVALID).
 */
class UploadImportServiceTest {

  private static final String XLSX_CONTENT_TYPE =
      "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
  private static final long MAX_FILE_SIZE = 5_242_880L; // 5 MB

  private FileParserPort xlsxParser;
  private ImportValidator importValidator;
  private SaveImportBatchPort saveImportBatchPort;
  private StaffAssignmentPort staffAssignmentPort;
  private IdempotencyStorePort idempotencyStorePort;
  private JsonCodecPort jsonCodecPort;
  private AuditRecorder auditRecorder;
  private UploadImportService service;

  @BeforeEach
  void setUp() {
    xlsxParser = mock(FileParserPort.class);
    importValidator =
        new ImportValidator(
            new CoursesValidator(),
            new RequirementsValidator(),
            new OfferingsValidator());
    saveImportBatchPort = mock(SaveImportBatchPort.class);
    staffAssignmentPort = mock(StaffAssignmentPort.class);
    idempotencyStorePort = mock(IdempotencyStorePort.class);
    jsonCodecPort = mock(JsonCodecPort.class);
    auditRecorder = mock(AuditRecorder.class);

    // Default: parser supports xlsx
    when(xlsxParser.supports(eq(XLSX_CONTENT_TYPE), anyString())).thenReturn(true);
    when(idempotencyStorePort.find(any(), anyString())).thenReturn(Optional.empty());

    service =
        new UploadImportService(
            List.of(xlsxParser),
            importValidator,
            saveImportBatchPort,
            staffAssignmentPort,
            idempotencyStorePort,
            jsonCodecPort,
            auditRecorder,
            MAX_FILE_SIZE);
  }

  /**
   * T3.3 SC-001-A: happy path — STAFF assigned to program → returns VALIDATED summary.
   */
  @Test
  void happy_path_staff_assigned_returns_validated_summary() {
    UUID staffId = UUID.randomUUID();
    UUID programId = UUID.randomUUID();
    UUID planId = UUID.randomUUID();

    // Staff is ACTIVE and assigned
    when(staffAssignmentPort.resolveActiveStaffId("staff@uniquindio.edu.co"))
        .thenReturn(Optional.of(staffId));
    when(staffAssignmentPort.isAssigned(staffId, programId)).thenReturn(true);

    // Parser returns 2 valid courses rows
    List<ImportRow> rows = List.of(
        new ImportRow(0, Map.of("course_name", "Cálculo I", "credits", "3", "weekly_hours", "4",
            "recommended_semester", "1", "type", "OBLIGATORIA", "journey", "J1")),
        new ImportRow(1, Map.of("course_name", "Álgebra", "credits", "3", "weekly_hours", "4",
            "recommended_semester", "2", "type", "OBLIGATORIA", "journey", "J1")));
    when(xlsxParser.parse(any(), any()))
        .thenReturn(new FileParserPort.ParseResult(rows, List.of()));

    String summaryJson = "{\"id\":\"abc\"}";
    when(jsonCodecPort.toJson(any())).thenReturn(summaryJson);

    UploadImportCommand cmd = buildCommand("staff@uniquindio.edu.co", "STAFF", programId, planId, null,
        ImportKind.COURSES, new byte[100]);

    ImportSummary summary = service.upload(cmd);

    assertThat(summary.status()).isEqualTo("VALIDATED");
    assertThat(summary.totalRows()).isEqualTo(2);
    assertThat(summary.validRows()).isEqualTo(2);

    // Verify save and audit were called
    verify(saveImportBatchPort).save(any(ImportBatch.class));
    verify(auditRecorder).record(any());
    verify(idempotencyStorePort).save(any(), anyString(), anyString(), eq(201), anyString());
  }

  /**
   * T3.3 SC-001-B: unsupported file format → UnsupportedFileTypeException (→ 415).
   */
  @Test
  void wrong_mime_raises_415_exception() {
    UploadImportCommand cmd = buildCommand("admin@uniquindio.edu.co", "ADMIN",
        UUID.randomUUID(), UUID.randomUUID(), null,
        ImportKind.COURSES, new byte[100],
        "text/csv", "data.csv");

    // No parser supports text/csv
    when(xlsxParser.supports("text/csv", "data.csv")).thenReturn(false);

    assertThatThrownBy(() -> service.upload(cmd))
        .isInstanceOf(UnsupportedFileTypeException.class);

    // Ensure no batch was saved, no audit
    verify(saveImportBatchPort, never()).save(any());
    verify(auditRecorder, never()).record(any());
  }

  /**
   * T3.3 SC-001-C: file exceeds size limit → FileSizeExceededException (→ 413).
   */
  @Test
  void file_exceeds_size_limit_raises_413_exception() {
    byte[] largeFile = new byte[(int) MAX_FILE_SIZE + 1];
    UploadImportCommand cmd = buildCommand("admin@uniquindio.edu.co", "ADMIN",
        UUID.randomUUID(), UUID.randomUUID(), null,
        ImportKind.COURSES, largeFile);

    assertThatThrownBy(() -> service.upload(cmd))
        .isInstanceOf(FileSizeExceededException.class);

    verify(saveImportBatchPort, never()).save(any());
  }

  /**
   * T3.3 SC-001-D / INV-06: STAFF not assigned to program → ImportAccessDeniedException (→ 403).
   * No batch persisted, no audit.
   */
  @Test
  void staff_not_assigned_raises_403_exception_before_batch_persisted() {
    UUID staffId = UUID.randomUUID();
    UUID programId = UUID.randomUUID();

    when(staffAssignmentPort.resolveActiveStaffId("staff@uniquindio.edu.co"))
        .thenReturn(Optional.of(staffId));
    when(staffAssignmentPort.isAssigned(staffId, programId)).thenReturn(false);

    UploadImportCommand cmd = buildCommand("staff@uniquindio.edu.co", "STAFF",
        programId, UUID.randomUUID(), null, ImportKind.COURSES, new byte[100]);

    assertThatThrownBy(() -> service.upload(cmd))
        .isInstanceOf(ImportAccessDeniedException.class);

    // SC-011-C: no audit on 403 rejection
    verify(auditRecorder, never()).record(any());
    verify(saveImportBatchPort, never()).save(any());
  }

  /**
   * T3.3 WARNING-1 (LOAD-BEARING): inactive staff account → ImportAccessDeniedException (→ 403).
   *
   * <p>This test is load-bearing: if {@link UploadImportService#enforceAssignmentGuard} is
   * changed to call {@link StaffAssignmentPort#resolveStaffId} instead of
   * {@link StaffAssignmentPort#resolveActiveStaffId}, this test WILL FAIL because
   * resolveStaffId returns a UUID for INACTIVE accounts.
   *
   * <p>Scenario: an INACTIVE staff member has an ACTIVE assignment row but MUST be denied.
   */
  @Test
  void inactive_staff_account_raises_403() {
    UUID programId = UUID.randomUUID();

    // INACTIVE staff: resolveActiveStaffId returns empty (WARNING-1 closure)
    when(staffAssignmentPort.resolveActiveStaffId("inactive@uniquindio.edu.co"))
        .thenReturn(Optional.empty());

    // Verify that even if resolveStaffId would return a value (simulating INACTIVE account
    // with active assignment), the service still denies access because it uses resolveActiveStaffId
    when(staffAssignmentPort.resolveStaffId("inactive@uniquindio.edu.co"))
        .thenReturn(Optional.of(UUID.randomUUID())); // would return UUID if inactive

    UploadImportCommand cmd = buildCommand("inactive@uniquindio.edu.co", "STAFF",
        programId, UUID.randomUUID(), null, ImportKind.COURSES, new byte[100]);

    assertThatThrownBy(() -> service.upload(cmd))
        .isInstanceOf(ImportAccessDeniedException.class);

    // The denial must come from resolveActiveStaffId, NOT from isAssigned
    // i.e., isAssigned must NEVER be called if resolveActiveStaffId returns empty
    verify(staffAssignmentPort, never()).isAssigned(any(), any());
    // Audit must not be recorded
    verify(auditRecorder, never()).record(any());
  }

  /**
   * T3.3 SC-001-E: idempotent retry — same key, same hash → returns stored summary.
   */
  @Test
  void idempotent_retry_same_key_same_hash_returns_stored_summary() {
    UUID idempotencyKey = UUID.randomUUID();
    String requestHash = "abc123";
    String storedJson = "{\"id\":\"stored\"}";
    ImportSummary storedSummary = new ImportSummary(UUID.randomUUID(), "COURSES",
        UUID.randomUUID(), null, null, "VALIDATED", 5, 5, List.of(), List.of(), null, 0);

    when(idempotencyStorePort.find(idempotencyKey, UploadImportService.OPERATION))
        .thenReturn(Optional.of(new IdempotencyStorePort.StoredRecord(requestHash, 201, storedJson)));
    when(jsonCodecPort.fromJson(storedJson, ImportSummary.class)).thenReturn(storedSummary);

    UploadImportCommand cmd = new UploadImportCommand(
        idempotencyKey, requestHash, "staff@uniquindio.edu.co", "STAFF", null,
        ImportKind.COURSES, UUID.randomUUID(), UUID.randomUUID(), null,
        new byte[100], "data.xlsx", XLSX_CONTENT_TYPE);

    ImportSummary result = service.upload(cmd);

    assertThat(result).isSameAs(storedSummary);
    // No new batch, no audit on replay
    verify(saveImportBatchPort, never()).save(any());
    verify(auditRecorder, never()).record(any());
  }

  /**
   * T3.3 SC-001-F: idempotent retry — same key, different hash → IdempotencyKeyConflictException (→ 409).
   */
  @Test
  void idempotent_retry_same_key_different_hash_raises_409() {
    UUID idempotencyKey = UUID.randomUUID();
    String originalHash = "hash1";
    String newHash = "hash2";

    when(idempotencyStorePort.find(idempotencyKey, UploadImportService.OPERATION))
        .thenReturn(
            Optional.of(new IdempotencyStorePort.StoredRecord(originalHash, 201, "{}")));

    UploadImportCommand cmd = new UploadImportCommand(
        idempotencyKey, newHash, "staff@uniquindio.edu.co", "STAFF", null,
        ImportKind.COURSES, UUID.randomUUID(), UUID.randomUUID(), null,
        new byte[100], "data.xlsx", XLSX_CONTENT_TYPE);

    assertThatThrownBy(() -> service.upload(cmd))
        .isInstanceOf(IdempotencyKeyConflictException.class);
  }

  /**
   * T3.3 SC-001-G: missing planId for COURSES kind → MissingImportParameterException (→ 400).
   */
  @Test
  void missing_plan_id_for_courses_raises_400() {
    UploadImportCommand cmd = buildCommand("admin@uniquindio.edu.co", "ADMIN",
        UUID.randomUUID(), null, null, // no planId
        ImportKind.COURSES, new byte[100]);

    assertThatThrownBy(() -> service.upload(cmd))
        .isInstanceOf(MissingImportParameterException.class)
        .hasMessageContaining("planId");
  }

  /**
   * T3.3 SC-001-H: missing periodId for OFFERINGS kind → MissingImportParameterException (→ 400).
   */
  @Test
  void missing_period_id_for_offerings_raises_400() {
    UploadImportCommand cmd = buildCommand("admin@uniquindio.edu.co", "ADMIN",
        UUID.randomUUID(), null, null, // no periodId
        ImportKind.OFFERINGS, new byte[100]);

    assertThatThrownBy(() -> service.upload(cmd))
        .isInstanceOf(MissingImportParameterException.class)
        .hasMessageContaining("periodId");
  }

  /**
   * T3.3 SC-001-J: ADMIN bypasses assignment check.
   */
  @Test
  void admin_bypasses_assignment_check() {
    UUID programId = UUID.randomUUID();
    UUID planId = UUID.randomUUID();

    List<ImportRow> rows = List.of(
        new ImportRow(0, Map.of("course_name", "Cálculo I", "credits", "3", "weekly_hours", "4",
            "recommended_semester", "1", "type", "OBLIGATORIA", "journey", "J1")));
    when(xlsxParser.parse(any(), any()))
        .thenReturn(new FileParserPort.ParseResult(rows, List.of()));
    when(jsonCodecPort.toJson(any())).thenReturn("{}");

    UploadImportCommand cmd = buildCommand("admin@uniquindio.edu.co", "ADMIN",
        programId, planId, null, ImportKind.COURSES, new byte[100]);

    service.upload(cmd);

    // ADMIN: neither resolveActiveStaffId nor isAssigned should be called
    verify(staffAssignmentPort, never()).resolveActiveStaffId(any());
    verify(staffAssignmentPort, never()).isAssigned(any(), any());
    // Batch was saved
    verify(saveImportBatchPort).save(any());
  }

  /**
   * T3.3 SC-011-A: audit recorded on successful upload.
   */
  @Test
  void audit_recorded_on_successful_upload() {
    UUID programId = UUID.randomUUID();
    UUID planId = UUID.randomUUID();

    when(staffAssignmentPort.resolveActiveStaffId(anyString()))
        .thenReturn(Optional.of(UUID.randomUUID()));
    when(staffAssignmentPort.isAssigned(any(), any())).thenReturn(true);

    List<ImportRow> rows = List.of(
        new ImportRow(0, Map.of("course_name", "Cálculo I", "credits", "3", "weekly_hours", "4",
            "recommended_semester", "1", "type", "OBLIGATORIA", "journey", "J1")));
    when(xlsxParser.parse(any(), any()))
        .thenReturn(new FileParserPort.ParseResult(rows, List.of()));
    when(jsonCodecPort.toJson(any())).thenReturn("{}");

    UploadImportCommand cmd = buildCommand("staff@uniquindio.edu.co", "STAFF",
        programId, planId, null, ImportKind.COURSES, new byte[100]);

    service.upload(cmd);

    ArgumentCaptor<co.edu.uniquindio.brujula.audit.application.dto.RecordAuditEventCommand> captor =
        ArgumentCaptor.forClass(co.edu.uniquindio.brujula.audit.application.dto.RecordAuditEventCommand.class);
    verify(auditRecorder).record(captor.capture());
    assertThat(captor.getValue().action()).isEqualTo("IMPORT_UPLOADED");
  }

  /**
   * T3.3 SC-011-C: no audit on 403 rejection (staff not assigned).
   */
  @Test
  void no_audit_on_403_rejection() {
    UUID staffId = UUID.randomUUID();
    UUID programId = UUID.randomUUID();

    when(staffAssignmentPort.resolveActiveStaffId("staff@uniquindio.edu.co"))
        .thenReturn(Optional.of(staffId));
    when(staffAssignmentPort.isAssigned(staffId, programId)).thenReturn(false);

    UploadImportCommand cmd = buildCommand("staff@uniquindio.edu.co", "STAFF",
        programId, UUID.randomUUID(), null, ImportKind.COURSES, new byte[100]);

    assertThatThrownBy(() -> service.upload(cmd)).isInstanceOf(ImportAccessDeniedException.class);

    verify(auditRecorder, never()).record(any());
  }

  // ---- helpers ----

  private UploadImportCommand buildCommand(
      String actorEmail,
      String actorRole,
      UUID programId,
      UUID planId,
      UUID periodId,
      ImportKind kind,
      byte[] fileBytes) {
    return buildCommand(actorEmail, actorRole, programId, planId, periodId, kind, fileBytes,
        XLSX_CONTENT_TYPE, "data.xlsx");
  }

  private UploadImportCommand buildCommand(
      String actorEmail,
      String actorRole,
      UUID programId,
      UUID planId,
      UUID periodId,
      ImportKind kind,
      byte[] fileBytes,
      String contentType,
      String filename) {
    return new UploadImportCommand(
        UUID.randomUUID(),
        "requestHash123",
        actorEmail,
        actorRole,
        null, // actorId
        kind,
        programId,
        planId,
        periodId,
        fileBytes,
        filename,
        contentType);
  }
}
