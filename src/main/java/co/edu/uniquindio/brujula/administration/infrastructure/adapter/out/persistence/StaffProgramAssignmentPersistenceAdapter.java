package co.edu.uniquindio.brujula.administration.infrastructure.adapter.out.persistence;

import co.edu.uniquindio.brujula.administration.application.port.out.LoadStaffProgramAssignmentsPort;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * JPA persistence adapter for staff-program assignment reads.
 *
 * <p>Implements {@link LoadStaffProgramAssignmentsPort}. Read-only — the write lifecycle
 * (assigning/revoking programs) is deferred.
 *
 * <p>Package-private.
 */
@Component
class StaffProgramAssignmentPersistenceAdapter implements LoadStaffProgramAssignmentsPort {

  private final StaffProgramAssignmentJpaRepository jpaRepository;

  StaffProgramAssignmentPersistenceAdapter(StaffProgramAssignmentJpaRepository jpaRepository) {
    this.jpaRepository = jpaRepository;
  }

  @Override
  @Transactional(readOnly = true)
  public Set<UUID> findActiveAssignmentsByStaffId(UUID staffId) {
    return jpaRepository.findByStaffIdAndActive(staffId, true).stream()
        .map(StaffProgramAssignmentJpaEntity::getProgramId)
        .collect(Collectors.toUnmodifiableSet());
  }
}
