package co.edu.uniquindio.brujula.offering.application.port.out;

import co.edu.uniquindio.brujula.offering.domain.CourseOffering;
import co.edu.uniquindio.brujula.offering.domain.CourseOfferingId;

import java.util.Optional;

/**
 * Secondary port: persist a course offering and load it for optimistic-lock writes.
 *
 * <p>The {@link #save(CourseOffering)} implementation MUST:
 * <ol>
 *   <li>Build the JPA entity from the domain object (including version for optimistic lock)
 *   <li>Call {@code repository.save} followed by {@code repository.flush}
 *   <li>Return the managed entity mapped back to domain so services read the @Version-bumped value
 * </ol>
 *
 * <p>Inline {@link co.edu.uniquindio.brujula.offering.domain.ScheduleOption}s are cascaded
 * via {@code CascadeType.ALL} — no separate port needed for Slice 1. Satisfies: OFFW-003, OFFW-004.
 */
public interface SaveOfferingPort {

  /**
   * Persist the offering (and its inline schedule options via cascade) and return the managed
   * post-flush domain object whose version reflects the @Version increment.
   *
   * <p>May throw {@link org.springframework.dao.OptimisticLockingFailureException} if the
   * entity's version differs from the persisted version.
   */
  CourseOffering save(CourseOffering offering);

  /**
   * Load the current offering for an update operation (plain read; optimistic conflict
   * is detected at {@code save + flush} time via @Version, not at load time).
   */
  Optional<CourseOffering> findCurrentById(CourseOfferingId id);
}
