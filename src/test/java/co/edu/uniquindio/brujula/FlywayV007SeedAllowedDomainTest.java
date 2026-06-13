package co.edu.uniquindio.brujula;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * RED test for V007__seed_allowed_email_domain.sql.
 *
 * <p>Verifies that exactly one row with {@code domain='uniquindio.edu.co'} exists in {@code
 * administration.allowed_email_domain} after the full migration run. Fails until the seed migration
 * file exists.
 *
 * <p>Data is seeded separately from structure to keep migrations idempotent.
 */
@SpringBootTest
class FlywayV007SeedAllowedDomainTest extends AbstractPostgresIntegrationTest {

  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  void uniquindio_edu_co_seed_domain_exists_after_v007() {
    Integer count =
        jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*)
            FROM administration.allowed_email_domain
            WHERE domain = 'uniquindio.edu.co'
            """,
            Integer.class);

    assertThat(count)
        .as(
            "administration.allowed_email_domain must contain the seed row"
                + " domain='uniquindio.edu.co' after V007 data migration")
        .isEqualTo(1);
  }
}
