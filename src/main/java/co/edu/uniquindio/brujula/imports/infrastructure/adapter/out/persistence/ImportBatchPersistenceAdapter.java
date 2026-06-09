package co.edu.uniquindio.brujula.imports.infrastructure.adapter.out.persistence;

import co.edu.uniquindio.brujula.imports.application.port.out.LoadImportBatchPort;
import co.edu.uniquindio.brujula.imports.application.port.out.SaveImportBatchPort;
import co.edu.uniquindio.brujula.imports.domain.model.ImportBatch;
import co.edu.uniquindio.brujula.imports.domain.model.ImportBatchId;
import co.edu.uniquindio.brujula.imports.domain.model.ImportKind;
import co.edu.uniquindio.brujula.imports.domain.model.ImportStatus;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Driven adapter: JPA-backed persistence for the imports module — both save and load.
 *
 * <p>Implements {@link SaveImportBatchPort} and {@link LoadImportBatchPort}. Package-private
 * per ARQ-006b.
 *
 * <p>Transaction strategy: class-level {@code @Transactional} (read-write) so write methods
 * inherit a writable transaction; each read method explicitly overrides with
 * {@code @Transactional(readOnly=true)} for performance. Mirrors
 * {@code OfferingPersistenceAdapter}.
 *
 * <p>Save pattern: {@code save → flush → toDomain} so that {@code @Version} increments and
 * constraint violations surface at call time rather than at commit time, giving the service
 * layer a deterministic result.
 */
@Component
@Transactional
class ImportBatchPersistenceAdapter implements SaveImportBatchPort, LoadImportBatchPort {

    private final ImportBatchJpaRepository repository;
    private final ImportBatchPersistenceMapper mapper;

    ImportBatchPersistenceAdapter(ImportBatchJpaRepository repository) {
        this.repository = repository;
        this.mapper = new ImportBatchPersistenceMapper();
    }

    // ---- SaveImportBatchPort ----

    @Override
    public ImportBatch save(ImportBatch batch) {
        ImportBatchJpaEntity entity = mapper.toJpaEntity(batch);
        ImportBatchJpaEntity managed = repository.save(entity);
        repository.flush();
        return mapper.toDomain(managed);
    }

    // ---- LoadImportBatchPort ----

    @Override
    @Transactional(readOnly = true)
    public Optional<ImportBatch> findById(ImportBatchId id) {
        return repository.findById(id.value())
                .map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ImportBatch> findByProgram(UUID programId, ImportKind kind, ImportStatus status) {
        String kindStr = kind != null ? kind.name() : null;
        String statusStr = status != null ? status.name() : null;
        return repository.findByProgram(programId, kindStr, statusStr)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ImportBatch> findAll(ImportKind kind, ImportStatus status) {
        String kindStr = kind != null ? kind.name() : null;
        String statusStr = status != null ? status.name() : null;
        return repository.findAllByFilters(kindStr, statusStr)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }
}
