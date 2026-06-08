package co.edu.uniquindio.brujula.imports.infrastructure.adapter.out.administration;

import co.edu.uniquindio.brujula.administration.application.port.in.spi.IsStaffAssignedToProgramQuery;
import co.edu.uniquindio.brujula.administration.application.port.in.spi.ResolveStaffByEmailQuery;
import co.edu.uniquindio.brujula.imports.application.port.out.StaffAssignmentPort;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Anti-corruption adapter: bridges imports' {@link StaffAssignmentPort} to administration's
 * published {@code administration::spi} queries (ADR-F6-005, IMP-010).
 *
 * <p>Mirrors the {@code CreditLimitAdapter} pattern in the simulation module.
 *
 * <p>ADMIN bypass is enforced by the calling service layer (UploadImportService /
 * ConfirmImportService), NOT by this adapter. This adapter is role-agnostic and purely translates
 * the imports-local port contract to the administration SPI.
 */
@Component
class StaffAssignmentAdapter implements StaffAssignmentPort {

  private final IsStaffAssignedToProgramQuery isAssignedQuery;
  private final ResolveStaffByEmailQuery resolveByEmailQuery;

  StaffAssignmentAdapter(
      IsStaffAssignedToProgramQuery isAssignedQuery,
      ResolveStaffByEmailQuery resolveByEmailQuery) {
    this.isAssignedQuery = isAssignedQuery;
    this.resolveByEmailQuery = resolveByEmailQuery;
  }

  @Override
  public boolean isAssigned(UUID staffId, UUID programId) {
    return isAssignedQuery.isAssigned(staffId, programId);
  }

  @Override
  public Optional<UUID> resolveStaffId(String email) {
    return resolveByEmailQuery.resolveStaffId(email);
  }
}
