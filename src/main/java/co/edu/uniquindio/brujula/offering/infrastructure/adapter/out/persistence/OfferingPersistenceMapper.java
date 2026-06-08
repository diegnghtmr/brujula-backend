package co.edu.uniquindio.brujula.offering.infrastructure.adapter.out.persistence;

import co.edu.uniquindio.brujula.offering.domain.CourseOffering;
import co.edu.uniquindio.brujula.offering.domain.CourseOfferingId;
import co.edu.uniquindio.brujula.offering.domain.OfferingStatus;
import co.edu.uniquindio.brujula.offering.domain.ScheduleOption;
import co.edu.uniquindio.brujula.offering.domain.ScheduleOptionId;
import co.edu.uniquindio.brujula.offering.domain.ScheduleSession;
import co.edu.uniquindio.brujula.offering.domain.Weekday;

import java.util.List;

/**
 * Mapper: {@link CourseOfferingJpaEntity} ↔ {@link CourseOffering} domain entity.
 *
 * <p>Package-private per ARQ-006b. Pure translation — no business logic.
 */
class OfferingPersistenceMapper {

  CourseOffering toDomain(CourseOfferingJpaEntity entity) {
    List<ScheduleOption> options = entity.getScheduleOptions().stream()
        .map(this::toOptionDomain)
        .toList();
    return CourseOffering.reconstitute(
        CourseOfferingId.of(entity.getId()),
        entity.getCourseId(),
        entity.getCourseName(),
        entity.getProgramId(),
        entity.getPeriodId(),
        OfferingStatus.valueOf(entity.getStatus()),
        entity.getNotes(),
        options,
        entity.getVersion());
  }

  private ScheduleOption toOptionDomain(ScheduleOptionJpaEntity entity) {
    List<ScheduleSession> sessions = entity.getSessions().stream()
        .map(this::toSessionDomain)
        .toList();
    return ScheduleOption.reconstitute(
        ScheduleOptionId.of(entity.getId()),
        entity.getOffering().getId(),
        entity.getLabel(),
        entity.getJourneyCode(),
        sessions,
        entity.getVersion());
  }

  private ScheduleSession toSessionDomain(ScheduleSessionJpaEntity entity) {
    return new ScheduleSession(
        entity.getId(),
        Weekday.valueOf(entity.getDay()),
        entity.getStartsAt(),
        entity.getEndsAt());
  }
}
