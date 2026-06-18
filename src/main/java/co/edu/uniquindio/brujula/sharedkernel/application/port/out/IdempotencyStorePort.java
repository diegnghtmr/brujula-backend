package co.edu.uniquindio.brujula.sharedkernel.application.port.out;

import java.util.Optional;
import java.util.UUID;

/**
 * Cross-module output port: opaque-JSON idempotency store.
 *
 * <p>Backed by {@code platform.idempotency_record} (Flyway V005). Generalizes the former
 * administration-scoped port to a domain-type-free contract: each caller owns its own
 * (de)serialization of {@code responseBodyJson}.
 *
 * <p>Dependency rule: this port imports only {@code java.*} — no module domain types (sharedkernel
 * {@code allowedDependencies = {}}).
 */
public interface IdempotencyStorePort {

  /**
   * Check whether the given key + operation pair already has a stored result.
   *
   * @param idempotencyKey the client-supplied idempotency key
   * @param operation the operation identifier (e.g. {@code CREATE_STAFF}, {@code
   *     CREATE_SUBSCRIPTION})
   * @return stored record if the key+operation was seen before, empty otherwise
   */
  Optional<StoredRecord> find(UUID idempotencyKey, String operation);

  /**
   * Persist the idempotency record after a successful first execution.
   *
   * <p>Implementations MUST be idempotent on the storage side: a second call with the same key +
   * operation MUST be a no-op (e.g. {@code ON CONFLICT DO NOTHING}).
   *
   * @param idempotencyKey the client key
   * @param operation the operation identifier
   * @param requestHash SHA-256 of the request body
   * @param responseStatus HTTP status code of the original response
   * @param responseBodyJson JSON-serialized response body for replay
   */
  void save(
      UUID idempotencyKey,
      String operation,
      String requestHash,
      int responseStatus,
      String responseBodyJson);

  /**
   * Stored idempotency record — opaque JSON representation of the original response.
   *
   * @param requestHash SHA-256 of the original request body
   * @param responseStatus HTTP status code of the original response
   * @param responseBodyJson JSON-serialized response body
   */
  record StoredRecord(String requestHash, int responseStatus, String responseBodyJson) {}
}
