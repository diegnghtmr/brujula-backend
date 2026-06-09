package co.edu.uniquindio.brujula.curriculum.infrastructure.adapter.out.persistence;

import co.edu.uniquindio.brujula.curriculum.application.port.out.LoadCourseRequirementPort;
import co.edu.uniquindio.brujula.curriculum.application.port.out.SaveCourseRequirementPort;
import co.edu.uniquindio.brujula.curriculum.domain.CourseRequirement;
import co.edu.uniquindio.brujula.curriculum.domain.CourseRequirementId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * JPA-backed adapter implementing {@link LoadCourseRequirementPort} and
 * {@link SaveCourseRequirementPort}.
 *
 * <p>Package-private per ARQ-006. Satisfies: CUR-003, Phase 2 WU4 (read) and WU5 (write).
 *
 * <p>Transaction strategy: class-level {@code @Transactional} (read-write default) so write
 * methods inherit a writable transaction; each read method explicitly overrides with
 * {@code @Transactional(readOnly=true)} (mirrors OfferingPersistenceAdapter).
 */
@Component
@Transactional
class CourseRequirementPersistenceAdapter
    implements LoadCourseRequirementPort, SaveCourseRequirementPort {

  private final CourseRequirementJpaRepository repository;

  CourseRequirementPersistenceAdapter(CourseRequirementJpaRepository repository) {
    this.repository = repository;
  }

  // ---- LoadCourseRequirementPort ----

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

  // ---- SaveCourseRequirementPort ----

  @Override
  public CourseRequirement save(CourseRequirement requirement) {
    CourseRequirementJpaEntity entity = CurriculumPersistenceMapper.toJpaEntity(requirement);
    CourseRequirementJpaEntity managed = repository.save(entity);
    repository.flush();
    return CurriculumPersistenceMapper.toDomain(managed);
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<CourseRequirement> findCurrentById(CourseRequirementId requirementId) {
    return repository.findById(requirementId.value())
        .map(CurriculumPersistenceMapper::toDomain);
  }

  @Override
  @Transactional(readOnly = true)
  public List<CourseRequirement> findAllByPlanId(UUID planId) {
    return repository.findAllByPlanId(planId).stream()
        .map(CurriculumPersistenceMapper::toDomain)
        .toList();
  }
}
