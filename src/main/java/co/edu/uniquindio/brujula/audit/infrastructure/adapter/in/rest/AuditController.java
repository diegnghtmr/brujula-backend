package co.edu.uniquindio.brujula.audit.infrastructure.adapter.in.rest;

import co.edu.uniquindio.brujula.audit.application.dto.AuditEventItem;
import co.edu.uniquindio.brujula.audit.application.dto.AuditEventPage;
import co.edu.uniquindio.brujula.audit.application.port.out.SearchAuditEventsPort;
import co.edu.uniquindio.brujula.generated.api.AuditApi;
import co.edu.uniquindio.brujula.generated.model.AuditEvent;
import co.edu.uniquindio.brujula.generated.model.UserRole;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST adapter implementing the generated {@link AuditApi} (contract-first).
 *
 * <p>Maps query parameters to the SearchAuditEventsPort and translates application-layer DTOs to
 * generated model DTOs. Enforces role-based authorization via {@code @PreAuthorize}.
 *
 * <p>The {@code /staff/audit-events} endpoint accepts a {@code programId} param per the contract,
 * but program-scoped filtering (staff→program assignment check) is deferred; the param is passed
 * through without enforcement.
 *
 * <p>No domain type imports — uses only application-layer DTOs ({@link AuditEventItem}).
 */
@RestController
class AuditController implements AuditApi {

  private final SearchAuditEventsPort searchAuditEventsPort;

  AuditController(SearchAuditEventsPort searchAuditEventsPort) {
    this.searchAuditEventsPort = searchAuditEventsPort;
  }

  @Override
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<co.edu.uniquindio.brujula.generated.model.AuditEventPage>
      listGlobalAuditEvents(
          @Nullable UserRole actorRole,
          @Nullable String action,
          @Nullable String entityType,
          @Nullable OffsetDateTime from,
          @Nullable OffsetDateTime to,
          Integer page,
          Integer size,
          @Nullable List<String> sort) {

    int pageNum = page != null ? page : 0;
    int pageSize = size != null ? size : 20;
    String roleStr = actorRole != null ? actorRole.getValue() : null;

    AuditEventPage result =
        searchAuditEventsPort.search(roleStr, action, entityType, from, to, pageNum, pageSize);
    return ResponseEntity.ok(toDto(result));
  }

  @Override
  @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
  public ResponseEntity<co.edu.uniquindio.brujula.generated.model.AuditEventPage>
      listProgramAuditEvents(
          UUID programId,
          @Nullable String action,
          @Nullable OffsetDateTime from,
          @Nullable OffsetDateTime to,
          Integer page,
          Integer size,
          @Nullable List<String> sort) {

    // Program-scoped filtering (staff→program assignment) is deferred.
    // STAFF sees events filtered only by action/time — no program assignment check.
    int pageNum = page != null ? page : 0;
    int pageSize = size != null ? size : 20;

    AuditEventPage result =
        searchAuditEventsPort.search(null, action, null, from, to, pageNum, pageSize);
    return ResponseEntity.ok(toDto(result));
  }

  // -------------------------------------------------------------------------
  // DTO mappers (confined to this adapter)
  // -------------------------------------------------------------------------

  private co.edu.uniquindio.brujula.generated.model.AuditEventPage toDto(AuditEventPage page) {
    List<AuditEvent> events = page.items().stream().map(this::toDto).toList();
    return new co.edu.uniquindio.brujula.generated.model.AuditEventPage(
        page.page(), page.size(), page.totalElements(), page.totalPages(), events);
  }

  private AuditEvent toDto(AuditEventItem item) {
    AuditEvent dto = new AuditEvent();
    dto.setId(item.id());
    dto.setActorId(item.actorId());
    dto.setActorRole(UserRole.fromValue(item.actorRole()));
    dto.setAction(item.action());
    dto.setEntityType(item.entityType());
    dto.setEntityId(item.entityId());
    dto.setProgramId(item.programId());
    dto.setOccurredAt(item.occurredAt());
    dto.setPreviousValue(item.previousValue());
    dto.setNewValue(item.newValue());
    return dto;
  }
}
