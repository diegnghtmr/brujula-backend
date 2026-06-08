package co.edu.uniquindio.brujula.offering.application.port.in;

import co.edu.uniquindio.brujula.offering.application.dto.AcademicPeriodView;

import java.util.UUID;

/** Primary port: resolve an academic period by ID (used by simulator). */
public interface ResolvePeriodQuery {

  /**
   * Returns the period identified by {@code periodId}.
   *
   * @param periodId the period UUID
   * @return academic period view
   * @throws co.edu.uniquindio.brujula.offering.domain.PeriodNotFoundException if not found
   */
  AcademicPeriodView resolvePeriod(UUID periodId);
}
