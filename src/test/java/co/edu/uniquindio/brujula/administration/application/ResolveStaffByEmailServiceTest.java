package co.edu.uniquindio.brujula.administration.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import co.edu.uniquindio.brujula.administration.application.port.in.spi.ResolveStaffByEmailQuery;
import co.edu.uniquindio.brujula.administration.application.port.out.LoadStaffPort;
import co.edu.uniquindio.brujula.administration.application.service.ResolveStaffByEmailService;
import co.edu.uniquindio.brujula.administration.domain.StaffAccount;
import co.edu.uniquindio.brujula.administration.domain.StaffAccountStatus;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link ResolveStaffByEmailService}.
 *
 * <p>Covers: SC-010-D (email lookup returns staffId), SC-010-E (unknown email → empty).
 */
class ResolveStaffByEmailServiceTest {

  private LoadStaffPort loadStaffPort;
  private ResolveStaffByEmailQuery service;

  @BeforeEach
  void setUp() {
    loadStaffPort = mock(LoadStaffPort.class);
    service = new ResolveStaffByEmailService(loadStaffPort);
  }

  /** SC-010-D: known email → returns the staff UUID. */
  @Test
  void email_lookup_returns_staff_id() {
    String email = "staff@uniquindio.edu.co";
    UUID staffId = UUID.randomUUID();
    StaffAccount account =
        StaffAccount.reconstitute(staffId, email, "Staff Member", StaffAccountStatus.ACTIVE, 0L);

    when(loadStaffPort.findByEmail(email)).thenReturn(Optional.of(account));

    Optional<UUID> result = service.resolveStaffId(email);
    assertThat(result).isPresent().contains(staffId);
  }

  /** SC-010-E: unknown email → empty Optional. */
  @Test
  void unknown_email_returns_empty() {
    String email = "unknown@uniquindio.edu.co";
    when(loadStaffPort.findByEmail(email)).thenReturn(Optional.empty());

    Optional<UUID> result = service.resolveStaffId(email);
    assertThat(result).isEmpty();
  }

  /** Inactive staff account is still resolved (resolution is ID lookup; authorization is caller's
   * responsibility to check status if needed). */
  @Test
  void inactive_staff_account_still_resolves() {
    String email = "inactive@uniquindio.edu.co";
    UUID staffId = UUID.randomUUID();
    StaffAccount account =
        StaffAccount.reconstitute(staffId, email, "Inactive Staff", StaffAccountStatus.INACTIVE, 1L);

    when(loadStaffPort.findByEmail(email)).thenReturn(Optional.of(account));

    Optional<UUID> result = service.resolveStaffId(email);
    assertThat(result).isPresent().contains(staffId);
  }

  /**
   * WARNING-1 closure: resolveActiveStaffId returns the UUID for an ACTIVE account.
   */
  @Test
  void resolve_active_staff_id_returns_uuid_for_active_account() {
    String email = "active@uniquindio.edu.co";
    UUID staffId = UUID.randomUUID();
    StaffAccount account =
        StaffAccount.reconstitute(staffId, email, "Active Staff", StaffAccountStatus.ACTIVE, 0L);

    when(loadStaffPort.findByEmail(email)).thenReturn(Optional.of(account));

    Optional<UUID> result = service.resolveActiveStaffId(email);
    assertThat(result).isPresent().contains(staffId);
  }

  /**
   * WARNING-1 closure (load-bearing): resolveActiveStaffId returns empty for an INACTIVE account.
   * An INACTIVE staff member must NOT resolve to a UUID — even if they have an ACTIVE assignment.
   * Removing the status filter in resolveActiveStaffId would cause this test to FAIL.
   */
  @Test
  void resolve_active_staff_id_returns_empty_for_inactive_account() {
    String email = "inactive@uniquindio.edu.co";
    UUID staffId = UUID.randomUUID();
    StaffAccount account =
        StaffAccount.reconstitute(staffId, email, "Inactive Staff", StaffAccountStatus.INACTIVE, 1L);

    when(loadStaffPort.findByEmail(email)).thenReturn(Optional.of(account));

    Optional<UUID> result = service.resolveActiveStaffId(email);
    assertThat(result).isEmpty();
  }

  /** WARNING-2: email normalization — mixed-case email is normalized to lowercase before lookup. */
  @Test
  void email_is_normalized_to_lowercase_before_lookup() {
    String rawEmail = "Staff@UNIQUINDIO.Edu.Co";
    String normalizedEmail = "staff@uniquindio.edu.co";
    UUID staffId = UUID.randomUUID();
    StaffAccount account =
        StaffAccount.reconstitute(staffId, normalizedEmail, "Staff", StaffAccountStatus.ACTIVE, 0L);

    when(loadStaffPort.findByEmail(normalizedEmail)).thenReturn(Optional.of(account));

    Optional<UUID> result = service.resolveStaffId(rawEmail);
    assertThat(result).isPresent().contains(staffId);
  }
}
