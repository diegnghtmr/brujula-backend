package co.edu.uniquindio.brujula.offering.application.port.out;

import co.edu.uniquindio.brujula.offering.application.dto.ListOfferingsCriteria;
import co.edu.uniquindio.brujula.offering.application.dto.OfferingPageView;
import co.edu.uniquindio.brujula.offering.domain.CourseOffering;
import co.edu.uniquindio.brujula.offering.domain.CourseOfferingId;
import java.util.Optional;

/** Secondary port: load course offering data from persistence. */
public interface LoadOfferingPort {

  /**
   * Returns a page of published offerings matching the criteria. Only PRELIMINARY and FINAL
   * statuses are included (student-visible).
   */
  OfferingPageView findPublished(ListOfferingsCriteria criteria);

  /**
   * Returns a page of offerings for privileged callers (STAFF/ADMIN). Includes DRAFT, PRELIMINARY,
   * and FINAL statuses (excludes ARCHIVED by default). The status filter in criteria is applied if
   * present.
   *
   * <p>Satisfies: OFFW-007.
   */
  OfferingPageView findForStaff(ListOfferingsCriteria criteria);

  /** Returns the offering with the given ID, or empty if not found. */
  Optional<CourseOffering> findById(CourseOfferingId id);
}
