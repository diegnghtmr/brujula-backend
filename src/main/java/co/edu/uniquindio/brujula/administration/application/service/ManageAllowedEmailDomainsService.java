package co.edu.uniquindio.brujula.administration.application.service;

import co.edu.uniquindio.brujula.administration.application.dto.AllowedDomainView;
import co.edu.uniquindio.brujula.administration.application.port.in.ManageAllowedEmailDomainsUseCase;
import co.edu.uniquindio.brujula.administration.application.port.out.SaveAllowedEmailDomainPort;
import co.edu.uniquindio.brujula.audit.application.dto.RecordAuditEventCommand;
import co.edu.uniquindio.brujula.audit.application.port.in.AuditRecorder;
import java.util.List;
import java.util.UUID;

/**
 * Use case: manage allowed email domains (list, add, remove).
 *
 * <p>Records an {@code ALLOWED_DOMAIN_DELETED} audit event when a domain is removed (synchronous).
 *
 * <p>Framework-free.
 */
public class ManageAllowedEmailDomainsService implements ManageAllowedEmailDomainsUseCase {

  private final SaveAllowedEmailDomainPort saveAllowedEmailDomainPort;
  private final AuditRecorder auditRecorder;

  public ManageAllowedEmailDomainsService(
      SaveAllowedEmailDomainPort saveAllowedEmailDomainPort, AuditRecorder auditRecorder) {
    this.saveAllowedEmailDomainPort = saveAllowedEmailDomainPort;
    this.auditRecorder = auditRecorder;
  }

  @Override
  public List<AllowedDomainView> listAllowedDomains() {
    return saveAllowedEmailDomainPort.findAll();
  }

  @Override
  public AllowedDomainView addDomain(String domain, UUID actorId) {
    if (domain == null || domain.isBlank()) {
      throw new IllegalArgumentException("domain must not be null or blank");
    }
    return saveAllowedEmailDomainPort.save(domain.toLowerCase().trim());
  }

  @Override
  public void removeDomain(UUID domainId, UUID actorId) {
    saveAllowedEmailDomainPort.deleteById(domainId);
    auditRecorder.record(
        new RecordAuditEventCommand(
            actorId,
            "ADMIN",
            "ALLOWED_DOMAIN_DELETED",
            "allowed_email_domain",
            domainId,
            null,
            null,
            null));
  }
}
