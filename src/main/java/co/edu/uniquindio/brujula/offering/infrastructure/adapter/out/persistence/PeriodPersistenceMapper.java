package co.edu.uniquindio.brujula.offering.infrastructure.adapter.out.persistence;

import co.edu.uniquindio.brujula.offering.domain.AcademicPeriod;
import co.edu.uniquindio.brujula.offering.domain.AcademicPeriodId;
import co.edu.uniquindio.brujula.offering.domain.PeriodStatus;

/**
 * Mapper: {@link AcademicPeriodJpaEntity} ↔ {@link AcademicPeriod} domain entity.
 *
 * <p>Package-private per ARQ-006b. Pure translation — no business logic.
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
}
