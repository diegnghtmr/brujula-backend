package co.edu.uniquindio.brujula.administration.application.port.in;

import co.edu.uniquindio.brujula.administration.application.dto.StaffAccountView;
import co.edu.uniquindio.brujula.administration.application.dto.StaffPage;
import java.util.Optional;
import java.util.UUID;
import org.springframework.lang.Nullable;

/**
 * Input port: query staff accounts.
 *
 * <p>Returns application-layer DTOs ({@link StaffAccountView}) and accepts a status name string so
 * REST adapters do not need to import domain types (ARQ-001).
 *
 * <p>Satisfies: Design §2.2 GetStaffQuery/ListStaffQuery.
 */
public interface ListStaffQuery {

  /**
   * List staff accounts with optional status filter, paginated.
   *
   * @param statusName optional status name filter (ACTIVE | INACTIVE); null means no filter
   * @param page zero-based page number
   * @param size page size
   * @return paginated result
   */
  StaffPage listStaff(@Nullable String statusName, int page, int size);

  /**
   * Find a single staff account by id.
   *
   * @param staffId the staff account UUID
   * @return the account view, or empty if not found
   */
  Optional<StaffAccountView> getById(UUID staffId);
}
