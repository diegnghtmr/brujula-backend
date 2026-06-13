package co.edu.uniquindio.brujula.administration.infrastructure.adapter.out.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA repository for staff-program assignments.
 *
 * <p>Package-private.
 */
interface StaffProgramAssignmentJpaRepository
    extends JpaRepository<StaffProgramAssignmentJpaEntity, UUID> {

  /**
   * Find all assignment rows for the given staff member regardless of active status.
   *
   * @param staffId the staff account UUID
   * @return list of assignment entities
   */
  List<StaffProgramAssignmentJpaEntity> findByStaffId(UUID staffId);

  /**
   * Find active assignment rows for the given staff member.
   *
   * @param staffId the staff account UUID
   * @param active the active flag value
   * @return list of matching assignment entities
   */
  List<StaffProgramAssignmentJpaEntity> findByStaffIdAndActive(UUID staffId, boolean active);
}
