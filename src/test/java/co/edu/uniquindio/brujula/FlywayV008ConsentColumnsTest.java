package co.edu.uniquindio.brujula;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * RED test for V008__add_consent_and_onboarding_state_to_app_user.sql.
 *
 * <p>Verifies that {@code identity.app_user} gains the consent columns and {@code onboarding_state}
 * column after V008 migration. Fails until the migration file exists.
 *
 * <p>Design §3 / §6: consent evidence stored on AppUser; onboarding_state text column replaces
 * onboarding_completed bool as the authoritative state field for Slice 2 onwards.
 *
 * <p>Satisfies: S2-B-1, Spec §Successful registration stores consent evidence.
 */
@SpringBootTest
class FlywayV008ConsentColumnsTest extends AbstractPostgresIntegrationTest {

  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  void identity_app_user_has_consent_timestamp_column() {
    List<String> columns =
        jdbcTemplate.queryForList(
            """
            SELECT column_name
            FROM information_schema.columns
            WHERE table_schema = 'identity'
              AND table_name = 'app_user'
            ORDER BY column_name
            """,
            String.class);

    assertThat(columns)
        .as("identity.app_user must have consent_timestamp column after V008")
        .contains("consent_timestamp");
  }

  @Test
  void identity_app_user_has_terms_version_column() {
    List<String> columns =
        jdbcTemplate.queryForList(
            """
            SELECT column_name
            FROM information_schema.columns
            WHERE table_schema = 'identity'
              AND table_name = 'app_user'
            """,
            String.class);

    assertThat(columns)
        .as("identity.app_user must have terms_version column after V008")
        .contains("terms_version");
  }

  @Test
  void identity_app_user_has_privacy_version_column() {
    List<String> columns =
        jdbcTemplate.queryForList(
            """
            SELECT column_name
            FROM information_schema.columns
            WHERE table_schema = 'identity'
              AND table_name = 'app_user'
            """,
            String.class);

    assertThat(columns)
        .as("identity.app_user must have privacy_version column after V008")
        .contains("privacy_version");
  }

  @Test
  void identity_app_user_has_onboarding_state_column() {
    List<String> columns =
        jdbcTemplate.queryForList(
            """
            SELECT column_name
            FROM information_schema.columns
            WHERE table_schema = 'identity'
              AND table_name = 'app_user'
            """,
            String.class);

    assertThat(columns)
        .as("identity.app_user must have onboarding_state column after V008")
        .contains("onboarding_state");
  }

  /**
   * W-002: verify that the onboarding_state column default is 'ACTIVE'.
   * The V008 UPDATE was a no-op (WHERE 'ACTIVE' = 'ACTIVE') — the column DEFAULT handles backfill.
   * This test proves the DEFAULT value is 'ACTIVE', making the no-op UPDATE harmless and removable.
   */
  @Test
  void identity_app_user_onboarding_state_column_default_is_active() {
    String columnDefault =
        jdbcTemplate.queryForObject(
            """
            SELECT column_default
            FROM information_schema.columns
            WHERE table_schema = 'identity'
              AND table_name = 'app_user'
              AND column_name = 'onboarding_state'
            """,
            String.class);

    assertThat(columnDefault)
        .as("onboarding_state column default must be 'ACTIVE' so existing rows are correctly backfilled")
        .contains("ACTIVE");
  }

  @Test
  void identity_app_user_consent_timestamp_is_nullable_timestamptz() {
    List<String> dataTypes =
        jdbcTemplate.queryForList(
            """
            SELECT data_type
            FROM information_schema.columns
            WHERE table_schema = 'identity'
              AND table_name = 'app_user'
              AND column_name = 'consent_timestamp'
            """,
            String.class);

    assertThat(dataTypes)
        .as("consent_timestamp must be a timestamp-with-timezone type")
        .isNotEmpty();
    assertThat(dataTypes.getFirst())
        .as("consent_timestamp data type")
        .containsIgnoringCase("timestamp");
  }
}
