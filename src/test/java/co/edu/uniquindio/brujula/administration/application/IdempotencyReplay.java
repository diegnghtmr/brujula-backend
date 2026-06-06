package co.edu.uniquindio.brujula.administration.application;

import co.edu.uniquindio.brujula.administration.domain.StaffAccount;

/**
 * Test-only helper: represents a stored idempotency replay result.
 *
 * <p>In production the adapter returns this shape via {@link
 * co.edu.uniquindio.brujula.administration.application.port.out.IdempotencyStorePort}.
 */
public record IdempotencyReplay(String requestHash, StaffAccount storedResult) {}
