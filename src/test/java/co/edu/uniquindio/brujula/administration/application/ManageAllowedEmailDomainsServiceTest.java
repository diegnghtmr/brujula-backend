package co.edu.uniquindio.brujula.administration.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.edu.uniquindio.brujula.administration.application.dto.AllowedDomainView;
import co.edu.uniquindio.brujula.administration.application.port.out.SaveAllowedEmailDomainPort;
import co.edu.uniquindio.brujula.administration.application.service.ManageAllowedEmailDomainsService;
import co.edu.uniquindio.brujula.audit.application.port.in.AuditRecorder;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link ManageAllowedEmailDomainsService} (strict TDD, zero Spring).
 *
 * <p>Spec §Audit Log: "allowed-domain delete" must be audited with action ALLOWED_DOMAIN_DELETED.
 */
class ManageAllowedEmailDomainsServiceTest {

  private SaveAllowedEmailDomainPort saveAllowedEmailDomainPort;
  private AuditRecorder auditRecorder;
  private ManageAllowedEmailDomainsService service;

  @BeforeEach
  void setUp() {
    saveAllowedEmailDomainPort = mock(SaveAllowedEmailDomainPort.class);
    auditRecorder = mock(AuditRecorder.class);
    service = new ManageAllowedEmailDomainsService(saveAllowedEmailDomainPort, auditRecorder);
  }

  // -------------------------------------------------------------------------
  // removeDomain — must record ALLOWED_DOMAIN_DELETED audit event
  // -------------------------------------------------------------------------

  @Test
  void remove_domain_records_allowed_domain_deleted_audit_event() {
    UUID domainId = UUID.randomUUID();
    UUID actorId = UUID.randomUUID();

    service.removeDomain(domainId, actorId);

    verify(auditRecorder)
        .record(
            argThat(
                cmd ->
                    "ALLOWED_DOMAIN_DELETED".equals(cmd.action())
                        && actorId.equals(cmd.actorId())
                        && "allowed_email_domain".equals(cmd.entityType())
                        && domainId.equals(cmd.entityId())));
    verify(saveAllowedEmailDomainPort).deleteById(domainId);
  }

  @Test
  void remove_domain_calls_persistence_port() {
    UUID domainId = UUID.randomUUID();
    UUID actorId = UUID.randomUUID();

    service.removeDomain(domainId, actorId);

    verify(saveAllowedEmailDomainPort).deleteById(domainId);
  }

  // -------------------------------------------------------------------------
  // addDomain — must NOT record deleted event
  // -------------------------------------------------------------------------

  @Test
  void add_domain_does_not_record_deleted_event() {
    UUID actorId = UUID.randomUUID();
    when(saveAllowedEmailDomainPort.save(any()))
        .thenReturn(new AllowedDomainView(UUID.randomUUID(), "newdomain.edu.co"));

    service.addDomain("newdomain.edu.co", actorId);

    verify(auditRecorder, never())
        .record(argThat(cmd -> "ALLOWED_DOMAIN_DELETED".equals(cmd.action())));
  }

  @Test
  void add_domain_rejects_blank_domain() {
    UUID actorId = UUID.randomUUID();

    assertThatThrownBy(() -> service.addDomain("  ", actorId))
        .isInstanceOf(IllegalArgumentException.class);
    verify(saveAllowedEmailDomainPort, never()).save(any());
  }

  @Test
  void list_domains_delegates_to_port() {
    UUID actorId = UUID.randomUUID();
    when(saveAllowedEmailDomainPort.findAll())
        .thenReturn(List.of(new AllowedDomainView(UUID.randomUUID(), "uniquindio.edu.co")));

    var result = service.listAllowedDomains();

    assertThat(result).hasSize(1);
    assertThat(result.getFirst().domain()).isEqualTo("uniquindio.edu.co");
  }
}
