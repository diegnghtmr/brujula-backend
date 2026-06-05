package co.edu.uniquindio.brujula.identity.application.port.out;

import co.edu.uniquindio.brujula.identity.application.dto.ActivityPage;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Output port: reads audit events scoped to a specific actor (the authenticated user's own
 * activity).
 *
 * <p>Reads from {@code audit.audit_event} filtered by actor_id. Comprehensive audit RECORDING
 * across modules is WU-C; for WU-B this port only supports reading the user's own events (may
 * return an empty page until WU-C wires recording).
 */
public interface ReadActivityPort {

  /**
   * Return a page of audit events for the given actor.
   *
   * @param actorId the internal user UUID (session owner)
   * @param from optional lower bound (inclusive)
   * @param to optional upper bound (inclusive)
   * @param page zero-based page number
   * @param size page size
   * @return a page of activity entries (may be empty)
   */
  ActivityPage findByActorId(
      UUID actorId, OffsetDateTime from, OffsetDateTime to, int page, int size);
}
