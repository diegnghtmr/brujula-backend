package co.edu.uniquindio.brujula.administration.adapter.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import co.edu.uniquindio.brujula.AbstractPostgresIntegrationTest;
import co.edu.uniquindio.brujula.administration.application.port.out.LoadStaffPort;
import co.edu.uniquindio.brujula.administration.application.port.out.SaveStaffPort;
import co.edu.uniquindio.brujula.administration.domain.StaffAccount;
import co.edu.uniquindio.brujula.administration.domain.StaffAccountStatus;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/** Integration tests for StaffAccount JPA persistence adapter. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Transactional
class StaffPersistenceAdapterTest extends AbstractPostgresIntegrationTest {

  @Autowired private SaveStaffPort saveStaffPort;
  @Autowired private LoadStaffPort loadStaffPort;

  @Test
  void save_and_load_by_id() {
    StaffAccount account =
        StaffAccount.create(
            "staff_persist_" + UUID.randomUUID() + "@uniquindio.edu.co", "Persist Test");
    StaffAccount saved = saveStaffPort.save(account);

    Optional<StaffAccount> loaded = loadStaffPort.findById(saved.getId());
    assertThat(loaded).isPresent();
    assertThat(loaded.get().getEmail()).isEqualTo(saved.getEmail());
    assertThat(loaded.get().getStatus()).isEqualTo(StaffAccountStatus.ACTIVE);
  }

  @Test
  void save_and_load_by_email() {
    String email = "unique_" + UUID.randomUUID() + "@uniquindio.edu.co";
    StaffAccount account = StaffAccount.create(email, "Email Test");
    saveStaffPort.save(account);

    Optional<StaffAccount> loaded = loadStaffPort.findByEmail(email);
    assertThat(loaded).isPresent();
    assertThat(loaded.get().getEmail()).isEqualTo(email);
  }

  @Test
  void update_status_persists_deactivation() {
    String email = "deactivate_" + UUID.randomUUID() + "@uniquindio.edu.co";
    StaffAccount account = StaffAccount.create(email, "Deactivate Test");
    StaffAccount saved = saveStaffPort.save(account);

    saved.deactivate();
    saveStaffPort.save(saved);

    Optional<StaffAccount> loaded = loadStaffPort.findById(saved.getId());
    assertThat(loaded).isPresent();
    assertThat(loaded.get().getStatus()).isEqualTo(StaffAccountStatus.INACTIVE);
  }

  @Test
  void list_staff_returns_all_accounts() {
    String email1 = "list_1_" + UUID.randomUUID() + "@uniquindio.edu.co";
    String email2 = "list_2_" + UUID.randomUUID() + "@uniquindio.edu.co";
    saveStaffPort.save(StaffAccount.create(email1, "List User 1"));
    saveStaffPort.save(StaffAccount.create(email2, "List User 2"));

    var page = loadStaffPort.findAll(null, 0, 100);
    assertThat(page.totalElements()).isGreaterThanOrEqualTo(2);
  }
}
