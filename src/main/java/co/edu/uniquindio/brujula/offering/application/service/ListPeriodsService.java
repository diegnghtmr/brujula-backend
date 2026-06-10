package co.edu.uniquindio.brujula.offering.application.service;

import co.edu.uniquindio.brujula.offering.application.dto.AcademicPeriodView;
import co.edu.uniquindio.brujula.offering.application.port.in.ListPeriodsQuery;
import co.edu.uniquindio.brujula.offering.application.port.out.LoadPeriodPort;
import co.edu.uniquindio.brujula.offering.domain.AcademicPeriod;
import java.util.List;

/**
 * Use case: list all non-archived academic periods.
 *
 * <p>Framework-free. Satisfies: OFF-001.
 */
public class ListPeriodsService implements ListPeriodsQuery {

  private final LoadPeriodPort loadPeriodPort;

  public ListPeriodsService(LoadPeriodPort loadPeriodPort) {
    this.loadPeriodPort = loadPeriodPort;
  }

  @Override
  public List<AcademicPeriodView> listPeriods(String statusFilter) {
    return loadPeriodPort.findAllNonArchived().stream()
        .filter(p -> statusFilter == null || p.getStatus().name().equals(statusFilter))
        .map(this::toView)
        .toList();
  }

  private AcademicPeriodView toView(AcademicPeriod period) {
    return new AcademicPeriodView(
        period.getId().value(),
        period.getName(),
        period.getStartDate(),
        period.getEndDate(),
        period.getStatus().name(),
        period.getVersion());
  }
}
