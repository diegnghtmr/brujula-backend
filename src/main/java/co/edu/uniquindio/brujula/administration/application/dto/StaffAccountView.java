package co.edu.uniquindio.brujula.administration.application.dto;

import java.util.UUID;

/**
 * Application-layer read-side projection for a staff account.
 *
 * <p>Used in paginated list results so REST adapters do not need to import domain types. The {@code
 * status} field is the string name of the {@code
 * co.edu.uniquindio.brujula.administration.domain.StaffAccountStatus} enum.
 *
 * @param id the staff account UUID
 * @param email institutional email
 * @param fullName display name
 * @param status status name (ACTIVE | INACTIVE)
 * @param version optimistic locking version
 */
public record StaffAccountView(
    UUID id, String email, String fullName, String status, long version) {}
