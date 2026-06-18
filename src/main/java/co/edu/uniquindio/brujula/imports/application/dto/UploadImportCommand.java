package co.edu.uniquindio.brujula.imports.application.dto;

import java.util.Objects;
import java.util.UUID;

/**
 * Command DTO for the UploadImport use case.
 *
 * <p>Carries all inputs needed by {@link
 * co.edu.uniquindio.brujula.imports.application.service.UploadImportService}: actor identity,
 * idempotency key, file content, and target program/plan/period references.
 *
 * <p>Application DTO — NOT the generated REST model (the REST adapter maps from MultipartFile +
 * request params to this command). No Spring/JPA imports.
 */
public record UploadImportCommand(
    /** Idempotency key from the Idempotency-Key header. */
    UUID idempotencyKey,
    /** SHA-256 hash of the request body for idempotency conflict detection. */
    String requestHash,
    /** Actor email (from OIDC principal or security context). */
    String actorEmail,
    /**
     * Actor role: "STAFF" or "ADMIN". ADMIN bypasses the program assignment check; STAFF is subject
     * to it.
     */
    String actorRole,
    /**
     * Actor UUID (for audit recording). May be null if the controller cannot resolve it cheaply.
     */
    UUID actorId,
    /**
     * Import kind name: "COURSES", "REQUIREMENTS", or "OFFERINGS". Converted to domain enum by the
     * service.
     */
    String kind,
    /** Target program UUID. */
    UUID programId,
    /**
     * Target study plan UUID. Required when kind is COURSES or REQUIREMENTS; null for OFFERINGS.
     */
    UUID planId,
    /**
     * Target academic period UUID. Required when kind is OFFERINGS; null for COURSES/REQUIREMENTS.
     */
    UUID periodId,
    /** Raw file bytes. */
    byte[] fileBytes,
    /** Original filename (used for content-type detection by extension). */
    String filename,
    /** Content-type declared by the client (e.g. application/vnd.openxmlformats-...). */
    String contentType) {

  public UploadImportCommand {
    Objects.requireNonNull(idempotencyKey, "idempotencyKey must not be null");
    Objects.requireNonNull(requestHash, "requestHash must not be null");
    Objects.requireNonNull(actorEmail, "actorEmail must not be null");
    Objects.requireNonNull(actorRole, "actorRole must not be null");
    Objects.requireNonNull(kind, "kind must not be null");
    Objects.requireNonNull(programId, "programId must not be null");
    Objects.requireNonNull(fileBytes, "fileBytes must not be null");
    Objects.requireNonNull(filename, "filename must not be null");
    Objects.requireNonNull(contentType, "contentType must not be null");
  }
}
