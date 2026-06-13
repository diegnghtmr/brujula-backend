package co.edu.uniquindio.brujula.imports.application.port.in;

import co.edu.uniquindio.brujula.imports.application.dto.ImportPageView;
import co.edu.uniquindio.brujula.imports.application.dto.ListImportsQuery;

/**
 * Input port: list imports for the calling actor.
 *
 * <p>STAFF actors receive only batches belonging to their assigned programs. ADMIN actors receive
 * all batches.
 *
 * <p>Published in the imports module. No Spring/JPA imports.
 */
public interface ListImportsUseCase {

  /**
   * List import batches scoped to the actor's assigned programs.
   *
   * @param query query parameters including actor identity, optional filters, and pagination
   * @return paginated view of import summaries
   */
  ImportPageView list(ListImportsQuery query);
}
