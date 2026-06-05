package co.edu.uniquindio.brujula.identity.infrastructure.adapter.out.persistence;

import co.edu.uniquindio.brujula.identity.application.port.out.LoadAllowedDomainsPort;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reads the allowed email domains from the {@code administration.allowed_email_domain} table using
 * a plain JDBC query.
 *
 * <p>This is a READ-ONLY cross-schema access: the identity module reads only the domain strings
 * from the administration schema. It does NOT reach into {@code administration} module's internal
 * JPA adapter (ARQ-006 compliant — no import of administration's JPA entity/repository).
 *
 * <p>Satisfies: S1-B-6, RN-007, RNF-SEG-006 (dual domain enforcement at provisioning).
 */
@Component
class AllowedDomainQueryAdapter implements LoadAllowedDomainsPort {

  private final JdbcClient jdbcClient;

  AllowedDomainQueryAdapter(JdbcClient jdbcClient) {
    this.jdbcClient = jdbcClient;
  }

  @Override
  @Transactional(readOnly = true)
  public Set<String> loadAllowedDomains() {
    List<String> domains =
        jdbcClient
            .sql("SELECT domain FROM administration.allowed_email_domain")
            .query(String.class)
            .list();
    return new HashSet<>(domains);
  }
}
