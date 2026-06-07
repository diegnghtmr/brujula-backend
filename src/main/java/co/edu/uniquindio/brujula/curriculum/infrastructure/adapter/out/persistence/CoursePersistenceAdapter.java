package co.edu.uniquindio.brujula.curriculum.infrastructure.adapter.out.persistence;

import co.edu.uniquindio.brujula.curriculum.application.port.out.LoadCoursePort;
import co.edu.uniquindio.brujula.curriculum.domain.Course;
import co.edu.uniquindio.brujula.curriculum.domain.CourseId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * JPA-backed adapter implementing {@link LoadCoursePort}.
 *
 * <p>Package-private per ARQ-006. Satisfies: CUR-001, Phase 2 WU4 persistence adapters.
 */
@Component
class CoursePersistenceAdapter implements LoadCoursePort {

  private final CourseJpaRepository repository;

  CoursePersistenceAdapter(CourseJpaRepository repository) {
    this.repository = repository;
  }

  @Override
  @Transactional(readOnly = true)
  public List<Course> loadByPlanId(UUID planId) {
    return repository.findByPlanId(planId).stream()
        .map(CurriculumPersistenceMapper::toDomain)
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<Course> loadById(CourseId courseId) {
    return repository.findById(courseId.value())
        .map(CurriculumPersistenceMapper::toDomain);
  }
}
