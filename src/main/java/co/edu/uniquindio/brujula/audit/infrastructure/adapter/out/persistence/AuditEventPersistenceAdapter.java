package co.edu.uniquindio.brujula.audit.infrastructure.adapter.out.persistence;

import co.edu.uniquindio.brujula.audit.application.dto.AuditEventItem;
import co.edu.uniquindio.brujula.audit.application.dto.AuditEventPage;
import co.edu.uniquindio.brujula.audit.application.port.out.SaveAuditEventPort;
import co.edu.uniquindio.brujula.audit.application.port.out.SearchAuditEventsPort;
import co.edu.uniquindio.brujula.audit.domain.AuditEvent;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Persistence adapter for audit events.
 *
 * <p>Implements {@link SaveAuditEventPort} (append-only via JPA) and {@link SearchAuditEventsPort}
 * (read via JdbcClient for dynamic filtering). Append-only: the adapter never calls update() or
 * delete(); it delegates only to {@code jpaRepository.save()} on insert.
 *
 * <p>Satisfies: Design §2.3, Spec §Audit Log (append-only, RNF-SEG-007).
 */
@Component
class AuditEventPersistenceAdapter implements SaveAuditEventPort, SearchAuditEventsPort {

  private final AuditEventJpaRepository jpaRepository;
  private final JdbcClient jdbcClient;

  AuditEventPersistenceAdapter(AuditEventJpaRepository jpaRepository, JdbcClient jdbcClient) {
    this.jpaRepository = jpaRepository;
    this.jdbcClient = jdbcClient;
  }

  @Override
  @Transactional
  public void save(AuditEvent event) {
    jpaRepository.saveAndFlush(AuditEventMapper.toEntity(event));
  }

  @Override
  @Transactional(readOnly = true)
  public AuditEventPage search(
      @Nullable String actorRole,
      @Nullable String action,
      @Nullable String entityType,
      @Nullable OffsetDateTime from,
      @Nullable OffsetDateTime to,
      int page,
      int size) {

    StringBuilder whereClause = new StringBuilder(" WHERE 1=1");
    var baseParams = new ArrayList<Object>();

    if (actorRole != null) {
      whereClause.append(" AND actor_role = ?");
      baseParams.add(actorRole);
    }
    if (action != null) {
      whereClause.append(" AND action = ?");
      baseParams.add(action);
    }
    if (entityType != null) {
      whereClause.append(" AND entity_type = ?");
      baseParams.add(entityType);
    }
    if (from != null) {
      whereClause.append(" AND occurred_at >= ?");
      baseParams.add(from);
    }
    if (to != null) {
      whereClause.append(" AND occurred_at <= ?");
      baseParams.add(to);
    }

    String countSql = "SELECT COUNT(*) FROM audit.audit_event" + whereClause;
    Long total = jdbcClient.sql(countSql).params(baseParams.toArray()).query(Long.class).single();
    long totalElements = total != null ? total : 0L;
    int totalPages = size > 0 ? (int) Math.ceil((double) totalElements / size) : 0;

    String dataSql =
        "SELECT id, actor_id, actor_role, action, entity_type, entity_id, program_id,"
            + " occurred_at, previous_value::text, new_value::text"
            + " FROM audit.audit_event"
            + whereClause
            + " ORDER BY occurred_at DESC LIMIT ? OFFSET ?";

    var dataParams = new ArrayList<>(baseParams);
    dataParams.add(size);
    dataParams.add((long) page * size);

    List<AuditEventItem> items =
        jdbcClient
            .sql(dataSql)
            .params(dataParams.toArray())
            .query(
                (rs, rowNum) ->
                    new AuditEventItem(
                        rs.getObject("id", UUID.class),
                        rs.getObject("actor_id", UUID.class),
                        rs.getString("actor_role"),
                        rs.getString("action"),
                        rs.getString("entity_type"),
                        rs.getObject("entity_id", UUID.class),
                        rs.getObject("program_id", UUID.class),
                        rs.getString("previous_value"),
                        rs.getString("new_value"),
                        rs.getObject("occurred_at", OffsetDateTime.class)))
            .list();

    return new AuditEventPage(items, page, size, totalElements, totalPages);
  }
}
