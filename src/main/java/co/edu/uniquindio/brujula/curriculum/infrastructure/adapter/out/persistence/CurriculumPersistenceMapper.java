package co.edu.uniquindio.brujula.curriculum.infrastructure.adapter.out.persistence;

import co.edu.uniquindio.brujula.curriculum.domain.Confidence;
import co.edu.uniquindio.brujula.curriculum.domain.Course;
import co.edu.uniquindio.brujula.curriculum.domain.CourseId;
import co.edu.uniquindio.brujula.curriculum.domain.CourseRequirement;
import co.edu.uniquindio.brujula.curriculum.domain.CourseRequirementId;
import co.edu.uniquindio.brujula.curriculum.domain.CourseRequirementStatus;
import co.edu.uniquindio.brujula.curriculum.domain.CourseStatus;
import co.edu.uniquindio.brujula.curriculum.domain.CourseType;
import co.edu.uniquindio.brujula.curriculum.domain.DegreeWorkModality;
import co.edu.uniquindio.brujula.curriculum.domain.DegreeWorkModalityId;
import co.edu.uniquindio.brujula.curriculum.domain.DegreeWorkModalityStatus;
import co.edu.uniquindio.brujula.curriculum.domain.RecommendedSemester;
import co.edu.uniquindio.brujula.curriculum.domain.RequirementType;
import co.edu.uniquindio.brujula.curriculum.domain.SequentialTrack;
import co.edu.uniquindio.brujula.curriculum.domain.SequentialTrackId;
import co.edu.uniquindio.brujula.curriculum.domain.SequentialTrackLevel;
import co.edu.uniquindio.brujula.curriculum.domain.TrackKind;
import java.util.List;

/**
 * Pure-translation mapper between persistence entities and domain objects for the curriculum module.
 *
 * <p>Package-private (ARQ-006). Stateless — all methods are static.
 * No business logic here — pure field mapping only.
 */
final class CurriculumPersistenceMapper {

  private CurriculumPersistenceMapper() {}

  // ---------------------------------------------------------------------------
  // Course
  // ---------------------------------------------------------------------------

  static Course toDomain(CourseJpaEntity entity) {
    List<RecommendedSemester> semesters = entity.getRecommendedSemesters().stream()
        .map(s -> new RecommendedSemester(s.getJourneyCode(), s.getSemester()))
        .toList();
    return Course.reconstitute(
        new CourseId(entity.getId()),
        entity.getPlanId(),
        entity.getProgramId(),
        entity.getOfficialCode(),
        entity.getName(),
        CourseType.valueOf(entity.getType()),
        entity.getCredits(),
        entity.getCurricularWeeklyHours(),
        semesters,
        CourseStatus.valueOf(entity.getStatus()),
        entity.getVersion());
  }

  // ---------------------------------------------------------------------------
  // CourseRequirement
  // ---------------------------------------------------------------------------

  static CourseRequirement toDomain(CourseRequirementJpaEntity entity) {
    return CourseRequirement.reconstitute(
        new CourseRequirementId(entity.getId()),
        entity.getTargetCourseId(),
        RequirementType.valueOf(entity.getType()),
        entity.getRequiredCourseId(),
        entity.getRequiredCredits(),
        entity.getSimultaneityAllowed(),
        entity.getConfidence() != null ? Confidence.valueOf(entity.getConfidence()) : null,
        entity.getInferred(),
        CourseRequirementStatus.valueOf(entity.getStatus()),
        entity.getVersion());
  }

  // ---------------------------------------------------------------------------
  // SequentialTrack
  // ---------------------------------------------------------------------------

  static SequentialTrack toDomain(SequentialTrackJpaEntity entity) {
    List<SequentialTrackLevel> levels = entity.getLevels().stream()
        .map(CurriculumPersistenceMapper::toLevelDomain)
        .toList();
    return SequentialTrack.reconstitute(
        new SequentialTrackId(entity.getId()),
        entity.getPlanId(),
        TrackKind.valueOf(entity.getKind()),
        entity.getLabel(),
        entity.isConsecutiveSimultaneityAllowed(),
        entity.getGatekeeperCourseId(),
        levels,
        entity.getVersion());
  }

  private static SequentialTrackLevel toLevelDomain(SequentialTrackLevelJpaEntity entity) {
    List<java.util.UUID> optionIds = entity.getOptions().stream()
        .map(SequentialTrackLevelOptionJpaEntity::getCourseId)
        .toList();
    return new SequentialTrackLevel(entity.getId(), entity.getLevel(), optionIds);
  }

  // ---------------------------------------------------------------------------
  // DegreeWorkModality
  // ---------------------------------------------------------------------------

  static DegreeWorkModality toDomain(DegreeWorkModalityJpaEntity entity) {
    return DegreeWorkModality.reconstitute(
        new DegreeWorkModalityId(entity.getId()),
        entity.getPlanId(),
        entity.getName(),
        entity.getCredits(),
        entity.getCurricularWeeklyHours(),
        DegreeWorkModalityStatus.valueOf(entity.getStatus()),
        entity.getVersion());
  }
}
