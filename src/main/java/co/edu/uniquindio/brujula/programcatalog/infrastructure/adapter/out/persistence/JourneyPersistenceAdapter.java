package co.edu.uniquindio.brujula.programcatalog.infrastructure.adapter.out.persistence;

import co.edu.uniquindio.brujula.programcatalog.application.port.out.LoadJourneyPort;
import co.edu.uniquindio.brujula.programcatalog.domain.Journey;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * JPA-backed adapter implementing {@link LoadJourneyPort}.
 *
 * <p>Package-private.
 */
@Component
class JourneyPersistenceAdapter implements LoadJourneyPort {

  private final JourneyJpaRepository repository;

  JourneyPersistenceAdapter(JourneyJpaRepository repository) {
    this.repository = repository;
  }

  @Override
  @Transactional(readOnly = true)
  public List<Journey> loadAllActive() {
    return repository.findByActiveTrue().stream().map(CatalogPersistenceMapper::toDomain).toList();
  }
}
