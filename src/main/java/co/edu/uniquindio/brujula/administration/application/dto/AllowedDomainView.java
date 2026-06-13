package co.edu.uniquindio.brujula.administration.application.dto;

import java.util.UUID;

/**
 * Application-layer DTO for an allowed email domain.
 *
 * <p>Moved from {@code ManageAllowedEmailDomainsUseCase.AllowedDomain} because types in {@code
 * application.port.in} must be interfaces only — no records/classes allowed.
 *
 * @param id the domain UUID
 * @param domain the normalized hostname
 */
public record AllowedDomainView(UUID id, String domain) {}
