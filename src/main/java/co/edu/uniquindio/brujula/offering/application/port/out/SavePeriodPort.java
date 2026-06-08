package co.edu.uniquindio.brujula.offering.application.port.out;

import co.edu.uniquindio.brujula.offering.domain.AcademicPeriod;
import co.edu.uniquindio.brujula.offering.domain.AcademicPeriodId;

import java.util.Optional;

/**
 * Secondary port: persist and load an {@link AcademicPeriod} for write operations.
 *
 * <p>Separate from {@link LoadPeriodPort} (read-only) per ISP. The save method
 * returns the managed post-flush domain object (version bumped by JPA @Version).
 *
 * <p>Satisfies: OFFW-001, OFFW-002.
 */
public interface SavePeriodPort {

  /**
   * Persist the given period and return the managed domain object after flush.
   *
   * <p>Implementations MUST flush after save so the returned period carries the
   * JPA-incremented {@code @Version} value. This enables optimistic-lock detection
   * and version read-back in {@code UpdatePeriodService}.
   *
   * @param period the domain entity to persist
   * @return the post-flush domain entity with the bumped version
   */
  AcademicPeriod save(AcademicPeriod period);

  /**
   * Load the current state of a period before applying an update.
   *
   * <p>This is a short read-only load; conflict detection relies on the {@code @Version}
   * optimistic lock applied at save time, NOT on a pessimistic SELECT FOR UPDATE.
   *
   * @param id the period identity
   * @return the current persisted period, or empty if not found
   */
  Optional<AcademicPeriod> findCurrentById(AcademicPeriodId id);
}
