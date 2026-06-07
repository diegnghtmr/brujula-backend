package co.edu.uniquindio.brujula.programcatalog.infrastructure.adapter.out.persistence;

import co.edu.uniquindio.brujula.programcatalog.application.port.out.LoadProgramPort;
import co.edu.uniquindio.brujula.programcatalog.domain.AcademicProgram;
import co.edu.uniquindio.brujula.programcatalog.domain.ProgramId;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * JPA-backed adapter implementing {@link LoadProgramPort}.
 *
 * <p>Package-private per ARQ-006. Delegates to {@link AcademicProgramJpaRepository} and
 * {@link CatalogPersistenceMapper} for domain ↔ entity conversion.
 *
 * <p>Satisfies: CAT-001, Phase 1 WU4 persistence adapters.
 */
@Component
class ProgramCatalogPersistenceAdapter implements LoadProgramPort {

  private final AcademicProgramJpaRepository repository;

  ProgramCatalogPersistenceAdapter(AcademicProgramJpaRepository repository) {
    this.repository = repository;
  }

  @Override
  @Transactional(readOnly = true)
  public List<AcademicProgram> loadAll() {
    return repository.findAll().stream()
        .map(CatalogPersistenceMapper::toDomain)
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<AcademicProgram> loadById(ProgramId programId) {
    return repository.findById(programId.value())
        .map(CatalogPersistenceMapper::toDomain);
  }
}
