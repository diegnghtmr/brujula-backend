package co.edu.uniquindio.brujula.simulation.application.dto;

import java.util.UUID;

/**
 * Application-layer view of a program credit summary for {@link SimulationDetailView} (spec
 * DET-002).
 *
 * <p>One entry per programId declared in the simulation. selectedCredits may be 0 for programs with
 * no active selections.
 *
 * <p>Plain String/UUID/primitive types only. No domain entities.
 *
 * @param programId program UUID
 * @param programName resolved program name (from ProgramNameReadPort — REAL, not placeholder)
 * @param selectedCredits total credits selected for this program (0.0 if none)
 * @param creditLimit configured maximum credits (from CreditLimitPort — REAL, default 23)
 * @param withinLimit true iff selectedCredits <= creditLimit
 */
public record ProgramCreditSummaryView(
    UUID programId,
    String programName,
    double selectedCredits,
    int creditLimit,
    boolean withinLimit) {}
