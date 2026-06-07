package co.edu.uniquindio.brujula.curriculum.infrastructure.adapter.out.persistence;

import co.edu.uniquindio.brujula.curriculum.application.port.out.LoadCourseRequirementPort;
import co.edu.uniquindio.brujula.curriculum.domain.CourseRequirement;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * JPA-backed adapter implementing {@link LoadCourseRequirementPort}.
 *
 * <p>Package-private per ARQ-006. Satisfies: CUR-003, Phase 2 WU4 persistence adapters.
 */
@Component
class CourseRequirementPersistenceAdapter implements LoadCourseRequirementPort {

  private final CourseRequirementJpaRepository repository;

  CourseRequirementPersistenceAdapter(CourseRequirementJpaRepository repository) {
    this.repository = repository;
  }

  @Override
  @Transactional(readOnly = true)
  public List<CourseRequirement> loadByTargetCourseId(UUID targetCourseId) {
    return repository.findByTargetCourseId(targetCourseId).stream()
        .map(CurriculumPersistenceMapper::toDomain)
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public List<CourseRequirement> loadByTargetCourseIds(List<UUID> courseIds) {
    if (courseIds.isEmpty()) {
      return List.of();
    }
    return repository.findByTargetCourseIdIn(courseIds).stream()
        .map(CurriculumPersistenceMapper::toDomain)
        .toList();
  }
}
