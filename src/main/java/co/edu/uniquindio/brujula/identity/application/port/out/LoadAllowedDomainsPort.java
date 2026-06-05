package co.edu.uniquindio.brujula.identity.application.port.out;

import java.util.Set;

/**
 * Output port: loads the current set of allowed email domains.
 *
 * <p>Data lives in {@code administration.allowed_email_domain}. This port exposes only a read-only
 * query used by the identity module during provisioning (RN-007, RNF-SEG-006 dual enforcement).
 *
 * <p>Implemented by the administration persistence adapter (allowed via Modulith dependency:
 * identity → sharedkernel, audit; but NOT directly to administration internals). In practice the
 * implementation is injected at Spring wiring time — the identity application layer depends only on
 * this interface.
 */
public interface LoadAllowedDomainsPort {

  /**
   * Return the set of currently allowed domain hostnames (e.g. {@code "uniquindio.edu.co"}).
   *
   * @return non-null, possibly empty set
   */
  Set<String> loadAllowedDomains();
}
