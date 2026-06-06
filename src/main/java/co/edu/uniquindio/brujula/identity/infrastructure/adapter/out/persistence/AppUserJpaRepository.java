package co.edu.uniquindio.brujula.identity.infrastructure.adapter.out.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA repository for {@link AppUserJpaEntity}.
 *
 * <p>Internal to the identity persistence adapter. Must not be accessed from outside {@code
 * identity.infrastructure.adapter.out.persistence} (ARQ-006).
 */
interface AppUserJpaRepository extends JpaRepository<AppUserJpaEntity, UUID> {

  Optional<AppUserJpaEntity> findByOidcSubject(String oidcSubject);

  Optional<AppUserJpaEntity> findByEmail(String email);

  boolean existsByEmail(String email);
}
