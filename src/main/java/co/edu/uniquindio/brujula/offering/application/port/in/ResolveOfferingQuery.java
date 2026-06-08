package co.edu.uniquindio.brujula.offering.application.port.in;

import co.edu.uniquindio.brujula.offering.application.dto.OfferingDetailView;

import java.util.UUID;

/** Primary port: resolve a single offering by ID (used by simulator). */
public interface ResolveOfferingQuery {

  /**
   * Returns the full detail of an offering by ID (any status, used internally).
   *
   * @param offeringId the offering UUID
   * @return offering detail view
   * @throws co.edu.uniquindio.brujula.offering.domain.OfferingNotFoundException if not found
   */
  OfferingDetailView resolveOffering(UUID offeringId);
}
