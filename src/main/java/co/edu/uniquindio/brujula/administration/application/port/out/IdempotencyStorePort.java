package co.edu.uniquindio.brujula.administration.application.port.out;

import co.edu.uniquindio.brujula.administration.domain.StaffAccount;
import java.util.Optional;
import java.util.UUID;

/**
 * Output port: idempotency store for the administration module's create operations.
 *
 * <p>Backed by {@code platform.idempotency_record} (Flyway V005). Satisfies: Design §4, Spec
 * §Idempotent Create (TRD §13, ADR-009).
 */
public interface IdempotencyStorePort {

  /**
   * Check whether the given key+operation pair already has a stored result.
   *
   * @param idempotencyKey the client-supplied key
   * @param operation the operation identifier (e.g. CREATE_STAFF)
   * @return optional result with the stored hash and staff account, or empty if not seen before
   */
  Optional<StoredResult> findByKeyAndOperation(UUID idempotencyKey, String operation);

  /**
   * Persist the idempotency record after a successful first execution.
   *
   * @param idempotencyKey the client key
   * @param operation the operation identifier
   * @param requestHash SHA-256 of the request body
   * @param result the created staff account
   */
  void save(UUID idempotencyKey, String operation, String requestHash, StaffAccount result);

  /** Stored idempotency result. */
  record StoredResult(String requestHash, StaffAccount result) {}
}
