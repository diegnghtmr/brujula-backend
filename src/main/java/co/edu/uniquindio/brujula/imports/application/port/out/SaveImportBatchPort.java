package co.edu.uniquindio.brujula.imports.application.port.out;

import co.edu.uniquindio.brujula.imports.domain.model.ImportBatch;

/**
 * Output port: persist an ImportBatch aggregate.
 *
 * <p>The implementation must flush the entity after save so that {@code @Version} increments
 * and constraint violations surface deterministically at call time (mirrors SaveOfferingPort
 * pattern: save → flush → toDomain).
 */
public interface SaveImportBatchPort {

    /**
     * Persist the given ImportBatch (insert or update) and return the managed snapshot.
     *
     * <p>The returned instance reflects the post-flush state: {@code version} is incremented
     * by 1 on update (optimistic locking via {@code @Version}).
     *
     * @param batch the aggregate to persist
     * @return the persisted, post-flush domain instance
     */
    ImportBatch save(ImportBatch batch);
}
