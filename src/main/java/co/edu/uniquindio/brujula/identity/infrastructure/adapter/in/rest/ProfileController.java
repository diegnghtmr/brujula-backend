package co.edu.uniquindio.brujula.identity.infrastructure.adapter.in.rest;

import co.edu.uniquindio.brujula.generated.api.ProfileApi;
import co.edu.uniquindio.brujula.generated.model.AccountStatus;
import co.edu.uniquindio.brujula.generated.model.AuditEvent;
import co.edu.uniquindio.brujula.generated.model.AuditEventPage;
import co.edu.uniquindio.brujula.generated.model.CurrentUser;
import co.edu.uniquindio.brujula.generated.model.UserRole;
import co.edu.uniquindio.brujula.identity.application.dto.ActivityItem;
import co.edu.uniquindio.brujula.identity.application.dto.ActivityPage;
import co.edu.uniquindio.brujula.identity.application.dto.UserProfile;
import co.edu.uniquindio.brujula.identity.application.port.in.GetCurrentUserQuery;
import co.edu.uniquindio.brujula.identity.application.port.in.ListMyActivityQuery;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST adapter for the Profile API ({@code GET /me}, {@code GET /me/activity}).
 *
 * <p>Implements the generated {@link ProfileApi} interface (contract-first). DTO↔domain mapping is
 * confined to this adapter (ARQ-005). Domain objects are never returned directly.
 *
 * <p>BOLA defence: the authenticated user's id is always derived from the session principal (via
 * {@link SecurityContextHolder}), never from a request parameter (TRD §10, Spec §Per-Resource
 * Authorization).
 *
 * <p>Satisfies: S1-B-7, Design §2.1 adapter.in.rest.
 */
@RestController
class ProfileController implements ProfileApi {

  private final GetCurrentUserQuery getCurrentUserQuery;
  private final ListMyActivityQuery listMyActivityQuery;

  ProfileController(
      GetCurrentUserQuery getCurrentUserQuery, ListMyActivityQuery listMyActivityQuery) {
    this.getCurrentUserQuery = getCurrentUserQuery;
    this.listMyActivityQuery = listMyActivityQuery;
  }

  /** GET /me — returns the authenticated user's own profile. */
  @Override
  public ResponseEntity<CurrentUser> getCurrentUser() {
    String sub = resolveSubjectFromContext();
    UserProfile profile = getCurrentUserQuery.getCurrentUserBySubject(sub);
    return ResponseEntity.ok(toDto(profile));
  }

  /** GET /me/activity — returns the authenticated user's own audit events. */
  @Override
  public ResponseEntity<AuditEventPage> listMyActivity(
      Integer page,
      Integer size,
      @Nullable List<String> sort,
      @Nullable OffsetDateTime from,
      @Nullable OffsetDateTime to) {
    String sub = resolveSubjectFromContext();
    UserProfile profile = getCurrentUserQuery.getCurrentUserBySubject(sub);
    int pageNum = page != null ? page : 0;
    int pageSize = size != null ? size : 20;
    ActivityPage activityPage =
        listMyActivityQuery.listMyActivity(profile.id(), from, to, pageNum, pageSize);
    return ResponseEntity.ok(toDto(activityPage));
  }

  // -------------------------------------------------------------------------
  // Principal resolution via SecurityContextHolder (BOLA safe — never from request param)
  // -------------------------------------------------------------------------

  private String resolveSubjectFromContext() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth != null && auth.getPrincipal() instanceof OidcUser oidcUser) {
      return oidcUser.getSubject();
    }
    // Fallback: use the authentication name (set to sub by Spring Security for OIDC)
    if (auth != null && auth.getName() != null) {
      return auth.getName();
    }
    throw new IllegalStateException("No authenticated principal found in SecurityContext");
  }

  // -------------------------------------------------------------------------
  // DTO mappers (confined to this adapter, ARQ-005)
  // Domain types are NOT imported here — only application-layer DTOs (ARQ-001).
  // -------------------------------------------------------------------------

  private CurrentUser toDto(UserProfile profile) {
    return new CurrentUser(
        profile.id(),
        profile.fullName(),
        profile.email(),
        UserRole.fromValue(profile.role()),
        AccountStatus.fromValue(profile.status()),
        profile.onboardingCompleted());
  }

  private AuditEventPage toDto(ActivityPage page) {
    List<AuditEvent> events = page.items().stream().map(this::toAuditEventDto).toList();
    return new AuditEventPage(
        page.page(), page.size(), page.totalElements(), page.totalPages(), events);
  }

  private AuditEvent toAuditEventDto(ActivityItem item) {
    AuditEvent event = new AuditEvent();
    event.setId(item.id());
    event.setActorId(item.actorId());
    event.setActorRole(UserRole.fromValue(item.actorRole()));
    event.setAction(item.action());
    event.setEntityType(item.entityType());
    event.setEntityId(item.entityId());
    event.setProgramId(item.programId());
    event.setOccurredAt(item.occurredAt());
    return event;
  }
}
