package co.edu.uniquindio.brujula.simulation.application.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Full detail view of a simulation (spec SIM-003, DET-001..003, STALE-001).
 *
 * <p>Extends the data in {@link SimulationView} with assembled sub-objects computed at read time.
 * Used for GET /me/simulations/{id} and POST /me/simulations/{id}/review responses.
 *
 * <p>ARQ-001: plain String/UUID/primitive types only. No domain entities.
 *
 * @param id simulation UUID
 * @param name simulation name
 * @param periodId period UUID
 * @param periodName human-readable period name (from offering::api)
 * @param programIds list of program UUIDs
 * @param statusName "CURRENT" or "OUTDATED" (computed at read, INV-004)
 * @param createdAt creation timestamp
 * @param updatedAt last-updated timestamp
 * @param version optimistic lock version
 * @param selections list of active selections (full SimulationSelectionView)
 * @param creditSummary one entry per program (DET-002 — includes 0-credit programs)
 * @param weeklySchedule session entries with inClash (DET-001)
 * @param totalWeeklyHours sum of all session durations in weekly hours
 * @param validation blocking + warning notices re-computed at read time (DET-003)
 */
public record SimulationDetailView(
    UUID id,
    String name,
    UUID periodId,
    String periodName,
    List<UUID> programIds,
    String statusName,
    Instant createdAt,
    Instant updatedAt,
    long version,
    List<SimulationSelectionView> selections,
    List<ProgramCreditSummaryView> creditSummary,
    List<WeeklyScheduleEntryView> weeklySchedule,
    Double totalWeeklyHours,
    SimulationValidationView validation) {}
