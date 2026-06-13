package co.edu.uniquindio.brujula.sharedkernel.infrastructure.adapter.out.persistence;

import co.edu.uniquindio.brujula.sharedkernel.application.port.out.IdempotencyStorePort;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * JDBC-based adapter for idempotency record persistence ({@code platform.idempotency_record},
 * V005).
 *
 * <p>Stores and retrieves opaque JSON response bodies. No module-domain coupling: callers own their
 * own (de)serialization. This is the ONLY idempotency adapter platform-wide (replaces the former
 * administration-scoped adapter).
 *
 * <p>Package-private. Design: ADR-F3-001.
 */
@Component
class IdempotencyPersistenceAdapter implements IdempotencyStorePort {

  private final JdbcClient jdbcClient;

  IdempotencyPersistenceAdapter(JdbcClient jdbcClient) {
    this.jdbcClient = jdbcClient;
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<StoredRecord> find(UUID idempotencyKey, String operation) {
    return jdbcClient
        .sql(
            """
            SELECT request_hash, response_status, response_body
              FROM platform.idempotency_record
             WHERE idempotency_key = :key
               AND operation = :op
            """)
        .param("key", idempotencyKey)
        .param("op", operation)
        .query(
            (rs, rowNum) ->
                new StoredRecord(
                    rs.getString("request_hash"),
                    rs.getInt("response_status"),
                    rs.getString("response_body")))
        .optional();
  }

  @Override
  @Transactional
  public void save(
      UUID idempotencyKey,
      String operation,
      String requestHash,
      int responseStatus,
      String responseBodyJson) {
    jdbcClient
        .sql(
            """
            INSERT INTO platform.idempotency_record
              (idempotency_key, operation, request_hash, response_status, response_body)
            VALUES (:key, :op, :hash, :status, :body::jsonb)
            ON CONFLICT (idempotency_key, operation) DO NOTHING
            """)
        .param("key", idempotencyKey)
        .param("op", operation)
        .param("hash", requestHash)
        .param("status", responseStatus)
        .param("body", responseBodyJson)
        .update();
  }
}
