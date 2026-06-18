package co.edu.uniquindio.brujula.simulation.infrastructure.adapter.out.persistence;

import co.edu.uniquindio.brujula.simulation.application.port.out.LoadSimulationSelectionPort;
import co.edu.uniquindio.brujula.simulation.application.port.out.SaveSimulationSelectionPort;
import co.edu.uniquindio.brujula.simulation.domain.SimulationSelection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * JPA-backed adapter implementing {@link LoadSimulationSelectionPort} and {@link
 * SaveSimulationSelectionPort}.
 *
 * <p>Package-private.
 */
@Component
@Transactional
class SimulationSelectionPersistenceAdapter
    implements LoadSimulationSelectionPort, SaveSimulationSelectionPort {

  private final SimulationSelectionJpaRepository repository;
  private final SimulationSelectionPersistenceMapper mapper;

  SimulationSelectionPersistenceAdapter(
      SimulationSelectionJpaRepository repository, SimulationSelectionPersistenceMapper mapper) {
    this.repository = repository;
    this.mapper = mapper;
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<SimulationSelection> loadById(UUID selectionId) {
    return repository
        .findById(selectionId)
        .filter(e -> "ACTIVE".equals(e.getStatus()))
        .map(mapper::toDomain);
  }

  @Override
  @Transactional(readOnly = true)
  public List<SimulationSelection> loadActiveBySimulationId(UUID simulationId) {
    return repository.findBySimulationIdAndStatus(simulationId, "ACTIVE").stream()
        .map(mapper::toDomain)
        .toList();
  }

  @Override
  public SimulationSelection save(SimulationSelection selection) {
    SimulationSelectionJpaEntity entity = mapper.toJpaEntity(selection);
    SimulationSelectionJpaEntity saved = repository.save(entity);
    repository.flush();
    return mapper.toDomain(repository.findById(saved.getId()).orElseThrow());
  }
}
