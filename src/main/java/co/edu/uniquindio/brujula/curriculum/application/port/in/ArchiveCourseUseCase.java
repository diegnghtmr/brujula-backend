package co.edu.uniquindio.brujula.curriculum.application.port.in;

import co.edu.uniquindio.brujula.curriculum.application.dto.ArchiveCourseCommand;
import co.edu.uniquindio.brujula.curriculum.application.dto.CourseView;

/**
 * Input port: archive a Course (soft-delete, optimistic lock).
 *
 * <p>Published via {@code curriculum::api} NamedInterface. No framework imports.
 */
public interface ArchiveCourseUseCase {

  /**
   * Archive a course (status → ARCHIVED). Does NOT hard-delete; requirements are preserved.
   *
   * @param command the archive command (contains expectedVersion for optimistic lock)
   * @return a view of the archived course
   * @throws org.springframework.dao.OptimisticLockingFailureException if the version is stale
   * @throws co.edu.uniquindio.brujula.curriculum.domain.CourseNotFoundException if course not found
   */
  CourseView archiveCourse(ArchiveCourseCommand command);
}
