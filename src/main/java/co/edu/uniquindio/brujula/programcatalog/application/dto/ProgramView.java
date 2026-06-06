package co.edu.uniquindio.brujula.programcatalog.application.dto;

import java.util.UUID;

/**
 * Application-layer DTO for an AcademicProgram read view.
 *
 * <p>Returned by query services to REST adapters (ARQ-001 — controllers do not touch domain).
 * No Spring/framework imports.
 *
 * @param id the program UUID
 * @param name the program name
 * @param faculty the faculty owning this program
 * @param status the program status string (ACTIVE, INACTIVE, ARCHIVED)
 * @param version optimistic lock version (DT-014)
 */
public record ProgramView(
    UUID id,
    String name,
    String faculty,
    String status,
    long version) {}
