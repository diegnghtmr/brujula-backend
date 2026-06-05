package co.edu.uniquindio.brujula.identity.adapter.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import co.edu.uniquindio.brujula.AbstractPostgresIntegrationTest;
import co.edu.uniquindio.brujula.identity.application.port.out.LoadAppUserPort;
import co.edu.uniquindio.brujula.identity.application.port.out.SaveAppUserPort;
import co.edu.uniquindio.brujula.identity.domain.AccountStatus;
import co.edu.uniquindio.brujula.identity.domain.AppUser;
import co.edu.uniquindio.brujula.identity.domain.Role;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

/**
 * RED → GREEN persistence adapter tests: AppUser JPA round-trip + constraints.
 *
 * <p>Uses Testcontainers PostgreSQL (via AbstractPostgresIntegrationTest) + Flyway to replicate the
 * real schema. Verifies: save/load round-trip, findByOidcSubject, unique constraint on oidcSubject,
 * unique constraint on email.
 *
 * <p>Satisfies: S1-B-6, Design §2.1 adapter.out.persistence.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class AppUserPersistenceAdapterTest extends AbstractPostgresIntegrationTest {

  @Autowired private LoadAppUserPort loadAppUserPort;
  @Autowired private SaveAppUserPort saveAppUserPort;

  @Test
  @Transactional
  void save_and_find_by_oidc_subject() {
    AppUser user =
        AppUser.provision(
            "sub-persist-" + UUID.randomUUID(),
            "persist_" + UUID.randomUUID() + "@uniquindio.edu.co",
            "Persistence Test User",
            Role.STUDENT);

    AppUser saved = saveAppUserPort.save(user);
    Optional<AppUser> found = loadAppUserPort.findByOidcSubject(saved.oidcSubject());

    assertThat(found).isPresent();
    assertThat(found.get().id()).isEqualTo(saved.id());
    assertThat(found.get().email()).isEqualTo(saved.email());
    assertThat(found.get().role()).isEqualTo(Role.STUDENT);
    assertThat(found.get().status()).isEqualTo(AccountStatus.ACTIVE);
    assertThat(found.get().onboardingCompleted()).isFalse();
  }

  @Test
  @Transactional
  void save_and_find_by_id() {
    AppUser user =
        AppUser.provision(
            "sub-byid-" + UUID.randomUUID(),
            "byid_" + UUID.randomUUID() + "@uniquindio.edu.co",
            "ById Test",
            Role.ADMIN);

    AppUser saved = saveAppUserPort.save(user);
    Optional<AppUser> found = loadAppUserPort.findById(saved.id());

    assertThat(found).isPresent();
    assertThat(found.get().role()).isEqualTo(Role.ADMIN);
  }

  @Test
  @Transactional
  void find_by_nonexistent_oidc_subject_returns_empty() {
    Optional<AppUser> found =
        loadAppUserPort.findByOidcSubject("does-not-exist-" + UUID.randomUUID());
    assertThat(found).isEmpty();
  }

  @Test
  @Transactional
  void find_by_nonexistent_id_returns_empty() {
    Optional<AppUser> found = loadAppUserPort.findById(UUID.randomUUID());
    assertThat(found).isEmpty();
  }

  @Test
  void duplicate_oidc_subject_violates_unique_constraint() {
    String subject = "sub-dup-" + UUID.randomUUID();
    AppUser u1 =
        AppUser.provision(
            subject, "dup1_" + UUID.randomUUID() + "@uniquindio.edu.co", "Dup1", Role.STUDENT);
    saveAppUserPort.save(u1);

    AppUser u2 =
        AppUser.provision(
            subject, "dup2_" + UUID.randomUUID() + "@uniquindio.edu.co", "Dup2", Role.STUDENT);

    assertThatThrownBy(() -> saveAppUserPort.save(u2))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void duplicate_email_violates_unique_constraint() {
    String email = "dupmail_" + UUID.randomUUID() + "@uniquindio.edu.co";
    AppUser u1 = AppUser.provision("sub-em1-" + UUID.randomUUID(), email, "Em1", Role.STUDENT);
    saveAppUserPort.save(u1);

    AppUser u2 = AppUser.provision("sub-em2-" + UUID.randomUUID(), email, "Em2", Role.STUDENT);

    assertThatThrownBy(() -> saveAppUserPort.save(u2))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  @Transactional
  void saved_user_starts_with_version_zero() {
    AppUser user =
        AppUser.provision(
            "sub-ver-" + UUID.randomUUID(),
            "ver_" + UUID.randomUUID() + "@uniquindio.edu.co",
            "Version Test",
            Role.STAFF);

    AppUser saved = saveAppUserPort.save(user);
    assertThat(saved.version()).isZero();
  }
}
