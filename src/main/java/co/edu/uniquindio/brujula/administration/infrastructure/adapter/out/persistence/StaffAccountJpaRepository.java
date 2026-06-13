package co.edu.uniquindio.brujula.administration.infrastructure.adapter.out.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data JPA repository for staff accounts. Package-private. */
interface StaffAccountJpaRepository extends JpaRepository<StaffAccountJpaEntity, UUID> {

  Optional<StaffAccountJpaEntity> findByEmail(String email);

  Page<StaffAccountJpaEntity> findByStatus(
      StaffAccountJpaEntity.StaffStatusJpa status, Pageable pageable);
}
