package co.edu.uniquindio.brujula.simulation.infrastructure.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * JPA entity mapping to {@code simulation.simulation_scope} (created by Flyway V024).
 *
 * <p>Package-private per ARQ-006b. Composite PK: (simulationId, programId, journeyCode).
 */
@Entity
@Table(name = "simulation_scope", schema = "simulation")
class SimulationScopeJpaEntity {

  @EmbeddedId private SimulationScopePk id;

  // JPA requires no-arg constructor
  protected SimulationScopeJpaEntity() {}

  SimulationScopeJpaEntity(UUID simulationId, UUID programId, String journeyCode) {
    this.id = new SimulationScopePk(simulationId, programId, journeyCode);
  }

  UUID getSimulationId() {
    return id.simulationId;
  }

  UUID getProgramId() {
    return id.programId;
  }

  String getJourneyCode() {
    return id.journeyCode;
  }

  /** Composite PK for {@link SimulationScopeJpaEntity}. */
  @Embeddable
  static class SimulationScopePk implements Serializable {

    @Column(name = "simulation_id", nullable = false)
    private UUID simulationId;

    @Column(name = "program_id", nullable = false)
    private UUID programId;

    @Column(name = "journey_code", nullable = false)
    private String journeyCode;

    protected SimulationScopePk() {}

    SimulationScopePk(UUID simulationId, UUID programId, String journeyCode) {
      this.simulationId = simulationId;
      this.programId = programId;
      this.journeyCode = journeyCode;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof SimulationScopePk that)) return false;
      return Objects.equals(simulationId, that.simulationId)
          && Objects.equals(programId, that.programId)
          && Objects.equals(journeyCode, that.journeyCode);
    }

    @Override
    public int hashCode() {
      return Objects.hash(simulationId, programId, journeyCode);
    }
  }
}
