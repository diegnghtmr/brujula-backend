package co.edu.uniquindio.brujula.simulation.application.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Read-side view of a simulation for list/create/update/duplicate responses.
 *
 * <p>{@code status} is the contract-level {@code SimulationStatus} string (CURRENT/OUTDATED),
 * computed at read time — never stored (INV-004).
 *
 * <p>{@code programIds} is the backward-compatible derived list of distinct program UUIDs. {@code
 * scopeEntries} carries the full (programId, journeyCode) scope.
 *
 * <p>All fields use plain String/UUID (ARQ-001).
 */
public record SimulationView(
    UUID id,
    String name,
    UUID periodId,
    String periodName,
    List<UUID> programIds,
    List<SimulationScopeEntryDto> scopeEntries,
    String status,
    Instant createdAt,
    Instant updatedAt,
    long version) {}
