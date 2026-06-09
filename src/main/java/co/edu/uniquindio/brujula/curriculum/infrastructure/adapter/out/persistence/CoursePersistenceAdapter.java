package co.edu.uniquindio.brujula.curriculum.infrastructure.adapter.out.persistence;

import co.edu.uniquindio.brujula.curriculum.application.port.out.LoadCoursePort;
import co.edu.uniquindio.brujula.curriculum.application.port.out.SaveCoursePort;
import co.edu.uniquindio.brujula.curriculum.domain.Course;
import co.edu.uniquindio.brujula.curriculum.domain.CourseId;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * JPA-backed adapter implementing {@link LoadCoursePort} and {@link SaveCoursePort}.
 *
 * <p>Package-private per ARQ-006. Satisfies: CUR-001, Phase 2 WU4 (read) and WU5 (write).
 *
 * <p>Transaction strategy: class-level {@code @Transactional} (read-write default) so write methods
 * inherit a writable transaction; each read method explicitly overrides with
 * {@code @Transactional(readOnly=true)} for performance (mirrors OfferingPersistenceAdapter).
 */
@Component
@Transactional
class CoursePersistenceAdapter implements LoadCoursePort, SaveCoursePort {

  private final CourseJpaRepository repository;

  CoursePersistenceAdapter(CourseJpaRepository repository) {
    this.repository = repository;
  }

  // ---- LoadCoursePort ----

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
    return repository.findById(courseId.value()).map(CurriculumPersistenceMapper::toDomain);
  }

  @Override
  @Transactional(readOnly = true)
  public List<Course> loadByIds(Set<UUID> courseIds) {
    if (courseIds == null || courseIds.isEmpty()) {
      return List.of();
    }
    // findAllById issues a single IN-clause query for all IDs
    return repository.findAllById(courseIds).stream()
        .map(CurriculumPersistenceMapper::toDomain)
        .toList();
  }

  // ---- SaveCoursePort ----

  @Override
  public Course save(Course course) {
    CourseJpaEntity entity = CurriculumPersistenceMapper.toJpaEntity(course);
    CourseJpaEntity managed = repository.save(entity);
    repository.flush();
    return CurriculumPersistenceMapper.toDomain(managed);
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<Course> findCurrentById(CourseId courseId) {
    return repository.findById(courseId.value()).map(CurriculumPersistenceMapper::toDomain);
  }
}
