package co.edu.uniquindio.brujula.progress.infrastructure.adapter.out.persistence;

import co.edu.uniquindio.brujula.progress.application.port.out.LoadCourseAttemptPort;
import co.edu.uniquindio.brujula.progress.application.port.out.SaveCourseAttemptPort;
import co.edu.uniquindio.brujula.progress.domain.CourseAttempt;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Driven adapter: JPA-backed persistence for course attempts.
 *
 * <p>Implements {@link LoadCourseAttemptPort} and {@link SaveCourseAttemptPort}. Package-private
 * per ARQ-006b. Optimistic locking is handled by JPA {@code @Version}; Spring translates
 * {@code OptimisticLockException} into {@code OptimisticLockingFailureException} which the REST
 * exception handler maps to 409.
 */
@Component
@Transactional
class CourseAttemptPersistenceAdapter implements LoadCourseAttemptPort, SaveCourseAttemptPort {

  private final CourseAttemptJpaRepository jpaRepository;
  private final CourseAttemptPersistenceMapper mapper;

  CourseAttemptPersistenceAdapter(CourseAttemptJpaRepository jpaRepository) {
    this.jpaRepository = jpaRepository;
    this.mapper = new CourseAttemptPersistenceMapper();
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<CourseAttempt> loadById(UUID attemptId) {
    return jpaRepository.findById(attemptId).map(mapper::toDomain);
  }

  @Override
  @Transactional(readOnly = true)
  public List<CourseAttempt> loadBySubscriptionId(UUID subscriptionId) {
    return jpaRepository.findBySubscriptionId(subscriptionId).stream()
        .map(mapper::toDomain)
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public List<CourseAttempt> loadBySubscriptionIdAndCourseId(
      UUID subscriptionId, UUID courseId, int page, int size) {
    return jpaRepository
        .findBySubscriptionIdAndOptionalCourseId(
            subscriptionId, courseId, PageRequest.of(page, size))
        .stream()
        .map(mapper::toDomain)
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public long countBySubscriptionIdAndCourseId(UUID subscriptionId, UUID courseId) {
    return jpaRepository.countBySubscriptionIdAndOptionalCourseId(subscriptionId, courseId);
  }

  @Override
  public CourseAttempt save(CourseAttempt attempt) {
    CourseAttemptJpaEntity entity = mapper.toJpaEntity(attempt);
    // Use the RETURNED managed entity — JPA increments @Version at flush time.
    CourseAttemptJpaEntity managed = jpaRepository.save(entity);
    jpaRepository.flush();
    return mapper.toDomain(managed);
  }

  @Override
  public void delete(CourseAttempt attempt) {
    jpaRepository.deleteById(attempt.getId().value());
  }
}
