package co.edu.uniquindio.brujula.offering.infrastructure.adapter.out.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Spring Data repository for {@link ScheduleOptionJpaEntity}.
 *
 * <p>Package-private — implementation detail, not exported.
 */
interface ScheduleOptionJpaRepository extends JpaRepository<ScheduleOptionJpaEntity, UUID> {

  /** Returns the schedule option with its sessions and parent offering eagerly loaded. */
  @Query(
      """
      SELECT opt FROM ScheduleOptionJpaEntity opt
      LEFT JOIN FETCH opt.sessions
      JOIN FETCH opt.offering
      WHERE opt.id = :id
      """)
  Optional<ScheduleOptionJpaEntity> findByIdWithSessionsAndOffering(@Param("id") UUID id);
}
