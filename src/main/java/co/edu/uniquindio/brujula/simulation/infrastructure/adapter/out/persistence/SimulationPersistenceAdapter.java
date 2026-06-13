package co.edu.uniquindio.brujula.simulation.infrastructure.adapter.out.persistence;

import co.edu.uniquindio.brujula.simulation.application.port.out.LoadSimulationPort;
import co.edu.uniquindio.brujula.simulation.application.port.out.SaveSimulationPort;
import co.edu.uniquindio.brujula.simulation.domain.Simulation;
import co.edu.uniquindio.brujula.simulation.domain.SimulationLifecycle;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * JPA-backed adapter implementing {@link LoadSimulationPort} and {@link SaveSimulationPort}.
 *
 * <p>Package-private.
 */
@Component
@Transactional
class SimulationPersistenceAdapter implements LoadSimulationPort, SaveSimulationPort {

  private final SimulationJpaRepository repository;
  private final SimulationPersistenceMapper mapper;

  SimulationPersistenceAdapter(
      SimulationJpaRepository repository, SimulationPersistenceMapper mapper) {
    this.repository = repository;
    this.mapper = mapper;
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<Simulation> loadById(UUID simulationId) {
    return repository.findById(simulationId).map(mapper::toDomain);
  }

  @Override
  @Transactional(readOnly = true)
  public List<Simulation> loadByOwner(UUID ownerId, UUID periodId, int page, int size) {
    Pageable pageable = PageRequest.of(page, size);
    Page<SimulationJpaEntity> entityPage;
    if (periodId != null) {
      entityPage =
          repository.findByOwnerIdAndStatusAndPeriodId(
              ownerId, SimulationLifecycle.ACTIVE.name(), periodId, pageable);
    } else {
      entityPage =
          repository.findByOwnerIdAndStatus(ownerId, SimulationLifecycle.ACTIVE.name(), pageable);
    }
    return entityPage.stream().map(mapper::toDomain).toList();
  }

  @Override
  @Transactional(readOnly = true)
  public long countByOwner(UUID ownerId, UUID periodId) {
    if (periodId != null) {
      return repository.countByOwnerIdAndStatusAndPeriodId(
          ownerId, SimulationLifecycle.ACTIVE.name(), periodId);
    }
    return repository.countByOwnerIdAndStatus(ownerId, SimulationLifecycle.ACTIVE.name());
  }

  @Override
  public Simulation save(Simulation simulation) {
    SimulationJpaEntity entity = mapper.toJpaEntity(simulation);
    SimulationJpaEntity saved = repository.save(entity);
    // Flush so @Version is incremented before we read it back
    repository.flush();
    return mapper.toDomain(repository.findById(saved.getId()).orElseThrow());
  }
}
