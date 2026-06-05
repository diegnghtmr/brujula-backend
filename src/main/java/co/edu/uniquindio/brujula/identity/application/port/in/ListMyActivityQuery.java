package co.edu.uniquindio.brujula.identity.application.port.in;

import co.edu.uniquindio.brujula.identity.application.dto.ActivityPage;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Input port: retrieve the authenticated user's own activity (audit events).
 *
 * <p>Backs {@code GET /me/activity}. Reads from {@code audit.audit_event} scoped by actor_id.
 *
 * <p>NOTE (WU-B scope): audit RECORDING across modules is WU-C. This query may return an empty page
 * until WU-C wires recording into use cases.
 */
public interface ListMyActivityQuery {

  /**
   * List the requesting user's own activity.
   *
   * @param userId the session owner's internal UUID
   * @param from optional lower date bound
   * @param to optional upper date bound
   * @param page zero-based page
   * @param size page size
   * @return a page of activity items (may be empty)
   */
  ActivityPage listMyActivity(
      UUID userId, OffsetDateTime from, OffsetDateTime to, int page, int size);
}
