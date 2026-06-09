package co.edu.uniquindio.brujula.curriculum.application.port.out;

import co.edu.uniquindio.brujula.curriculum.domain.Course;
import co.edu.uniquindio.brujula.curriculum.domain.CourseId;
import java.util.Optional;

/**
 * Output port: persist Course aggregates (create, update, archive).
 *
 * <p>Named by need (ISP). No Spring/framework imports.
 *
 * <p>Contract: {@link #save} must flush the JPA entity after save so that the {@code @Version}
 * column is incremented and returned in the managed entity — the caller reads the bumped version
 * from the returned domain object (optimistic-lock pattern, ADR-010, mirrors SaveOfferingPort).
 */
public interface SaveCoursePort {

  /**
   * Persist (insert or update) a Course aggregate.
   *
   * <p>Flushes after save so {@code @Version} is incremented before the method returns.
   *
   * @param course the course to persist
   * @return the managed, post-flush Course with the updated version
   * @throws org.springframework.dao.OptimisticLockingFailureException if the entity was modified
   *     concurrently and the supplied version does not match the DB version
   */
  Course save(Course course);

  /**
   * Load the current version of a Course for write (optimistic-lock read).
   *
   * @param id the course ID
   * @return the current course, or empty if not found
   */
  Optional<Course> findCurrentById(CourseId id);
}
