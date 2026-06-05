package co.edu.uniquindio.brujula.identity.infrastructure.adapter.out.persistence;

import co.edu.uniquindio.brujula.identity.application.dto.ActivityItem;
import co.edu.uniquindio.brujula.identity.application.dto.ActivityPage;
import co.edu.uniquindio.brujula.identity.application.port.out.ReadActivityPort;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reads the authenticated user's own activity records from {@code audit.audit_event} using JDBC.
 *
 * <p>This is a READ-ONLY cross-schema access from the identity module into the audit schema. The
 * identity module reads only its own actor's events — it does NOT import the audit module's JPA
 * adapter (ARQ-006 compliant). Comprehensive audit RECORDING is WU-C; for WU-B the table may be
 * empty.
 *
 * <p>Satisfies: S1-B-6 (activity sub-task), Design §2.1 ReadActivityPort, Spec §Personal Profile
 * and Activity.
 */
@Component
class AuditEventReadAdapter implements ReadActivityPort {

  private final JdbcClient jdbcClient;

  AuditEventReadAdapter(JdbcClient jdbcClient) {
    this.jdbcClient = jdbcClient;
  }

  @Override
  @Transactional(readOnly = true)
  public ActivityPage findByActorId(
      UUID actorId, OffsetDateTime from, OffsetDateTime to, int page, int size) {

    // Build WHERE clause and params dynamically
    StringBuilder whereClause = new StringBuilder(" WHERE actor_id = ?");
    var baseParams = new ArrayList<Object>();
    baseParams.add(actorId);

    if (from != null) {
      whereClause.append(" AND occurred_at >= ?");
      baseParams.add(from);
    }
    if (to != null) {
      whereClause.append(" AND occurred_at <= ?");
      baseParams.add(to);
    }

    // Count query
    String countSql = "SELECT COUNT(*) FROM audit.audit_event" + whereClause;
    Long total = jdbcClient.sql(countSql).params(baseParams.toArray()).query(Long.class).single();

    long totalElements = total != null ? total : 0L;
    int totalPages = size > 0 ? (int) Math.ceil((double) totalElements / size) : 0;

    // Data query
    String dataSql =
        "SELECT id, actor_id, actor_role, action, entity_type, entity_id, program_id,"
            + " occurred_at FROM audit.audit_event"
            + whereClause
            + " ORDER BY occurred_at DESC LIMIT ? OFFSET ?";

    var dataParams = new ArrayList<>(baseParams);
    dataParams.add(size);
    dataParams.add((long) page * size);

    List<ActivityItem> items =
        jdbcClient
            .sql(dataSql)
            .params(dataParams.toArray())
            .query(
                (rs, rowNum) ->
                    new ActivityItem(
                        rs.getObject("id", UUID.class),
                        rs.getObject("actor_id", UUID.class),
                        rs.getString("actor_role"),
                        rs.getString("action"),
                        rs.getString("entity_type"),
                        rs.getObject("entity_id", UUID.class),
                        rs.getObject("program_id", UUID.class),
                        rs.getObject("occurred_at", OffsetDateTime.class)))
            .list();

    return new ActivityPage(items, page, size, totalElements, totalPages);
  }
}
