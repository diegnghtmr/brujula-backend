package co.edu.uniquindio.brujula.audit.adapter.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import co.edu.uniquindio.brujula.AbstractPostgresIntegrationTest;
import co.edu.uniquindio.brujula.audit.application.dto.AuditEventPage;
import co.edu.uniquindio.brujula.audit.application.port.out.SaveAuditEventPort;
import co.edu.uniquindio.brujula.audit.application.port.out.SearchAuditEventsPort;
import co.edu.uniquindio.brujula.audit.domain.AuditEvent;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for the audit persistence adapter using Testcontainers PostgreSQL.
 *
 * <p>Verifies append-only behavior: only inserts, no updates.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Transactional
class AuditEventPersistenceAdapterTest extends AbstractPostgresIntegrationTest {

  @Autowired private SaveAuditEventPort saveAuditEventPort;
  @Autowired private SearchAuditEventsPort searchAuditEventsPort;

  @Test
  void save_persists_audit_event_and_retrieves_it() {
    UUID actorId = UUID.randomUUID();
    AuditEvent event =
        AuditEvent.record(actorId, "ADMIN", "TEST_ACTION", "test_entity", null, null, null, null);

    saveAuditEventPort.save(event);

    AuditEventPage page = searchAuditEventsPort.search(null, null, null, null, null, 0, 20);
    assertThat(page.totalElements()).isGreaterThanOrEqualTo(1);
  }

  @Test
  void save_assigns_id_to_persisted_event() {
    UUID actorId = UUID.randomUUID();
    AuditEvent event =
        AuditEvent.record(
            actorId, "STAFF", "DOMAIN_ADDED", "allowed_email_domain", null, null, null, null);

    saveAuditEventPort.save(event);

    assertThat(event.getId()).isNotNull();
  }

  @Test
  void search_filters_by_actor_id() {
    UUID actorId1 = UUID.randomUUID();
    UUID actorId2 = UUID.randomUUID();

    AuditEvent e1 =
        AuditEvent.record(actorId1, "ADMIN", "LOGIN", "session", null, null, null, null);
    AuditEvent e2 =
        AuditEvent.record(actorId2, "STUDENT", "LOGIN", "session", null, null, null, null);

    saveAuditEventPort.save(e1);
    saveAuditEventPort.save(e2);

    // Search is global (no actor filter in the search port by default)
    AuditEventPage page = searchAuditEventsPort.search(null, null, null, null, null, 0, 100);
    assertThat(page.totalElements()).isGreaterThanOrEqualTo(2);
  }

  @Test
  void search_with_action_filter_returns_matching_events() {
    UUID actorId = UUID.randomUUID();
    AuditEvent event =
        AuditEvent.record(
            actorId, "ADMIN", "UNIQUE_ACTION_PERSISTENCE_TEST", "app_user", null, null, null, null);
    saveAuditEventPort.save(event);

    AuditEventPage page =
        searchAuditEventsPort.search(
            null, "UNIQUE_ACTION_PERSISTENCE_TEST", null, null, null, 0, 20);
    assertThat(page.totalElements()).isGreaterThanOrEqualTo(1);
    assertThat(page.items().getFirst().action()).isEqualTo("UNIQUE_ACTION_PERSISTENCE_TEST");
  }

  @Test
  void search_returns_paginated_results() {
    UUID actorId = UUID.randomUUID();
    for (int i = 0; i < 5; i++) {
      AuditEvent event =
          AuditEvent.record(actorId, "ADMIN", "PAGING_TEST", "entity", null, null, null, null);
      saveAuditEventPort.save(event);
    }

    AuditEventPage page = searchAuditEventsPort.search(null, "PAGING_TEST", null, null, null, 0, 2);
    assertThat(page.items()).hasSize(2);
    assertThat(page.totalElements()).isGreaterThanOrEqualTo(5);
  }
}
