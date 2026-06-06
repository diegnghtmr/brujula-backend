package co.edu.uniquindio.brujula.administration.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.edu.uniquindio.brujula.administration.application.dto.CreateStaffCommand;
import co.edu.uniquindio.brujula.administration.application.dto.StaffAccountView;
import co.edu.uniquindio.brujula.administration.application.port.out.IdempotencyStorePort;
import co.edu.uniquindio.brujula.administration.application.port.out.LoadAllowedEmailDomainsPort;
import co.edu.uniquindio.brujula.administration.application.port.out.LoadStaffPort;
import co.edu.uniquindio.brujula.administration.application.port.out.SaveStaffPort;
import co.edu.uniquindio.brujula.administration.application.service.CreateStaffService;
import co.edu.uniquindio.brujula.administration.application.service.IdempotencyKeyConflictException;
import co.edu.uniquindio.brujula.administration.domain.StaffAccount;
import co.edu.uniquindio.brujula.audit.application.port.in.AuditRecorder;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link CreateStaffService} (TDD RED phase, zero Spring).
 *
 * <p>Tests cover: happy path creation, idempotency replay, idempotency conflict, domain validation.
 */
class CreateStaffServiceTest {

  private LoadStaffPort loadStaffPort;
  private SaveStaffPort saveStaffPort;
  private LoadAllowedEmailDomainsPort loadAllowedEmailDomainsPort;
  private IdempotencyStorePort idempotencyStorePort;
  private AuditRecorder auditRecorder;
  private CreateStaffService service;

  @BeforeEach
  void setUp() {
    loadStaffPort = mock(LoadStaffPort.class);
    saveStaffPort = mock(SaveStaffPort.class);
    loadAllowedEmailDomainsPort = mock(LoadAllowedEmailDomainsPort.class);
    idempotencyStorePort = mock(IdempotencyStorePort.class);
    auditRecorder = mock(AuditRecorder.class);
    service =
        new CreateStaffService(
            loadStaffPort,
            saveStaffPort,
            loadAllowedEmailDomainsPort,
            idempotencyStorePort,
            auditRecorder);
  }

  @Test
  void create_staff_happy_path_persists_account_and_records_audit() {
    UUID idempotencyKey = UUID.randomUUID();
    String email = "alice@uniquindio.edu.co";
    String requestHash = "hash1";

    when(loadAllowedEmailDomainsPort.loadAllowedDomains()).thenReturn(Set.of("uniquindio.edu.co"));
    when(loadStaffPort.findByEmail(email)).thenReturn(Optional.empty());
    when(idempotencyStorePort.findByKeyAndOperation(idempotencyKey, "CREATE_STAFF"))
        .thenReturn(Optional.empty());

    StaffAccount saved = StaffAccount.create(email, "Alice Smith");
    when(saveStaffPort.save(any())).thenReturn(saved);

    var command =
        new CreateStaffCommand(
            idempotencyKey, UUID.randomUUID(), email, "Alice Smith", requestHash);
    StaffAccountView result = service.createStaff(command);

    assertThat(result).isNotNull();
    assertThat(result.email()).isEqualTo(email);
    verify(saveStaffPort).save(any());
    verify(auditRecorder).record(argThat(cmd -> "STAFF_CREATED".equals(cmd.action())));
    verify(idempotencyStorePort)
        .save(eq(idempotencyKey), eq("CREATE_STAFF"), eq(requestHash), any());
  }

  @Test
  void create_staff_idempotent_replay_returns_existing_result() {
    UUID idempotencyKey = UUID.randomUUID();
    String email = "bob@uniquindio.edu.co";
    String requestHash = "hash1";

    StaffAccount existing = StaffAccount.create(email, "Bob Jones");
    when(idempotencyStorePort.findByKeyAndOperation(idempotencyKey, "CREATE_STAFF"))
        .thenReturn(Optional.of(new IdempotencyStorePort.StoredResult(requestHash, existing)));

    var command =
        new CreateStaffCommand(idempotencyKey, UUID.randomUUID(), email, "Bob Jones", requestHash);
    StaffAccountView result = service.createStaff(command);

    assertThat(result.email()).isEqualTo(email);
    verify(saveStaffPort, never()).save(any());
    verify(auditRecorder, never()).record(any());
  }

  @Test
  void create_staff_idempotency_conflict_throws_exception() {
    UUID idempotencyKey = UUID.randomUUID();
    String email = "carol@uniquindio.edu.co";

    StaffAccount existing = StaffAccount.create(email, "Carol White");
    when(idempotencyStorePort.findByKeyAndOperation(idempotencyKey, "CREATE_STAFF"))
        .thenReturn(Optional.of(new IdempotencyStorePort.StoredResult("different-hash", existing)));

    var command =
        new CreateStaffCommand(idempotencyKey, UUID.randomUUID(), email, "Carol White", "new-hash");
    assertThatThrownBy(() -> service.createStaff(command))
        .isInstanceOf(IdempotencyKeyConflictException.class);
    verify(saveStaffPort, never()).save(any());
  }

  @Test
  void create_staff_rejects_disallowed_email_domain() {
    UUID idempotencyKey = UUID.randomUUID();
    when(loadAllowedEmailDomainsPort.loadAllowedDomains()).thenReturn(Set.of("uniquindio.edu.co"));
    when(idempotencyStorePort.findByKeyAndOperation(idempotencyKey, "CREATE_STAFF"))
        .thenReturn(Optional.empty());

    var command =
        new CreateStaffCommand(
            idempotencyKey, UUID.randomUUID(), "person@gmail.com", "Person", "hash");
    assertThatThrownBy(() -> service.createStaff(command))
        .isInstanceOf(co.edu.uniquindio.brujula.identity.domain.DomainNotAllowedException.class);
    verify(saveStaffPort, never()).save(any());
  }
}
