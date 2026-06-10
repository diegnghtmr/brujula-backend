package co.edu.uniquindio.brujula.simulation.infrastructure.adapter.out.persistence;

import co.edu.uniquindio.brujula.simulation.domain.Simulation;
import co.edu.uniquindio.brujula.simulation.domain.SimulationId;
import co.edu.uniquindio.brujula.simulation.domain.SimulationLifecycle;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Maps between {@link Simulation} domain objects and {@link SimulationJpaEntity}. Package-private.
 */
@Component
class SimulationPersistenceMapper {

  Simulation toDomain(SimulationJpaEntity entity) {
    List<UUID> programIds =
        entity.getProgramIds() != null ? Arrays.asList(entity.getProgramIds()) : List.of();
    return Simulation.reconstitute(
        SimulationId.of(entity.getId()),
        entity.getOwnerId(),
        entity.getName(),
        entity.getPeriodId(),
        programIds,
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
