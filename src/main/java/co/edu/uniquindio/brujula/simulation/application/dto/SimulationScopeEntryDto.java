package co.edu.uniquindio.brujula.simulation.application.dto;

import java.util.UUID;

/**
 * Application-layer DTO carrying one (programId, journeyCode) scope pair.
 *
 * <p>Used in {@link CreateSimulationCommand} and as the scope-entry type in read views ({@link
 * SimulationView}, {@link SimulationDetailView}) so that REST adapters never depend directly on the
 * domain {@code SimulationScopeEntry} value object (ARQ-001).
 */
public record SimulationScopeEntryDto(UUID programId, String journeyCode) {}
