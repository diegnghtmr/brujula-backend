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
import co.edu.uniquindio.brujula.administration.application.port.out.LoadAllowedEmailDomainsPort;
import co.edu.uniquindio.brujula.administration.application.port.out.LoadStaffPort;
import co.edu.uniquindio.brujula.administration.application.port.out.SaveStaffPort;
import co.edu.uniquindio.brujula.administration.application.service.CreateStaffService;
import co.edu.uniquindio.brujula.administration.application.service.IdempotencyKeyConflictException;
import co.edu.uniquindio.brujula.administration.domain.StaffAccount;
import co.edu.uniquindio.brujula.audit.application.port.in.AuditRecorder;
import co.edu.uniquindio.brujula.sharedkernel.application.port.out.IdempotencyStorePort;
import co.edu.uniquindio.brujula.sharedkernel.application.port.out.JsonCodecPort;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link CreateStaffService} (TDD, zero Spring).
 *
 * <p>Tests cover: happy path creation, idempotency replay, idempotency conflict, domain validation.
 * Wired against the generalized {@link IdempotencyStorePort} from {@code sharedkernel}
 * (ADR-F3-001) and {@link JsonCodecPort} (FIX-1: resolves WARNING-001, moves JSON I/O out of the
 * application layer). Scenario: CROSS-8-RELO.
 */
class CreateStaffServiceTest {

  private LoadStaffPort loadStaffPort;
  private SaveStaffPort saveStaffPort;
  private LoadAllowedEmailDomainsPort loadAllowedEmailDomainsPort;
  private IdempotencyStorePort idempotencyStorePort;
  private JsonCodecPort jsonCodecPort;
  private AuditRecorder auditRecorder;
  private CreateStaffService service;

  @BeforeEach
  void setUp() {
    loadStaffPort = mock(LoadStaffPort.class);
    saveStaffPort = mock(SaveStaffPort.class);
    loadAllowedEmailDomainsPort = mock(LoadAllowedEmailDomainsPort.class);
    idempotencyStorePort = mock(IdempotencyStorePort.class);
    jsonCodecPort = mock(JsonCodecPort.class);
    auditRecorder = mock(AuditRecorder.class);
    service =
        new CreateStaffService(
            loadStaffPort,
            saveStaffPort,
            loadAllowedEmailDomainsPort,
            idempotencyStorePort,
            jsonCodecPort,
            auditRecorder);
  }

  @Test
  void create_staff_happy_path_persists_account_and_records_audit() {
    UUID idempotencyKey = UUID.randomUUID();
    String email = "alice@uniquindio.edu.co";
    String requestHash = "hash1";

    when(loadAllowedEmailDomainsPort.loadAllowedDomains()).thenReturn(Set.of("uniquindio.edu.co"));
    when(loadStaffPort.findByEmail(email)).thenReturn(Optional.empty());
    when(idempotencyStorePort.find(idempotencyKey, "CREATE_STAFF")).thenReturn(Optional.empty());

    StaffAccount saved = StaffAccount.create(email, "Alice Smith");
    when(saveStaffPort.save(any())).thenReturn(saved);
    when(jsonCodecPort.toJson(any())).thenReturn("{\"id\":\"...\",\"email\":\"alice@uniquindio.edu.co\"}");

    var command =
        new CreateStaffCommand(
            idempotencyKey, UUID.randomUUID(), email, "Alice Smith", requestHash);
    StaffAccountView result = service.createStaff(command);

    assertThat(result).isNotNull();
    assertThat(result.email()).isEqualTo(email);
    verify(saveStaffPort).save(any());
    verify(auditRecorder).record(argThat(cmd -> "STAFF_CREATED".equals(cmd.action())));
    // Verify the codec is called for serialization
    verify(jsonCodecPort).toJson(any(StaffAccountView.class));
    // Verify the 5-arg idempotency save: key, operation, hash, status=201, JSON body
    verify(idempotencyStorePort).save(eq(idempotencyKey), eq("CREATE_STAFF"), eq(requestHash),
        eq(201), any(String.class));
  }

  @Test
  void create_staff_idempotent_replay_returns_existing_result() {
    UUID idempotencyKey = UUID.randomUUID();
    String email = "bob@uniquindio.edu.co";
    String requestHash = "hash1";
    UUID replayId = UUID.fromString("00000000-0000-0000-0000-000000000001");
    // The stored record contains JSON-serialized StaffAccountView
    String storedJson =
        "{\"id\":\"00000000-0000-0000-0000-000000000001\",\"email\":\""
            + email
            + "\",\"fullName\":\"Bob Jones\",\"status\":\"ACTIVE\",\"version\":0}";
    StaffAccountView replayView = new StaffAccountView(replayId, email, "Bob Jones", "ACTIVE", 0L);

    when(idempotencyStorePort.find(idempotencyKey, "CREATE_STAFF"))
        .thenReturn(
            Optional.of(new IdempotencyStorePort.StoredRecord(requestHash, 201, storedJson)));
    when(jsonCodecPort.fromJson(storedJson, StaffAccountView.class)).thenReturn(replayView);

    var command =
        new CreateStaffCommand(idempotencyKey, UUID.randomUUID(), email, "Bob Jones", requestHash);
    StaffAccountView result = service.createStaff(command);

    assertThat(result.id()).isEqualTo(replayId);
    assertThat(result.email()).isEqualTo(email);
    assertThat(result.fullName()).isEqualTo("Bob Jones");
    assertThat(result.status()).isEqualTo("ACTIVE");
    assertThat(result.version()).isEqualTo(0L);
    verify(saveStaffPort, never()).save(any());
    verify(auditRecorder, never()).record(any());
    verify(jsonCodecPort).fromJson(storedJson, StaffAccountView.class);
  }

  @Test
  void create_staff_idempotency_conflict_throws_exception() {
    UUID idempotencyKey = UUID.randomUUID();
    String email = "carol@uniquindio.edu.co";
    String storedJson =
        "{\"id\":\"00000000-0000-0000-0000-000000000002\",\"email\":\""
            + email
            + "\",\"fullName\":\"Carol White\",\"status\":\"ACTIVE\",\"version\":0}";

    when(idempotencyStorePort.find(idempotencyKey, "CREATE_STAFF"))
        .thenReturn(
            Optional.of(new IdempotencyStorePort.StoredRecord("different-hash", 201, storedJson)));

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
    when(idempotencyStorePort.find(idempotencyKey, "CREATE_STAFF")).thenReturn(Optional.empty());

    var command =
        new CreateStaffCommand(
            idempotencyKey, UUID.randomUUID(), "person@gmail.com", "Person", "hash");
    assertThatThrownBy(() -> service.createStaff(command))
        .isInstanceOf(co.edu.uniquindio.brujula.identity.domain.DomainNotAllowedException.class);
    verify(saveStaffPort, never()).save(any());
  }
}
