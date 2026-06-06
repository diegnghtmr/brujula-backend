package co.edu.uniquindio.brujula.administration.application.service;

import co.edu.uniquindio.brujula.administration.application.dto.StaffAccountView;
import co.edu.uniquindio.brujula.administration.application.dto.StaffPage;
import co.edu.uniquindio.brujula.administration.application.port.in.ListStaffQuery;
import co.edu.uniquindio.brujula.administration.application.port.out.LoadStaffPort;
import co.edu.uniquindio.brujula.administration.domain.StaffAccount;
import co.edu.uniquindio.brujula.administration.domain.StaffAccountStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.lang.Nullable;

/**
 * Query service: list and find staff accounts.
 *
 * <p>Converts the status name string to a domain enum internally, so REST adapters using {@link
 * ListStaffQuery} do not need to import domain types (ARQ-001).
 *
 * <p>Framework-free. Satisfies: Design §2.2 GetStaffQuery/ListStaffQuery.
 */
public class ListStaffService implements ListStaffQuery {

  private final LoadStaffPort loadStaffPort;

  public ListStaffService(LoadStaffPort loadStaffPort) {
    this.loadStaffPort = loadStaffPort;
  }

  @Override
  public StaffPage listStaff(@Nullable String statusName, int page, int size) {
    StaffAccountStatus status = statusName != null ? StaffAccountStatus.valueOf(statusName) : null;
    return loadStaffPort.findAll(status, page, size);
  }

  @Override
  public Optional<StaffAccountView> getById(UUID staffId) {
    return loadStaffPort.findById(staffId).map(this::toView);
  }

  private StaffAccountView toView(StaffAccount account) {
    return new StaffAccountView(
        account.getId(),
        account.getEmail(),
        account.getFullName(),
        account.getStatus().name(),
        account.getVersion());
  }
}
