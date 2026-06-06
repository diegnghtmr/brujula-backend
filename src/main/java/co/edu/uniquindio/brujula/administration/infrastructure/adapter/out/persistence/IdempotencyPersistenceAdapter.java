package co.edu.uniquindio.brujula.administration.infrastructure.adapter.out.persistence;

import co.edu.uniquindio.brujula.administration.application.port.out.IdempotencyStorePort;
import co.edu.uniquindio.brujula.administration.domain.StaffAccount;
import co.edu.uniquindio.brujula.administration.domain.StaffAccountStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * JDBC-based adapter for idempotency record persistence ({@code platform.idempotency_record},
 * V005).
 *
 * <p>Stores and retrieves {@link StaffAccount} responses as JSONB. Package-private (ARQ-006).
 */
@Component
class IdempotencyPersistenceAdapter implements IdempotencyStorePort {

  private static final String OPERATION_CREATE_STAFF = "CREATE_STAFF";

  private final JdbcClient jdbcClient;
  private final ObjectMapper objectMapper;

  IdempotencyPersistenceAdapter(JdbcClient jdbcClient, ObjectMapper objectMapper) {
    this.jdbcClient = jdbcClient;
    this.objectMapper = objectMapper;
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<StoredResult> findByKeyAndOperation(UUID idempotencyKey, String operation) {
    return jdbcClient
        .sql(
            """
            SELECT request_hash, response_body
              FROM platform.idempotency_record
             WHERE idempotency_key = :key
               AND operation = :op
            """)
        .param("key", idempotencyKey)
        .param("op", operation)
        .query(
            (rs, rowNum) -> {
              String requestHash = rs.getString("request_hash");
              String responseJson = rs.getString("response_body");
              StaffAccount result = deserializeStaffAccount(responseJson);
              return new StoredResult(requestHash, result);
            })
        .optional();
  }

  @Override
  @Transactional
  public void save(UUID idempotencyKey, String operation, String requestHash, StaffAccount result) {
    String responseBody = serializeStaffAccount(result);
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
        .param("status", 201)
        .param("body", responseBody)
        .update();
  }

  private String serializeStaffAccount(StaffAccount account) {
    try {
      ObjectNode node = objectMapper.createObjectNode();
      node.put("id", account.getId().toString());
      node.put("email", account.getEmail());
      node.put("fullName", account.getFullName());
      node.put("status", account.getStatus().name());
      node.put("version", account.getVersion());
      return objectMapper.writeValueAsString(node);
    } catch (Exception e) {
      throw new IllegalStateException("Failed to serialize StaffAccount for idempotency store", e);
    }
  }

  private StaffAccount deserializeStaffAccount(String json) {
    try {
      JsonNode node = objectMapper.readTree(json);
      return StaffAccount.reconstitute(
          UUID.fromString(node.get("id").asText()),
          node.get("email").asText(),
          node.get("fullName").asText(),
          StaffAccountStatus.valueOf(node.get("status").asText()),
          node.get("version").asLong());
    } catch (Exception e) {
      throw new IllegalStateException(
          "Failed to deserialize StaffAccount from idempotency store", e);
    }
  }
}
