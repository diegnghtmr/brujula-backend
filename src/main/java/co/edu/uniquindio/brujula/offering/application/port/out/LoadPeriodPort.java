package co.edu.uniquindio.brujula.offering.application.port.out;

import co.edu.uniquindio.brujula.offering.domain.AcademicPeriod;
import co.edu.uniquindio.brujula.offering.domain.AcademicPeriodId;

import java.util.List;
import java.util.Optional;

/** Secondary port: load academic period data from persistence. */
public interface LoadPeriodPort {

  /** Returns all non-archived periods ordered by startDate descending. */
  List<AcademicPeriod> findAllNonArchived();

  /** Returns the period with the given ID, or empty if not found. */
  Optional<AcademicPeriod> findById(AcademicPeriodId id);
}
