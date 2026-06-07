package co.edu.uniquindio.brujula.programcatalog.infrastructure.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import co.edu.uniquindio.brujula.AbstractPostgresIntegrationTest;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import jakarta.persistence.OptimisticLockException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * FIX-3 (W1): persistence-level optimistic lock conflict test for StudyPlan (CAT-002).
 *
 * <p>Validates that saving a stale {@link StudyPlanJpaEntity} (version=0 when the DB is already at
 * version=1) raises {@link ObjectOptimisticLockingFailureException} — covering the CAT-002
 * "concurrent update → HTTP 409" invariant at the persistence layer without requiring a write
 * endpoint (all return 501 in Fase 2 WU4 scope).
 *
 * <p>Runs inside the same package as {@link StudyPlanJpaEntity} to access the package-private class.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class StudyPlanOptimisticLockTest extends AbstractPostgresIntegrationTest {

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @PersistenceContext
  private EntityManager entityManager;

  @Autowired
  private PlatformTransactionManager transactionManager;

  /**
   * RED → GREEN: merging a stale StudyPlanJpaEntity raises ObjectOptimisticLockingFailureException.
   *
   * <p>Steps:
   * <ol>
   *   <li>Insert a program row (FK) and a StudyPlan row with version=0.
   *   <li>Simulate a concurrent update by incrementing the DB version to 1 via raw SQL.
   *   <li>Inside a JPA transaction, merge a detached entity still carrying version=0.
   *   <li>Flush — Hibernate issues {@code UPDATE … WHERE id=? AND version=0} which matches 0 rows,
   *       triggering {@link ObjectOptimisticLockingFailureException}.
   * </ol>
   */
  @Test
  void saving_stale_study_plan_raises_optimistic_locking_failure() {
    UUID programId = UUID.randomUUID();
    UUID planId = UUID.randomUUID();

    // Step 1: Insert prerequisite program + study plan (version = 0)
    jdbcTemplate.update(
        "INSERT INTO catalog.academic_program (id, name, faculty, status, version) "
            + "VALUES (?, ?, ?, ?, 0)",
        programId, "Prog-OptLock-" + planId, "Test Faculty", "ACTIVE");

    jdbcTemplate.update(
        "INSERT INTO catalog.study_plan (id, program_id, name, status, total_credits, version) "
            + "VALUES (?, ?, ?, ?, ?, 0)",
        planId, programId, "Plan Optimistic Lock Test", "DRAFT", 120);

    // Step 2: Simulate concurrent update — advance DB version to 1
    int updated = jdbcTemplate.update(
        "UPDATE catalog.study_plan SET name = 'Plan Concurrente', version = version + 1 "
            + "WHERE id = ? AND version = 0",
        planId);
    assert updated == 1 : "Precondition: row must have been updated";

    // Step 3 + 4: Try to merge a detached entity with stale version=0
    // This simulates the scenario where two users loaded version=0 simultaneously.
    // The second one to flush must fail with OOLF.
    TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
    // Spring wraps jakarta.persistence.OptimisticLockException into
    // ObjectOptimisticLockingFailureException when the exception propagates through
    // Spring's JPA exception translation. The merge/flush may throw either the JPA
    // exception directly (before translation) or the Spring wrapper — we accept both.
    assertThatThrownBy(() ->
        txTemplate.execute(status -> {
          // Build a detached entity manually with stale version (0)
          StudyPlanJpaEntity stale = new StudyPlanJpaEntity(
              planId, programId, "Plan Stale", "DRAFT", 120, null);
          // Merge will issue UPDATE WHERE id=? AND version=0 — 0 rows affected → OOLF
          entityManager.merge(stale);
          entityManager.flush();
          return null;
        }))
        .satisfies(thrown -> {
          // Accept either Spring's wrapper or the underlying JPA/Hibernate exception
          boolean isSpringWrapper = thrown instanceof ObjectOptimisticLockingFailureException;
          boolean isJpaException = thrown instanceof OptimisticLockException
              || (thrown.getCause() instanceof OptimisticLockException);
          boolean isHibernateStale = thrown.getMessage() != null
              && thrown.getMessage().contains("already updated or deleted");
          assert isSpringWrapper || isJpaException || isHibernateStale
              : "Expected an optimistic locking failure, got: " + thrown.getClass().getName()
              + " — " + thrown.getMessage();
        });
  }
}
