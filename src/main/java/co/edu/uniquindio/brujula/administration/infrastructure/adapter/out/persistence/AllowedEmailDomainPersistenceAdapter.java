package co.edu.uniquindio.brujula.administration.infrastructure.adapter.out.persistence;

import co.edu.uniquindio.brujula.administration.application.dto.AllowedDomainView;
import co.edu.uniquindio.brujula.administration.application.port.out.SaveAllowedEmailDomainPort;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * JPA persistence adapter for the {@code allowed_email_domain} table (Flyway V003).
 *
 * <p>Implements {@link SaveAllowedEmailDomainPort}. Package-private (ARQ-006).
 */
@Component
class AllowedEmailDomainPersistenceAdapter implements SaveAllowedEmailDomainPort {

  private final AllowedEmailDomainJpaRepository jpaRepository;

  AllowedEmailDomainPersistenceAdapter(AllowedEmailDomainJpaRepository jpaRepository) {
    this.jpaRepository = jpaRepository;
  }

  @Override
  @Transactional
  public AllowedDomainView save(String domain) {
    AllowedEmailDomainJpaEntity entity = new AllowedEmailDomainJpaEntity(UUID.randomUUID(), domain);
    AllowedEmailDomainJpaEntity saved = jpaRepository.save(entity);
    return new AllowedDomainView(saved.getId(), saved.getDomain());
  }

  @Override
  @Transactional
  public void deleteById(UUID id) {
    jpaRepository.deleteById(id);
  }

  @Override
  @Transactional(readOnly = true)
  public List<AllowedDomainView> findAll() {
    return jpaRepository.findAll().stream()
        .map(e -> new AllowedDomainView(e.getId(), e.getDomain()))
        .toList();
  }
}
