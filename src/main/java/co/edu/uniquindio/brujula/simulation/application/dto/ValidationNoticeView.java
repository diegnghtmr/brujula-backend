package co.edu.uniquindio.brujula.simulation.application.dto;

import java.util.Map;

/**
 * Application-layer view of a validation notice (spec DET-003, STALE-002).
 *
 * <p>Used in {@link SimulationValidationView} to report blocking and warning notices.
 *
 * @param code SIMULATION_* error code
 * @param message human-readable message
 * @param metadata optional context map (may be null)
 */
public record ValidationNoticeView(String code, String message, Map<String, Object> metadata) {}
