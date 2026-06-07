package co.edu.uniquindio.brujula.programcatalog.infrastructure.adapter.out.persistence;

import co.edu.uniquindio.brujula.programcatalog.application.port.out.LoadStudyPlanPort;
import co.edu.uniquindio.brujula.programcatalog.domain.PlanId;
import co.edu.uniquindio.brujula.programcatalog.domain.StudyPlan;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * JPA-backed adapter implementing {@link LoadStudyPlanPort}.
 *
 * <p>Package-private per ARQ-006. Satisfies: CAT-002, Phase 1 WU4 persistence adapters.
 */
@Component
class StudyPlanPersistenceAdapter implements LoadStudyPlanPort {

  private final StudyPlanJpaRepository repository;

  StudyPlanPersistenceAdapter(StudyPlanJpaRepository repository) {
    this.repository = repository;
  }

  @Override
  @Transactional(readOnly = true)
  public List<StudyPlan> loadByProgramId(UUID programId) {
    return repository.findByProgramId(programId).stream()
        .map(CatalogPersistenceMapper::toDomain)
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<StudyPlan> loadById(PlanId planId) {
    return repository.findById(planId.value())
        .map(CatalogPersistenceMapper::toDomain);
  }
}
