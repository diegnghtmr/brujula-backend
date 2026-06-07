package co.edu.uniquindio.brujula.curriculum.infrastructure.adapter.out.persistence;

import co.edu.uniquindio.brujula.curriculum.application.port.out.LoadDegreeWorkModalityPort;
import co.edu.uniquindio.brujula.curriculum.domain.DegreeWorkModality;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * JPA-backed adapter implementing {@link LoadDegreeWorkModalityPort}.
 *
 * <p>Package-private per ARQ-006. Satisfies: CUR-006, Phase 2 WU4 persistence adapters.
 */
@Component
class DegreeWorkModalityPersistenceAdapter implements LoadDegreeWorkModalityPort {

  private final DegreeWorkModalityJpaRepository repository;

  DegreeWorkModalityPersistenceAdapter(DegreeWorkModalityJpaRepository repository) {
    this.repository = repository;
  }

  @Override
  @Transactional(readOnly = true)
  public List<DegreeWorkModality> loadByPlanId(UUID planId) {
    return repository.findByPlanId(planId).stream()
        .map(CurriculumPersistenceMapper::toDomain)
        .toList();
  }
}
