package co.edu.uniquindio.brujula.administration.application.service;

import co.edu.uniquindio.brujula.administration.application.port.in.spi.IsStaffAssignedToProgramQuery;
import co.edu.uniquindio.brujula.administration.application.port.out.LoadStaffProgramAssignmentsPort;
import java.util.Set;
import java.util.UUID;

/**
 * Application service: checks whether a staff member has an active assignment to a program.
 *
 * <p>Implements {@link IsStaffAssignedToProgramQuery} published via {@code administration::spi}.
 *
 * <p>Framework-free: no Spring or JPA dependencies. Wired via {@code @Bean} in {@link
 * co.edu.uniquindio.brujula.administration.infrastructure.configuration.AdministrationConfiguration}.
 *
 * <p>Only active assignments ({@code active = true}) are considered.
 */
public class IsStaffAssignedToProgramService implements IsStaffAssignedToProgramQuery {

  private final LoadStaffProgramAssignmentsPort loadAssignmentsPort;

  public IsStaffAssignedToProgramService(LoadStaffProgramAssignmentsPort loadAssignmentsPort) {
    this.loadAssignmentsPort = loadAssignmentsPort;
  }

  @Override
  public boolean isAssigned(UUID staffId, UUID programId) {
    return loadAssignmentsPort.findActiveAssignmentsByStaffId(staffId).contains(programId);
  }

  @Override
  public Set<UUID> assignedPrograms(UUID staffId) {
    return loadAssignmentsPort.findActiveAssignmentsByStaffId(staffId);
  }
}
