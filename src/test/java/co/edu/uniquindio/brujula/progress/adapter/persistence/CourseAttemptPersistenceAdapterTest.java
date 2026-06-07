package co.edu.uniquindio.brujula.progress.adapter.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import co.edu.uniquindio.brujula.AbstractPostgresIntegrationTest;
import co.edu.uniquindio.brujula.progress.application.port.out.LoadCourseAttemptPort;
import co.edu.uniquindio.brujula.progress.application.port.out.LoadSubscriptionPort;
import co.edu.uniquindio.brujula.progress.application.port.out.SaveCourseAttemptPort;
import co.edu.uniquindio.brujula.progress.application.port.out.SaveSubscriptionPort;
import co.edu.uniquindio.brujula.progress.domain.AttemptId;
import co.edu.uniquindio.brujula.progress.domain.AttemptStatus;
import co.edu.uniquindio.brujula.progress.domain.CourseAttempt;
import co.edu.uniquindio.brujula.progress.domain.Grade;
import co.edu.uniquindio.brujula.progress.domain.Subscription;
import co.edu.uniquindio.brujula.progress.domain.SubscriptionId;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * WU3-T05 RED: persistence adapter integration tests for {@link CourseAttempt}.
 *
 * <p>Uses {@link AbstractPostgresIntegrationTest} ({@code @SpringBootTest} + real Postgres V015).
 * Validates save, load, list-by-subscription, and get-by-id ownership queries.
 *
 * <p>FIX-W4: all {@code CourseAttempt.register()} calls now use a non-null periodId.
 */
@SpringBootTest
class CourseAttemptPersistenceAdapterTest extends AbstractPostgresIntegrationTest {

  @Autowired private SaveSubscriptionPort saveSubscriptionPort;
  @Autowired private LoadSubscriptionPort loadSubscriptionPort;
  @Autowired private SaveCourseAttemptPort saveCourseAttemptPort;
  @Autowired private LoadCourseAttemptPort loadCourseAttemptPort;

  private final UUID userId = UUID.randomUUID();
  private final UUID programId = UUID.randomUUID();
  private final UUID planId = UUID.randomUUID();
  private final UUID courseId = UUID.randomUUID();
  // FIX-W4: use a stable test period UUID
  private final UUID testPeriodId = UUID.fromString("00000000-0000-4000-a000-000000000001");

  @Test
  void save_and_load_attempt_round_trip() {
    Subscription sub = createSubscription();
    Grade grade = new Grade(new BigDecimal("4.0"));
    CourseAttempt attempt = CourseAttempt.register(
        AttemptId.generate(),
        sub.getId(),
        courseId,
        AttemptStatus.PASSED,
        grade,
        testPeriodId,
        "DIURNA");

    CourseAttempt saved = saveCourseAttemptPort.save(attempt);
    Optional<CourseAttempt> loaded = loadCourseAttemptPort.loadById(saved.getId().value());

    assertThat(loaded).isPresent();
    assertThat(loaded.get().getId()).isEqualTo(saved.getId());
    assertThat(loaded.get().getCourseId()).isEqualTo(courseId);
    assertThat(loaded.get().getStatus()).isEqualTo(AttemptStatus.PASSED);
    assertThat(loaded.get().getGrade().value()).isEqualByComparingTo(new BigDecimal("4.0"));
    assertThat(loaded.get().getVersion()).isGreaterThanOrEqualTo(0L);
  }

  @Test
  void load_by_subscription_id_returns_all_attempts() {
    Subscription sub = createSubscription();
    UUID courseId2 = UUID.randomUUID();

    CourseAttempt a1 = CourseAttempt.register(
        AttemptId.generate(), sub.getId(), courseId, AttemptStatus.PASSED,
        new Grade(new BigDecimal("4.0")), testPeriodId, "DIURNA");
    CourseAttempt a2 = CourseAttempt.register(
        AttemptId.generate(), sub.getId(), courseId2, AttemptStatus.FAILED,
        new Grade(new BigDecimal("2.0")), testPeriodId, "DIURNA");

    saveCourseAttemptPort.save(a1);
    saveCourseAttemptPort.save(a2);

    List<CourseAttempt> results = loadCourseAttemptPort.loadBySubscriptionId(sub.getId().value());
    assertThat(results).hasSizeGreaterThanOrEqualTo(2);
    assertThat(results).anyMatch(a -> a.getCourseId().equals(courseId));
    assertThat(results).anyMatch(a -> a.getCourseId().equals(courseId2));
  }

  @Test
  void load_by_id_returns_empty_when_not_found() {
    Optional<CourseAttempt> result = loadCourseAttemptPort.loadById(UUID.randomUUID());
    assertThat(result).isEmpty();
  }

  @Test
  void cancelled_attempt_has_null_grade() {
    Subscription sub = createSubscription();
    CourseAttempt attempt = CourseAttempt.register(
        AttemptId.generate(), sub.getId(), courseId, AttemptStatus.CANCELLED,
        null, testPeriodId, "DIURNA");

    CourseAttempt saved = saveCourseAttemptPort.save(attempt);
    Optional<CourseAttempt> loaded = loadCourseAttemptPort.loadById(saved.getId().value());

    assertThat(loaded).isPresent();
    assertThat(loaded.get().getGrade()).isNull();
    assertThat(loaded.get().getStatus()).isEqualTo(AttemptStatus.CANCELLED);
  }

  @Test
  void save_returns_attempt_with_incremented_version_after_update() {
    Subscription sub = createSubscription();
    CourseAttempt attempt = CourseAttempt.register(
        AttemptId.generate(), sub.getId(), courseId, AttemptStatus.FAILED,
        new Grade(new BigDecimal("2.0")), testPeriodId, "DIURNA");

    CourseAttempt saved = saveCourseAttemptPort.save(attempt);
    long initialVersion = saved.getVersion();

    // update and re-save
    CourseAttempt reloaded = loadCourseAttemptPort.loadById(saved.getId().value()).orElseThrow();
    reloaded.update(AttemptStatus.PASSED, new Grade(new BigDecimal("3.5")), null, "DIURNA");
    CourseAttempt updated = saveCourseAttemptPort.save(reloaded);

    assertThat(updated.getVersion()).isGreaterThan(initialVersion);
    assertThat(updated.getStatus()).isEqualTo(AttemptStatus.PASSED);
  }

  @Test
  void period_id_is_stored_and_loaded_as_opaque_uuid() {
    Subscription sub = createSubscription();
    UUID opaquePeriodId = UUID.randomUUID();
    CourseAttempt attempt = CourseAttempt.register(
        AttemptId.generate(), sub.getId(), courseId, AttemptStatus.PASSED,
        new Grade(new BigDecimal("4.0")), opaquePeriodId, "DIURNA");

    CourseAttempt saved = saveCourseAttemptPort.save(attempt);
    Optional<CourseAttempt> loaded = loadCourseAttemptPort.loadById(saved.getId().value());

    assertThat(loaded).isPresent();
    assertThat(loaded.get().getPeriodId()).isEqualTo(opaquePeriodId);
  }

  // -------------------------------------------------------------------------
  // Helper
  // -------------------------------------------------------------------------

  private Subscription createSubscription() {
    SubscriptionId id = SubscriptionId.generate();
    Subscription sub = Subscription.create(id, userId, programId, planId, "DIURNA");
    return saveSubscriptionPort.save(sub);
  }
}
