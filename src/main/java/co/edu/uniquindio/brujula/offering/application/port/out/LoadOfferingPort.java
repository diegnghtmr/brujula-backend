package co.edu.uniquindio.brujula.offering.application.port.out;

import co.edu.uniquindio.brujula.offering.application.dto.ListOfferingsCriteria;
import co.edu.uniquindio.brujula.offering.application.dto.OfferingPageView;
import co.edu.uniquindio.brujula.offering.domain.CourseOffering;
import co.edu.uniquindio.brujula.offering.domain.CourseOfferingId;

import java.util.Optional;

/** Secondary port: load course offering data from persistence. */
public interface LoadOfferingPort {

  /**
   * Returns a page of published offerings matching the criteria.
   * Only PRELIMINARY and FINAL statuses are included.
   */
  OfferingPageView findPublished(ListOfferingsCriteria criteria);

  /** Returns the offering with the given ID, or empty if not found. */
  Optional<CourseOffering> findById(CourseOfferingId id);
}
