package co.edu.uniquindio.brujula.offering.application.port.in;

import co.edu.uniquindio.brujula.offering.application.dto.AcademicPeriodView;
import java.util.List;

/** Primary port: list all non-archived academic periods. */
public interface ListPeriodsQuery {

  /**
   * Returns periods filtered by status. If {@code statusFilter} is null, returns all non-ARCHIVED
   * periods. Otherwise returns periods matching the given status string.
   *
   * @param statusFilter optional status filter (e.g. "ACTIVE"); null returns all non-archived
   */
  List<AcademicPeriodView> listPeriods(String statusFilter);
}
