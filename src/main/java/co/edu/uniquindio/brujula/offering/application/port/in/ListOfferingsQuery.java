package co.edu.uniquindio.brujula.offering.application.port.in;

import co.edu.uniquindio.brujula.offering.application.dto.ListOfferingsCriteria;
import co.edu.uniquindio.brujula.offering.application.dto.OfferingPageView;

/** Primary port: list published offerings for a period (student view). */
public interface ListOfferingsQuery {

  /**
   * Returns a page of published offerings matching the given criteria.
   *
   * @param criteria filter and pagination parameters
   * @return paged view of offering summaries
   */
  OfferingPageView listOfferings(ListOfferingsCriteria criteria);
}
