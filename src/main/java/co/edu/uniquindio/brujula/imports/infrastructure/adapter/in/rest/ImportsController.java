package co.edu.uniquindio.brujula.imports.infrastructure.adapter.in.rest;

import co.edu.uniquindio.brujula.generated.api.ImportsApi;
import co.edu.uniquindio.brujula.generated.model.ImportIssue;
import co.edu.uniquindio.brujula.generated.model.ImportKind;
import co.edu.uniquindio.brujula.generated.model.ImportPage;
import co.edu.uniquindio.brujula.generated.model.ImportStatus;
import co.edu.uniquindio.brujula.generated.model.ImportSummary;
import co.edu.uniquindio.brujula.generated.model.VersionedAction;
import co.edu.uniquindio.brujula.identity.application.port.in.GetCurrentUserQuery;
import co.edu.uniquindio.brujula.imports.application.dto.ImportPageView;
import co.edu.uniquindio.brujula.imports.application.dto.ListImportsQuery;
import co.edu.uniquindio.brujula.imports.application.dto.UploadImportCommand;
import co.edu.uniquindio.brujula.imports.application.dto.ConfirmImportCommand;
import co.edu.uniquindio.brujula.imports.application.port.in.ConfirmImportUseCase;
import co.edu.uniquindio.brujula.imports.application.port.in.GetImportUseCase;
import co.edu.uniquindio.brujula.imports.application.port.in.ListImportsUseCase;
import co.edu.uniquindio.brujula.imports.application.port.in.UploadImportUseCase;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

/**
 * REST adapter: imports module endpoints ({@code /staff/imports}).
 *
 * <p>Implements the generated {@link ImportsApi} contract-first interface (ARQ-001).
 * Package-private per ARQ-006b.
 *
 * <p>WU4 scope:
 * <ul>
 *   <li>POST /staff/imports — multipart upload (IMP-001)
 *   <li>GET /staff/imports — list with program-scoping (IMP-003)
 *   <li>GET /staff/imports/{importId} — get single batch (IMP-004)
 *   <li>POST /staff/imports/{importId}/confirmation — stub (WU6)
 * </ul>
 *
 * <p>ARQ-001: uses only generated.model.* + application.dto.* at the REST boundary — no domain
 * types leak through.
 */
@RestController
class ImportsController implements ImportsApi {

  private final UploadImportUseCase uploadImportUseCase;
  private final ListImportsUseCase listImportsUseCase;
  private final GetImportUseCase getImportUseCase;
  private final ConfirmImportUseCase confirmImportUseCase;
  private final GetCurrentUserQuery getCurrentUserQuery;
  private final ObjectMapper objectMapper;

  ImportsController(
      UploadImportUseCase uploadImportUseCase,
      ListImportsUseCase listImportsUseCase,
      GetImportUseCase getImportUseCase,
      ConfirmImportUseCase confirmImportUseCase,
      GetCurrentUserQuery getCurrentUserQuery,
      ObjectMapper objectMapper) {
    this.uploadImportUseCase = uploadImportUseCase;
    this.listImportsUseCase = listImportsUseCase;
    this.getImportUseCase = getImportUseCase;
    this.confirmImportUseCase = confirmImportUseCase;
    this.getCurrentUserQuery = getCurrentUserQuery;
    this.objectMapper = objectMapper;
  }

  // -------------------------------------------------------------------------
  // POST /staff/imports — upload import file (IMP-001)
  // -------------------------------------------------------------------------

  @Override
  @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
  public ResponseEntity<ImportSummary> uploadImport(
      UUID idempotencyKey,
      String X_XSRF_TOKEN,
      ImportKind kind,
      UUID programId,
      MultipartFile file,
      @Nullable UUID planId,
      @Nullable UUID periodId) {

    var profile = resolveCurrentUserProfile();
    String requestHash = hashMultipartRequest(kind, programId, planId, periodId, file);

    byte[] fileBytes;
    try {
      fileBytes = file.getBytes();
    } catch (IOException e) {
      throw new IllegalStateException("Failed to read uploaded file bytes", e);
    }

    UploadImportCommand command = new UploadImportCommand(
        idempotencyKey,
        requestHash,
        profile.email(),
        profile.role(),
        profile.id(),
        kind.getValue(),
        programId,
        planId,
        periodId,
        fileBytes,
        file.getOriginalFilename() != null ? file.getOriginalFilename() : "",
        file.getContentType() != null ? file.getContentType() : "application/octet-stream");

    co.edu.uniquindio.brujula.imports.application.dto.ImportSummary appSummary =
        uploadImportUseCase.upload(command);

    ImportSummary response = toContractSummary(appSummary);
    URI location = URI.create("/staff/imports/" + appSummary.id());
    return ResponseEntity.status(HttpStatus.CREATED)
        .location(location)
        .body(response);
  }

  // -------------------------------------------------------------------------
  // GET /staff/imports — list imports (IMP-003)
  // -------------------------------------------------------------------------

  @Override
  @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
  public ResponseEntity<ImportPage> listImports(
      @Nullable ImportKind kind,
      @Nullable ImportStatus status,
      Integer page,
      Integer size,
      @Nullable List<String> sort) {

    var profile = resolveCurrentUserProfile();
    ListImportsQuery query = new ListImportsQuery(
        profile.email(),
        profile.role(),
        kind != null ? kind.name() : null,
        status != null ? status.name() : null,
        page != null ? page : 0,
        size != null ? size : 20);

    ImportPageView pageView = listImportsUseCase.list(query);
    ImportPage response = toContractPage(pageView);
    return ResponseEntity.ok(response);
  }

  // -------------------------------------------------------------------------
  // GET /staff/imports/{importId} — get single import (IMP-004)
  // -------------------------------------------------------------------------

  @Override
  @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
  public ResponseEntity<ImportSummary> getImport(UUID importId) {
    var profile = resolveCurrentUserProfile();
    co.edu.uniquindio.brujula.imports.application.dto.ImportSummary appSummary =
        getImportUseCase.get(importId, profile.email(), profile.role());
    return ResponseEntity.ok(toContractSummary(appSummary));
  }

  // -------------------------------------------------------------------------
  // POST /staff/imports/{importId}/confirmation — confirm (WU6, IMP-005)
  // -------------------------------------------------------------------------

  @Override
  @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
  public ResponseEntity<ImportSummary> confirmImport(
      UUID idempotencyKey,
      String X_XSRF_TOKEN,
      UUID importId,
      VersionedAction versionedAction) {

    var profile = resolveCurrentUserProfile();
    long expectedVersion = versionedAction != null && versionedAction.getVersion() != null
        ? versionedAction.getVersion()
        : 0L;
    String requestHash = hashConfirmRequest(importId, expectedVersion);

    ConfirmImportCommand command = new ConfirmImportCommand(
        importId,
        profile.email(),
        profile.role(),
        profile.id(),
        expectedVersion,
        idempotencyKey,
        requestHash);

    co.edu.uniquindio.brujula.imports.application.dto.ImportSummary appSummary =
        confirmImportUseCase.confirm(command);

    return ResponseEntity.ok(toContractSummary(appSummary));
  }

  // -------------------------------------------------------------------------
  // Private helpers — ARQ-001 compliant (application DTOs / generated model only)
  // -------------------------------------------------------------------------

  /** Resolve current user profile from the Spring Security context. */
  private co.edu.uniquindio.brujula.identity.application.dto.UserProfile resolveCurrentUserProfile() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || auth.getName() == null) {
      throw new IllegalStateException("No authenticated principal found in SecurityContext");
    }
    return getCurrentUserQuery.getCurrentUserBySubject(auth.getName());
  }

  /**
   * Compute a deterministic hash for a confirm request (importId + expectedVersion).
   * Used for idempotency conflict detection.
   */
  private String hashConfirmRequest(UUID importId, long expectedVersion) {
    try {
      String repr = "CONFIRM:" + importId + ":" + expectedVersion;
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(repr.getBytes(java.nio.charset.StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hash);
    } catch (Exception e) {
      throw new IllegalStateException("Failed to hash confirm request", e);
    }
  }

  /**
   * Compute a SHA-256 hash of the multipart request parameters (excluding the file binary itself
   * to avoid reading the stream twice). Mirrors OfferingController#hashRequest.
   */
  private String hashMultipartRequest(
      ImportKind kind, UUID programId, @Nullable UUID planId,
      @Nullable UUID periodId, MultipartFile file) {
    try {
      // Hash key parameters + filename + content-type (deterministic representation)
      String repr = kind.getValue() + "|" + programId
          + "|" + (planId != null ? planId : "")
          + "|" + (periodId != null ? periodId : "")
          + "|" + (file.getOriginalFilename() != null ? file.getOriginalFilename() : "")
          + "|" + (file.getContentType() != null ? file.getContentType() : "");
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(repr.getBytes(java.nio.charset.StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hash);
    } catch (Exception e) {
      throw new IllegalStateException("Failed to hash multipart request", e);
    }
  }

  /** Map application-layer ImportSummary DTO to generated ImportSummary model. */
  private ImportSummary toContractSummary(
      co.edu.uniquindio.brujula.imports.application.dto.ImportSummary app) {
    ImportSummary summary = new ImportSummary();
    summary.setId(app.id());
    summary.setKind(ImportKind.fromValue(app.kind()));
    summary.setProgramId(app.programId());
    summary.setPlanId(app.planId());
    summary.setPeriodId(app.periodId());
    summary.setStatus(ImportStatus.fromValue(app.status()));
    summary.setTotalRows(app.totalRows());
    summary.setValidRows(app.validRows());
    summary.setErrors(toContractIssues(app.errors()));
    summary.setWarnings(toContractIssues(app.warnings()));
    summary.setCreatedAt(OffsetDateTime.ofInstant(app.createdAt(), ZoneOffset.UTC));
    summary.setVersion(app.version());
    return summary;
  }

  private List<ImportIssue> toContractIssues(
      List<co.edu.uniquindio.brujula.imports.application.dto.ImportSummary.ImportIssueDto> dtos) {
    return dtos.stream()
        .map(dto -> {
          ImportIssue issue = new ImportIssue(dto.row(), dto.code(), dto.message());
          issue.setColumn(dto.column());
          return issue;
        })
        .toList();
  }

  private ImportPage toContractPage(ImportPageView view) {
    ImportPage page = new ImportPage();
    page.setPage(view.page());
    page.setSize(view.size());
    page.setTotalElements(view.totalElements());
    page.setTotalPages(view.totalPages());
    page.setContent(view.content().stream().map(this::toContractSummary).toList());
    return page;
  }
}
