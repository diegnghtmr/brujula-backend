package co.edu.uniquindio.brujula.progress.infrastructure.adapter.out.persistence;

import co.edu.uniquindio.brujula.progress.domain.AttemptId;
import co.edu.uniquindio.brujula.progress.domain.AttemptStatus;
import co.edu.uniquindio.brujula.progress.domain.CourseAttempt;
import co.edu.uniquindio.brujula.progress.domain.Grade;
import co.edu.uniquindio.brujula.progress.domain.SubscriptionId;

/**
 * Mapper: {@link CourseAttemptJpaEntity} ↔ {@link CourseAttempt} domain aggregate.
 *
 * <p>Package-private per ARQ-006b. Pure translation — no business logic.
 */
class CourseAttemptPersistenceMapper {

  CourseAttempt toDomain(CourseAttemptJpaEntity entity) {
    Grade grade = entity.getFinalGrade() != null ? new Grade(entity.getFinalGrade()) : null;
    return CourseAttempt.reconstitute(
        AttemptId.of(entity.getId()),
        SubscriptionId.of(entity.getSubscriptionId()),
        entity.getCourseId(),
        AttemptStatus.valueOf(entity.getStatus()),
        grade,
        entity.getPeriodId(),
        entity.getJourneyCode(),
        entity.getRegisteredAt(),
        entity.getVersion());
  }

  CourseAttemptJpaEntity toJpaEntity(CourseAttempt attempt) {
    CourseAttemptJpaEntity entity = new CourseAttemptJpaEntity();
    entity.setId(attempt.getId().value());
    entity.setSubscriptionId(attempt.getSubscriptionId().value());
    entity.setCourseId(attempt.getCourseId());
    entity.setStatus(attempt.getStatus().name());
    entity.setFinalGrade(attempt.getGrade() != null ? attempt.getGrade().value() : null);
    entity.setPeriodId(attempt.getPeriodId());
    entity.setJourneyCode(attempt.getJourneyCode());
    entity.setRegisteredAt(attempt.getRegisteredAt());
    entity.setVersion(attempt.getVersion());
    return entity;
  }
}
