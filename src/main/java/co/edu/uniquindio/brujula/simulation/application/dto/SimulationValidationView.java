package co.edu.uniquindio.brujula.simulation.application.dto;

import java.util.List;

/**
 * Application-layer view of the read-time validation snapshot (spec DET-003).
 *
 * <p>Re-computed at read time — does NOT mutate stored simulation state.
 *
 * @param blocking list of blocking validation notices (empty if none)
 * @param warnings list of non-blocking advisory notices (empty if none)
 */
public record SimulationValidationView(
    List<ValidationNoticeView> blocking,
    List<ValidationNoticeView> warnings) {}
