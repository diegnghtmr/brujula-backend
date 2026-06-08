package co.edu.uniquindio.brujula.offering.application.service;

import co.edu.uniquindio.brujula.offering.application.dto.AcademicPeriodView;
import co.edu.uniquindio.brujula.offering.application.port.in.ResolvePeriodQuery;
import co.edu.uniquindio.brujula.offering.application.port.out.LoadPeriodPort;
import co.edu.uniquindio.brujula.offering.domain.AcademicPeriod;
import co.edu.uniquindio.brujula.offering.domain.AcademicPeriodId;
import co.edu.uniquindio.brujula.offering.domain.PeriodNotFoundException;

import java.util.UUID;

/**
 * Use case: resolve a period by ID for internal use (e.g., simulator).
 *
 * <p>Framework-free. Satisfies: OFF-004.
 */
public class ResolvePeriodService implements ResolvePeriodQuery {

  private final LoadPeriodPort loadPeriodPort;

  public ResolvePeriodService(LoadPeriodPort loadPeriodPort) {
    this.loadPeriodPort = loadPeriodPort;
  }

  @Override
  public AcademicPeriodView resolvePeriod(UUID periodId) {
    AcademicPeriod period = loadPeriodPort
        .findById(new AcademicPeriodId(periodId))
        .orElseThrow(() -> new PeriodNotFoundException(new AcademicPeriodId(periodId)));
    return new AcademicPeriodView(
        period.getId().value(),
        period.getName(),
        period.getStartDate(),
        period.getEndDate(),
        period.getStatus().name(),
        period.getVersion());
  }
}
