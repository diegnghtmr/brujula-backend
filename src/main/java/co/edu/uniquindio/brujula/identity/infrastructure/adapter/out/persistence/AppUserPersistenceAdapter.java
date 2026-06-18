package co.edu.uniquindio.brujula.identity.infrastructure.adapter.out.persistence;

import co.edu.uniquindio.brujula.identity.application.port.out.LoadAppUserPort;
import co.edu.uniquindio.brujula.identity.application.port.out.SaveAppUserPort;
import co.edu.uniquindio.brujula.identity.domain.AppUser;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * JPA-backed adapter implementing the {@link LoadAppUserPort} and {@link SaveAppUserPort} output
 * ports.
 *
 * <p>Uses {@link AppUserJpaRepository} (Spring Data JPA) and {@link AppUserMapper} for domain ↔
 * entity conversion. Optimistic locking is handled by the {@code @Version} annotation on {@link
 * AppUserJpaEntity}.
 */
@Component
class AppUserPersistenceAdapter implements LoadAppUserPort, SaveAppUserPort {

  private final AppUserJpaRepository repository;

  AppUserPersistenceAdapter(AppUserJpaRepository repository) {
    this.repository = repository;
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<AppUser> findByOidcSubject(String oidcSubject) {
    return repository.findByOidcSubject(oidcSubject).map(AppUserMapper::toDomain);
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<AppUser> findById(UUID id) {
    return repository.findById(id).map(AppUserMapper::toDomain);
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<AppUser> findByEmail(String email) {
    return repository.findByEmail(email).map(AppUserMapper::toDomain);
  }

  @Override
  @Transactional(readOnly = true)
  public boolean existsByEmail(String email) {
    return repository.existsByEmail(email);
  }

  @Override
  @Transactional
  public AppUser save(AppUser user) {
    AppUserJpaEntity entity = AppUserMapper.toEntity(user);
    AppUserJpaEntity saved = repository.save(entity);
    return AppUserMapper.toDomain(saved);
  }
}
