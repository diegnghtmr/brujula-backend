package co.edu.uniquindio.brujula.offering.infrastructure.adapter.out.persistence;

import co.edu.uniquindio.brujula.offering.domain.CourseOffering;
import co.edu.uniquindio.brujula.offering.domain.CourseOfferingId;
import co.edu.uniquindio.brujula.offering.domain.OfferingStatus;
import co.edu.uniquindio.brujula.offering.domain.ScheduleOption;
import co.edu.uniquindio.brujula.offering.domain.ScheduleOptionId;
import co.edu.uniquindio.brujula.offering.domain.ScheduleSession;
import co.edu.uniquindio.brujula.offering.domain.Weekday;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Mapper: {@link CourseOfferingJpaEntity} ↔ {@link CourseOffering} domain entity.
 *
 * <p>Package-private. Pure translation — no business logic. Supports both read (toDomain) and write
 * (toJpaEntity) directions.
 */
class OfferingPersistenceMapper {

  /**
   * Map a domain {@link CourseOffering} to a JPA entity for persistence.
   *
   * <p>Builds child {@link ScheduleOptionJpaEntity} list from domain options, each with their
   * {@link ScheduleSessionJpaEntity} children; wires back-references ({@code
   * option.setOffering(parent)}, {@code session.setOption(option)}). Sets {@code version} from the
   * domain object so Hibernate's {@code @Version} optimistic-lock detection fires correctly on
   * update.
   */
  CourseOfferingJpaEntity toJpaEntity(CourseOffering domain) {
    CourseOfferingJpaEntity entity = new CourseOfferingJpaEntity();
    entity.setId(domain.getId().value());
    entity.setCourseId(domain.getCourseId());
    entity.setCourseName(domain.getCourseName());
    entity.setProgramId(domain.getProgramId());
    entity.setPeriodId(domain.getPeriodId());
    entity.setStatus(domain.getStatus().name());
    entity.setNotes(domain.getNotes());
    entity.setVersion(domain.getVersion());

    Set<ScheduleOptionJpaEntity> optionEntities = new LinkedHashSet<>();
    for (ScheduleOption option : domain.getScheduleOptions()) {
      ScheduleOptionJpaEntity optionEntity = toOptionJpaEntity(option, entity);
      optionEntities.add(optionEntity);
    }
    entity.setScheduleOptions(optionEntities);
    return entity;
  }

  private ScheduleOptionJpaEntity toOptionJpaEntity(
      ScheduleOption option, CourseOfferingJpaEntity parent) {
    ScheduleOptionJpaEntity entity = new ScheduleOptionJpaEntity();
    entity.setId(option.getId().value());
    entity.setOffering(parent);
    entity.setLabel(option.getLabel());
    entity.setJourneyCode(option.getJourneyCode());
    entity.setVersion(option.getVersion());

    Set<ScheduleSessionJpaEntity> sessionEntities = new LinkedHashSet<>();
    for (ScheduleSession session : option.getSessions()) {
      ScheduleSessionJpaEntity sessionEntity = toSessionJpaEntity(session, entity);
      sessionEntities.add(sessionEntity);
    }
    entity.setSessions(sessionEntities);
    return entity;
  }

  private ScheduleSessionJpaEntity toSessionJpaEntity(
      ScheduleSession session, ScheduleOptionJpaEntity optionParent) {
    ScheduleSessionJpaEntity entity = new ScheduleSessionJpaEntity();
    entity.setId(session.id());
    entity.setOption(optionParent);
    entity.setDay(session.day().name());
    entity.setStartsAt(session.startsAt());
    entity.setEndsAt(session.endsAt());
    return entity;
  }

  CourseOffering toDomain(CourseOfferingJpaEntity entity) {
    List<ScheduleOption> options =
        entity.getScheduleOptions().stream().map(this::toOptionDomain).toList();
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
    List<ScheduleSession> sessions =
        entity.getSessions().stream().map(this::toSessionDomain).toList();
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
        entity.getId(), Weekday.valueOf(entity.getDay()), entity.getStartsAt(), entity.getEndsAt());
  }
}
