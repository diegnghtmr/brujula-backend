package co.edu.uniquindio.brujula.offering.infrastructure.adapter.out.persistence;

import co.edu.uniquindio.brujula.offering.domain.AcademicPeriod;
import co.edu.uniquindio.brujula.offering.domain.AcademicPeriodId;
import co.edu.uniquindio.brujula.offering.domain.PeriodStatus;

/**
 * Mapper: {@link AcademicPeriodJpaEntity} ↔ {@link AcademicPeriod} domain entity.
 *
 * <p>Package-private. Pure translation — no business logic. Supports both read (toDomain) and write
 * (toJpaEntity) directions.
 */
class PeriodPersistenceMapper {

  AcademicPeriod toDomain(AcademicPeriodJpaEntity entity) {
    return AcademicPeriod.reconstitute(
        AcademicPeriodId.of(entity.getId()),
        entity.getName(),
        entity.getStartDate(),
        entity.getEndDate(),
        PeriodStatus.valueOf(entity.getStatus()),
        entity.getVersion());
  }

  /**
   * Map a domain {@link AcademicPeriod} to a JPA entity for persistence.
   *
   * <p>Sets {@code version} from the domain object so Hibernate's {@code @Version} optimistic-lock
   * detection fires correctly on update.
   */
  AcademicPeriodJpaEntity toJpaEntity(AcademicPeriod domain) {
    AcademicPeriodJpaEntity entity = new AcademicPeriodJpaEntity();
    entity.setId(domain.getId().value());
    entity.setName(domain.getName());
    entity.setStartDate(domain.getStartDate());
    entity.setEndDate(domain.getEndDate());
    entity.setStatus(domain.getStatus().name());
    entity.setVersion(domain.getVersion());
    return entity;
  }
}
