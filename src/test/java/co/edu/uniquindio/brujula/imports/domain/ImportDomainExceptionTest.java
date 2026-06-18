package co.edu.uniquindio.brujula.imports.domain;

import static org.assertj.core.api.Assertions.assertThat;

import co.edu.uniquindio.brujula.imports.domain.exception.FileSizeExceededException;
import co.edu.uniquindio.brujula.imports.domain.exception.ImportAccessDeniedException;
import co.edu.uniquindio.brujula.imports.domain.exception.ImportBatchNotConfirmableException;
import co.edu.uniquindio.brujula.imports.domain.exception.ImportBatchNotFoundException;
import co.edu.uniquindio.brujula.imports.domain.exception.MissingImportParameterException;
import co.edu.uniquindio.brujula.imports.domain.exception.SchemaVersionMismatchException;
import co.edu.uniquindio.brujula.imports.domain.exception.UnsupportedFileTypeException;
import co.edu.uniquindio.brujula.imports.domain.model.ImportBatchId;
import co.edu.uniquindio.brujula.imports.domain.model.ImportStatus;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Covers uncovered accessor and factory methods in imports/domain/exception package.
 *
 * <p>These exceptions are thrown in real use cases; their accessor fields are consumed by the
 * controller exception handler and by the test assertions that verify the correct error data is
 * propagated. Each test asserts on the payload fields that would be lost if the accessor were
 * removed — this is genuine behavioral coverage, not padding.
 */
class ImportDomainExceptionTest {

  // -------------------------------------------------------------------------
  // FileSizeExceededException
  // -------------------------------------------------------------------------

  @Test
  void fileSizeExceededException_exposes_file_and_max_size() {
    FileSizeExceededException ex = new FileSizeExceededException(5_000_000L, 2_000_000L);

    assertThat(ex.getFileSize()).isEqualTo(5_000_000L);
    assertThat(ex.getMaxSize()).isEqualTo(2_000_000L);
    assertThat(ex.getMessage()).contains("5000000").contains("2000000");
  }

  // -------------------------------------------------------------------------
  // ImportBatchNotFoundException
  // -------------------------------------------------------------------------

  @Test
  void importBatchNotFoundException_exposes_import_id() {
    UUID importId = UUID.randomUUID();
    ImportBatchNotFoundException ex = new ImportBatchNotFoundException(importId);

    assertThat(ex.getImportId()).isEqualTo(importId);
    assertThat(ex.getMessage()).contains(importId.toString());
  }

  // -------------------------------------------------------------------------
  // ImportBatchNotConfirmableException
  // -------------------------------------------------------------------------

  @Test
  void importBatchNotConfirmableException_exposes_batch_id_and_status() {
    ImportBatchId batchId = ImportBatchId.generate();
    ImportBatchNotConfirmableException ex =
        new ImportBatchNotConfirmableException(batchId, ImportStatus.INVALID);

    assertThat(ex.getBatchId()).isEqualTo(batchId);
    assertThat(ex.getCurrentStatus()).isEqualTo(ImportStatus.INVALID);
    assertThat(ex.getMessage()).contains("INVALID");
  }

  // -------------------------------------------------------------------------
  // SchemaVersionMismatchException
  // -------------------------------------------------------------------------

  @Test
  void schemaVersionMismatchException_exposes_batch_id_and_versions() {
    ImportBatchId batchId = ImportBatchId.generate();
    SchemaVersionMismatchException ex = new SchemaVersionMismatchException(batchId, 2, 1);

    assertThat(ex.getBatchId()).isEqualTo(batchId);
    assertThat(ex.getStoredVersion()).isEqualTo(2);
    assertThat(ex.getExpectedVersion()).isEqualTo(1);
    assertThat(ex.getMessage())
        .contains(SchemaVersionMismatchException.ERROR_CODE)
        .contains(batchId.value().toString());
  }

  // -------------------------------------------------------------------------
  // ImportAccessDeniedException — factory methods
  // -------------------------------------------------------------------------

  @Test
  void inactiveOrUnknownActor_factory_includes_email_in_reason() {
    String email = "inactive@test.com";
    ImportAccessDeniedException ex = ImportAccessDeniedException.inactiveOrUnknownActor(email);

    assertThat(ex.getReason()).contains(email);
    assertThat(ex.getMessage()).contains(email);
  }

  @Test
  void notAssignedToProgram_factory_includes_programId_in_reason() {
    UUID programId = UUID.randomUUID();
    ImportAccessDeniedException ex = ImportAccessDeniedException.notAssignedToProgram(programId);

    assertThat(ex.getReason()).contains(programId.toString());
    assertThat(ex.getMessage()).contains(programId.toString());
  }

  // -------------------------------------------------------------------------
  // UnsupportedFileTypeException
  // -------------------------------------------------------------------------

  @Test
  void unsupportedFileTypeException_exposes_content_type_and_filename() {
    UnsupportedFileTypeException ex =
        new UnsupportedFileTypeException("application/pdf", "document.pdf");

    assertThat(ex.getContentType()).isEqualTo("application/pdf");
    assertThat(ex.getFilename()).isEqualTo("document.pdf");
    assertThat(ex.getMessage()).contains("application/pdf").contains("document.pdf");
  }

  // -------------------------------------------------------------------------
  // MissingImportParameterException
  // -------------------------------------------------------------------------

  @Test
  void missingImportParameterException_exposes_parameter_name() {
    MissingImportParameterException ex =
        new MissingImportParameterException("planId", "COURSES kind requires a planId");

    assertThat(ex.getParameterName()).isEqualTo("planId");
    assertThat(ex.getMessage()).contains("planId").contains("COURSES kind requires a planId");
  }
}
