package co.edu.uniquindio.brujula.administration.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/** Unit tests for {@link StaffAccount} domain aggregate (TDD RED phase). */
class StaffAccountTest {

  @Test
  void create_produces_active_account_with_required_fields() {
    StaffAccount account = StaffAccount.create("alice@uniquindio.edu.co", "Alice Smith");

    assertThat(account.getId()).isNotNull();
    assertThat(account.getEmail()).isEqualTo("alice@uniquindio.edu.co");
    assertThat(account.getFullName()).isEqualTo("Alice Smith");
    assertThat(account.getStatus()).isEqualTo(StaffAccountStatus.ACTIVE);
    assertThat(account.getVersion()).isEqualTo(0L);
  }

  @Test
  void create_rejects_null_email() {
    assertThatThrownBy(() -> StaffAccount.create(null, "Alice Smith"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("email");
  }

  @Test
  void create_rejects_blank_email() {
    assertThatThrownBy(() -> StaffAccount.create("  ", "Alice Smith"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("email");
  }

  @Test
  void create_rejects_null_fullName() {
    assertThatThrownBy(() -> StaffAccount.create("alice@uniquindio.edu.co", null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("fullName");
  }

  @Test
  void deactivate_changes_status_to_inactive() {
    StaffAccount account = StaffAccount.create("bob@uniquindio.edu.co", "Bob Jones");
    account.deactivate();
    assertThat(account.getStatus()).isEqualTo(StaffAccountStatus.INACTIVE);
  }

  @Test
  void activate_changes_status_to_active() {
    StaffAccount account = StaffAccount.create("carol@uniquindio.edu.co", "Carol White");
    account.deactivate();
    account.activate();
    assertThat(account.getStatus()).isEqualTo(StaffAccountStatus.ACTIVE);
  }

  @Test
  void reconstitute_restores_account_from_db() {
    var id = java.util.UUID.randomUUID();
    StaffAccount account =
        StaffAccount.reconstitute(
            id, "dave@uniquindio.edu.co", "Dave Brown", StaffAccountStatus.INACTIVE, 3L);

    assertThat(account.getId()).isEqualTo(id);
    assertThat(account.getStatus()).isEqualTo(StaffAccountStatus.INACTIVE);
    assertThat(account.getVersion()).isEqualTo(3L);
  }
}
