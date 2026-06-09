package co.edu.uniquindio.brujula.curriculum.infrastructure.adapter.out.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/**
 * Spring Data JPA repository for {@link CourseRequirementJpaEntity}.
 *
 * <p>Package-private per ARQ-006.
 */
interface CourseRequirementJpaRepository extends JpaRepository<CourseRequirementJpaEntity, UUID> {

  List<CourseRequirementJpaEntity> findByTargetCourseId(UUID targetCourseId);

  /**
   * Bulk fetch: load all requirements whose target course ID is in the given collection.
   * Used by malla assembly to avoid N+1 per course (ADR-F2-001).
   */
  List<CourseRequirementJpaEntity> findByTargetCourseIdIn(List<UUID> courseIds);

  /**
   * Load all requirements whose target course belongs to a given plan.
   * Used by cycle detection (CUR-003) to build the prerequisite graph before adding a new edge.
   */
  @Query("SELECT r FROM CourseRequirementJpaEntity r JOIN CourseJpaEntity c ON r.targetCourseId = c.id WHERE c.planId = :planId")
  List<CourseRequirementJpaEntity> findAllByPlanId(UUID planId);
}
