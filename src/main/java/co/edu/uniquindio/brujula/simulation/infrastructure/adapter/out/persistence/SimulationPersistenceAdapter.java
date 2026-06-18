package co.edu.uniquindio.brujula.simulation.infrastructure.adapter.out.persistence;

import co.edu.uniquindio.brujula.simulation.application.port.out.LoadSimulationPort;
import co.edu.uniquindio.brujula.simulation.application.port.out.SaveSimulationPort;
import co.edu.uniquindio.brujula.simulation.domain.Simulation;
import co.edu.uniquindio.brujula.simulation.domain.SimulationLifecycle;
import co.edu.uniquindio.brujula.simulation.domain.SimulationScopeEntry;
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
 * <p>Package-private per ARQ-006b.
 */
@Component
@Transactional
class SimulationPersistenceAdapter implements LoadSimulationPort, SaveSimulationPort {

  private final SimulationJpaRepository repository;
  private final SimulationScopeJpaRepository scopeRepository;
  private final SimulationPersistenceMapper mapper;

  SimulationPersistenceAdapter(
      SimulationJpaRepository repository,
      SimulationScopeJpaRepository scopeRepository,
      SimulationPersistenceMapper mapper) {
    this.repository = repository;
    this.scopeRepository = scopeRepository;
    this.mapper = mapper;
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<Simulation> loadById(UUID simulationId) {
    return repository
        .findById(simulationId)
        .map(entity -> mapper.toDomain(entity, scopeRepository.findByIdSimulationId(simulationId)));
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
    return entityPage.stream()
        .map(
            entity -> mapper.toDomain(entity, scopeRepository.findByIdSimulationId(entity.getId())))
        .toList();
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

    // Persist scope entries: delete-then-insert to handle updates cleanly
    UUID simulationId = saved.getId();
    scopeRepository.deleteBySimulationId(simulationId);
    List<SimulationScopeEntry> scopeEntries = simulation.getScopeEntries();
    if (scopeEntries != null && !scopeEntries.isEmpty()) {
      List<SimulationScopeJpaEntity> scopeRows =
          scopeEntries.stream()
              .map(e -> new SimulationScopeJpaEntity(simulationId, e.programId(), e.journeyCode()))
              .toList();
      scopeRepository.saveAll(scopeRows);
      scopeRepository.flush();
    }

    List<SimulationScopeJpaEntity> persistedScope =
        scopeRepository.findByIdSimulationId(simulationId);
    return mapper.toDomain(repository.findById(simulationId).orElseThrow(), persistedScope);
  }
}
