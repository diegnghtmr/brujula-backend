package co.edu.uniquindio.brujula.administration.application.port.out;

import java.util.Set;

/**
 * Output port: read allowed email domains from the administration module's own store.
 *
 * <p>This port is used by the administration application services (CreateStaffService). It reads
 * from {@code administration.allowed_email_domain}. The identity module has its own {@code
 * LoadAllowedDomainsPort} that reads via JdbcClient cross-schema — they are separate.
 */
public interface LoadAllowedEmailDomainsPort {

  /** Load all currently allowed email domain hostnames (lowercase). */
  Set<String> loadAllowedDomains();
}
