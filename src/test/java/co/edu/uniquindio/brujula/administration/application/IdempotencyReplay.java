package co.edu.uniquindio.brujula.administration.application;

import co.edu.uniquindio.brujula.administration.application.dto.StaffAccountView;

/**
 * Test-only helper: represents a stored idempotency replay result for administration tests.
 *
 * <p>In production the adapter returns {@link
 * co.edu.uniquindio.brujula.sharedkernel.application.port.out.IdempotencyStorePort.StoredRecord};
 * callers (e.g. {@link
 * co.edu.uniquindio.brujula.administration.application.service.CreateStaffService}) own the
 * (de)serialization of the view.
 */
public record IdempotencyReplay(String requestHash, StaffAccountView storedView) {}
