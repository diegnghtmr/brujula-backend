package co.edu.uniquindio.brujula.administration.infrastructure.adapter.out.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data JPA repository for allowed email domains. Package-private (ARQ-006). */
interface AllowedEmailDomainJpaRepository extends JpaRepository<AllowedEmailDomainJpaEntity, UUID> {

  boolean existsByDomain(String domain);
}
