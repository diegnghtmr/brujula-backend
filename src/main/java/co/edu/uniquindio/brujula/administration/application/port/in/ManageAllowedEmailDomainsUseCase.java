package co.edu.uniquindio.brujula.administration.application.port.in;

import co.edu.uniquindio.brujula.administration.application.dto.AllowedDomainView;
import java.util.List;
import java.util.UUID;

/**
 * Input port: manage allowed email domains (list, add, remove).
 *
 * <p>Uses {@link AllowedDomainView} from {@code application.dto} to comply with ARQ-003 (all types
 * in {@code application.port.in} must be interfaces only).
 *
 * <p>Satisfies: Design §2.2 ManageAllowedEmailDomainsUseCase, Spec §Allowed Email Domains.
 */
public interface ManageAllowedEmailDomainsUseCase {

  /** List all currently allowed email domains. */
  List<AllowedDomainView> listAllowedDomains();

  /**
   * Add a new allowed email domain.
   *
   * @param domain the normalized hostname (e.g. otheruniversity.edu.co)
   * @param actorId the internal ID of the admin performing the action (for audit)
   * @return the persisted domain entry
   */
  AllowedDomainView addDomain(String domain, UUID actorId);

  /**
   * Remove an allowed email domain by id.
   *
   * @param domainId the domain UUID
   * @param actorId the internal ID of the admin performing the action (for audit)
   */
  void removeDomain(UUID domainId, UUID actorId);
}
