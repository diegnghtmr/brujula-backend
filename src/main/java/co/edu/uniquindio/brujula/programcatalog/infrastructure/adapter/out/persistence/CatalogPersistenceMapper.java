package co.edu.uniquindio.brujula.programcatalog.infrastructure.adapter.out.persistence;

import co.edu.uniquindio.brujula.programcatalog.domain.AcademicProgram;
import co.edu.uniquindio.brujula.programcatalog.domain.Journey;
import co.edu.uniquindio.brujula.programcatalog.domain.JourneyId;
import co.edu.uniquindio.brujula.programcatalog.domain.PlanId;
import co.edu.uniquindio.brujula.programcatalog.domain.PlanStatus;
import co.edu.uniquindio.brujula.programcatalog.domain.ProgramId;
import co.edu.uniquindio.brujula.programcatalog.domain.ProgramStatus;
import co.edu.uniquindio.brujula.programcatalog.domain.StudyPlan;

/**
 * Pure-translation mapper between persistence entities and domain objects for the catalog module.
 *
 * <p>Package-private (ARQ-006). Stateless — all methods are static.
 * No business logic here — pure field mapping only.
 */
final class CatalogPersistenceMapper {

  private CatalogPersistenceMapper() {}

  // ---------------------------------------------------------------------------
  // AcademicProgram
  // ---------------------------------------------------------------------------

  static AcademicProgram toDomain(AcademicProgramJpaEntity entity) {
    return AcademicProgram.reconstitute(
        new ProgramId(entity.getId()),
        entity.getName(),
        entity.getFaculty(),
        ProgramStatus.valueOf(entity.getStatus()),
        entity.getVersion());
  }

  // ---------------------------------------------------------------------------
  // StudyPlan
  // ---------------------------------------------------------------------------

  static StudyPlan toDomain(StudyPlanJpaEntity entity) {
    return StudyPlan.reconstitute(
        new PlanId(entity.getId()),
        entity.getProgramId(),
        entity.getName(),
        PlanStatus.valueOf(entity.getStatus()),
        entity.getTotalCredits(),
        entity.getReferenceYear(),
        entity.getVersion());
  }

  // ---------------------------------------------------------------------------
  // Journey
  // ---------------------------------------------------------------------------

  static Journey toDomain(JourneyJpaEntity entity) {
    return Journey.reconstitute(
        new JourneyId(entity.getId()),
        entity.getCode(),
        entity.getName(),
        entity.isActive());
  }
}
