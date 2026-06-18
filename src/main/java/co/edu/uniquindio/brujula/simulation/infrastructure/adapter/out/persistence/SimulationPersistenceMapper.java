package co.edu.uniquindio.brujula.simulation.infrastructure.adapter.out.persistence;

import co.edu.uniquindio.brujula.simulation.domain.Simulation;
import co.edu.uniquindio.brujula.simulation.domain.SimulationId;
import co.edu.uniquindio.brujula.simulation.domain.SimulationLifecycle;
import co.edu.uniquindio.brujula.simulation.domain.SimulationScopeEntry;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Maps between {@link Simulation} domain objects and {@link SimulationJpaEntity}. Package-private.
 */
@Component
class SimulationPersistenceMapper {

  /**
   * Reconstitute a domain {@link Simulation} from a JPA entity plus already-loaded scope rows.
   *
   * @param entity the simulation JPA entity
   * @param scopeRows scope rows loaded from {@code simulation.simulation_scope}
   */
  Simulation toDomain(SimulationJpaEntity entity, List<SimulationScopeJpaEntity> scopeRows) {
    List<UUID> programIds =
        entity.getProgramIds() != null ? Arrays.asList(entity.getProgramIds()) : List.of();
    List<SimulationScopeEntry> scopeEntries =
        scopeRows.stream()
            .map(s -> new SimulationScopeEntry(s.getProgramId(), s.getJourneyCode()))
            .toList();
    return Simulation.reconstitute(
        SimulationId.of(entity.getId()),
        entity.getOwnerId(),
        entity.getName(),
        entity.getPeriodId(),
        programIds,
        scopeEntries,
        SimulationLifecycle.valueOf(entity.getStatus()),
        entity.getCreatedAt(),
        entity.getUpdatedAt(),
        entity.getVersion());
  }

  SimulationJpaEntity toJpaEntity(Simulation simulation) {
    SimulationJpaEntity entity = new SimulationJpaEntity();
    entity.setId(simulation.getId().value());
    entity.setOwnerId(simulation.getOwnerId());
    entity.setName(simulation.getName());
    entity.setPeriodId(simulation.getPeriodId());
    entity.setProgramIds(simulation.getProgramIds().toArray(UUID[]::new));
    entity.setStatus(simulation.getLifecycleStatus().name());
    entity.setCreatedAt(simulation.getCreatedAt());
    entity.setUpdatedAt(simulation.getUpdatedAt());
    entity.setVersion(simulation.getVersion());
    return entity;
  }
}
