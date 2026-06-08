package co.edu.uniquindio.brujula.simulation.application.dto;

import java.util.List;

/** Paginated list of {@link SimulationView} items (SIM-002). */
public record SimulationPageView(
    List<SimulationView> content,
    int page,
    int size,
    long totalElements,
    int totalPages) {}
