package co.edu.uniquindio.brujula.audit.application.port.out;

import co.edu.uniquindio.brujula.audit.application.dto.AuditEventPage;
import java.time.OffsetDateTime;
import org.springframework.lang.Nullable;

/**
 * Output port: queries audit events with optional filters.
 *
 * <p>Used by the global admin read side and the staff scoped read side. Program-scoped filtering
 * for STAFF is a Fase-2 concern; in Fase 1 the programId filter is passed as-is to the query.
 */
public interface SearchAuditEventsPort {

  /**
   * Search audit events with optional filters.
   *
   * @param actorRole optional filter by role string
   * @param action optional filter by action code
   * @param entityType optional filter by entity type
   * @param from optional lower bound (inclusive)
   * @param to optional upper bound (inclusive)
   * @param page zero-based page number
   * @param size page size
   * @return paginated result (never null)
   */
  AuditEventPage search(
      @Nullable String actorRole,
      @Nullable String action,
      @Nullable String entityType,
      @Nullable OffsetDateTime from,
      @Nullable OffsetDateTime to,
      int page,
      int size);
}
