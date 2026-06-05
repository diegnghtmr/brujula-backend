package co.edu.uniquindio.brujula.identity.application.service;

import co.edu.uniquindio.brujula.identity.application.dto.ActivityPage;
import co.edu.uniquindio.brujula.identity.application.port.in.ListMyActivityQuery;
import co.edu.uniquindio.brujula.identity.application.port.out.ReadActivityPort;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Query service: returns the authenticated user's own activity from the audit log.
 *
 * <p>Backs {@code GET /me/activity}. Delegates pagination and filtering to {@link
 * ReadActivityPort}.
 *
 * <p>NOTE (WU-B scope): audit recording across modules is WU-C. This service returns an empty page
 * until WU-C wires audit recording into use cases.
 */
public class ListMyActivityService implements ListMyActivityQuery {

  private final ReadActivityPort readActivityPort;

  public ListMyActivityService(ReadActivityPort readActivityPort) {
    this.readActivityPort = readActivityPort;
  }

  @Override
  public ActivityPage listMyActivity(
      UUID userId, OffsetDateTime from, OffsetDateTime to, int page, int size) {
    return readActivityPort.findByActorId(userId, from, to, page, size);
  }
}
