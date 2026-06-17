package co.edu.uniquindio.brujula.tooling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Real end-to-end round-trip integration test for backup.sh → restore.sh.
 *
 * <p>This test:
 *
 * <ol>
 *   <li>Starts a PostgreSQL 18.4-alpine container (source DB).
 *   <li>Creates a {@code platform} schema with a tiny seeded table ({@code compass_data}) and the
 *       three transient tables ({@code spring_session}, {@code spring_session_attributes}, {@code
 *       idempotency_record}).
 *   <li>Seeds rows into all tables, including the transient ones.
 *   <li>Generates an ephemeral age keypair in {@code @TempDir} via {@code age-keygen}.
 *   <li>Runs the REAL {@code backup.sh} against the source container.
 *   <li>Starts a second PostgreSQL 18.4-alpine container (restore target) with the same schema
 *       pre-created (simulating Flyway having run).
 *   <li>Runs the REAL {@code restore.sh} against the restore target.
 *   <li>Asserts that seeded rows from {@code compass_data} are restored correctly, and that the
 *       transient tables are empty (backup excluded them).
 * </ol>
 *
 * <p>Tagged {@code @Tag("requires-age")} — excluded from {@code ./gradlew check}, run explicitly
 * via {@code ./gradlew roundTripTest}.
 *
 * <p>Guarded with {@code assumeTrue} checks so it SKIPS gracefully if {@code age}, {@code pg_dump},
 * {@code psql}, or Docker are unavailable.
 */
@Tag("requires-age")
@Testcontainers
class BackupRestoreRoundTripIT {

  private static final String PG_IMAGE = "postgres:18.4-alpine";
  private static final String DB_NAME = "brujula_rt";
  private static final String DB_USER = "brujula";
  private static final String DB_PASSWORD = "roundtrip_pw";

  @TempDir Path work;

  @Test
  void backupAndRestoreRoundTripsData() throws Exception {
    // ─── Guard: require tools on PATH ────────────────────────────────────────
    assumeTrue(toolOnPath("age"), "age is not on PATH — skipping round-trip IT");
    assumeTrue(toolOnPath("age-keygen"), "age-keygen is not on PATH — skipping round-trip IT");
    assumeTrue(toolOnPath("pg_dump"), "pg_dump is not on PATH — skipping round-trip IT");
    assumeTrue(toolOnPath("psql"), "psql is not on PATH — skipping round-trip IT");

    // ─── Source DB container ──────────────────────────────────────────────────
    try (PostgreSQLContainer<?> source =
            new PostgreSQLContainer<>(PG_IMAGE)
                .withDatabaseName(DB_NAME)
                .withUsername(DB_USER)
                .withPassword(DB_PASSWORD);
        PostgreSQLContainer<?> target =
            new PostgreSQLContainer<>(PG_IMAGE)
                .withDatabaseName(DB_NAME)
                .withUsername(DB_USER)
                .withPassword(DB_PASSWORD)) {

      source.start();

      // ─── Build schema and seed data ───────────────────────────────────────
      createSchemaAndSeedData(source);

      // ─── Generate ephemeral age keypair ───────────────────────────────────
      Path privateKeyFile = work.resolve("brujula-roundtrip-key.txt");
      String publicKey = generateAgeKeypair(privateKeyFile);

      // ─── Write a .env for the scripts ─────────────────────────────────────
      // backup.sh reads DB vars + AGE_PUBLIC_KEY
      // restore.sh reads DB vars + AGE_PRIVATE_KEY_PATH
      Path envFile = work.resolve(".env");
      Files.writeString(
          envFile,
          """
          DB_HOST=%s
          DB_PORT=%d
          DB_NAME=%s
          DB_USER=%s
          DB_PASSWORD=%s
          AGE_PUBLIC_KEY=%s
          AGE_PRIVATE_KEY_PATH=%s
          """
              .formatted(
                  source.getHost(),
                  source.getMappedPort(5432),
                  DB_NAME,
                  DB_USER,
                  DB_PASSWORD,
                  publicKey,
                  privateKeyFile.toAbsolutePath()));

      // ─── Run backup.sh ────────────────────────────────────────────────────
      Path backupsDir = work.resolve("backups");
      Files.createDirectories(backupsDir);

      String backupScript = resolveScriptPath("scripts/db/backup.sh");
      ScriptRun backupRun =
          runScript(
              backupScript,
              List.of("roundtrip-test"),
              Map.of(
                  "BRUJULA_ENV_FILE", envFile.toAbsolutePath().toString(),
                  "REPO_ROOT", work.toAbsolutePath().toString()));

      assertThat(backupRun.exitCode())
          .as("backup.sh must exit 0; stdout=%s stderr=%s", backupRun.stdout(), backupRun.stderr())
          .isEqualTo(0);

      // Verify a .sql.age was produced
      List<Path> ageFiles;
      try (Stream<Path> files = Files.list(backupsDir)) {
        ageFiles = files.filter(p -> p.getFileName().toString().endsWith(".sql.age")).toList();
      }
      assertThat(ageFiles).as("backup.sh must produce exactly one .sql.age file").hasSize(1);
      Path backupFile = ageFiles.get(0);
      assertThat(Files.size(backupFile)).as("backup file must be non-empty").isGreaterThan(0);

      // Verify no plaintext .sql files remain
      try (Stream<Path> all = Files.walk(work)) {
        List<Path> sqlFiles =
            all.filter(p -> p.toString().endsWith(".sql"))
                .filter(p -> !p.toString().endsWith(".sql.age"))
                .toList();
        assertThat(sqlFiles).as("no plaintext .sql files should remain after backup").isEmpty();
      }

      // ─── Start restore target and build schema ────────────────────────────
      target.start();
      createSchemaOnly(target); // simulates Flyway pre-build (schema without data)

      // ─── Update .env to point at restore target ───────────────────────────
      Files.writeString(
          envFile,
          """
          DB_HOST=%s
          DB_PORT=%d
          DB_NAME=%s
          DB_USER=%s
          DB_PASSWORD=%s
          AGE_PUBLIC_KEY=%s
          AGE_PRIVATE_KEY_PATH=%s
          """
              .formatted(
                  target.getHost(),
                  target.getMappedPort(5432),
                  DB_NAME,
                  DB_USER,
                  DB_PASSWORD,
                  publicKey,
                  privateKeyFile.toAbsolutePath()));

      // ─── Run restore.sh ───────────────────────────────────────────────────
      String restoreScript = resolveScriptPath("scripts/db/restore.sh");
      ScriptRun restoreRun =
          runScript(
              restoreScript,
              List.of(backupFile.toAbsolutePath().toString()),
              Map.of(
                  "BRUJULA_ENV_FILE", envFile.toAbsolutePath().toString(),
                  "REPO_ROOT", work.toAbsolutePath().toString()));

      assertThat(restoreRun.exitCode())
          .as(
              "restore.sh must exit 0; stdout=%s stderr=%s",
              restoreRun.stdout(), restoreRun.stderr())
          .isEqualTo(0);

      // ─── Assert round-trip correctness ────────────────────────────────────
      try (Connection conn =
          DriverManager.getConnection(target.getJdbcUrl(), DB_USER, DB_PASSWORD)) {

        // Seeded rows in compass_data must be present
        int compassCount = queryCount(conn, "SELECT COUNT(*) FROM platform.compass_data");
        assertThat(compassCount)
            .as("compass_data must have 3 seeded rows after restore")
            .isEqualTo(3);

        // Transient tables must be empty (excluded from backup)
        int sessionCount = queryCount(conn, "SELECT COUNT(*) FROM platform.spring_session");
        assertThat(sessionCount)
            .as("spring_session must be empty after restore (excluded from backup)")
            .isEqualTo(0);

        int sessionAttrsCount =
            queryCount(conn, "SELECT COUNT(*) FROM platform.spring_session_attributes");
        assertThat(sessionAttrsCount)
            .as("spring_session_attributes must be empty after restore (excluded from backup)")
            .isEqualTo(0);

        int idempotencyCount = queryCount(conn, "SELECT COUNT(*) FROM platform.idempotency_record");
        assertThat(idempotencyCount)
            .as("idempotency_record must be empty after restore (excluded from backup)")
            .isEqualTo(0);
      }
    }
  }

  // ─── Schema helpers ──────────────────────────────────────────────────────────

  private void createSchemaAndSeedData(PostgreSQLContainer<?> container) throws SQLException {
    try (Connection conn =
        DriverManager.getConnection(container.getJdbcUrl(), DB_USER, DB_PASSWORD)) {
      try (Statement st = conn.createStatement()) {

        // Create platform schema and tables
        st.execute("CREATE SCHEMA IF NOT EXISTS platform");

        st.execute(
            """
            CREATE TABLE platform.compass_data (
                id    SERIAL PRIMARY KEY,
                label VARCHAR(100) NOT NULL,
                value INTEGER NOT NULL
            )
            """);

        st.execute(
            """
            CREATE TABLE platform.spring_session (
                primary_id    CHAR(36) PRIMARY KEY,
                session_id    CHAR(36) NOT NULL,
                creation_time BIGINT   NOT NULL
            )
            """);

        st.execute(
            """
            CREATE TABLE platform.spring_session_attributes (
                session_primary_id CHAR(36)     NOT NULL,
                attribute_name     VARCHAR(200) NOT NULL,
                attribute_bytes    BYTEA        NOT NULL,
                PRIMARY KEY (session_primary_id, attribute_name)
            )
            """);

        st.execute(
            """
            CREATE TABLE platform.idempotency_record (
                idempotency_key VARCHAR(255) PRIMARY KEY,
                created_at      TIMESTAMP    NOT NULL
            )
            """);

        // Seed compass_data with 3 rows
        st.execute(
            """
            INSERT INTO platform.compass_data (label, value) VALUES
                ('alpha', 1),
                ('beta',  2),
                ('gamma', 3)
            """);

        // Seed the transient tables with rows that should NOT survive backup/restore
        st.execute(
            """
            INSERT INTO platform.spring_session (primary_id, session_id, creation_time)
            VALUES ('aaaa-bbbb-cccc-dddd-eeee', 'sess-001', 1718000000000)
            """);

        st.execute(
            """
            INSERT INTO platform.spring_session_attributes
                (session_primary_id, attribute_name, attribute_bytes)
            VALUES ('aaaa-bbbb-cccc-dddd-eeee', 'USER', '\\x01'::bytea)
            """);

        st.execute(
            """
            INSERT INTO platform.idempotency_record (idempotency_key, created_at)
            VALUES ('key-001', NOW())
            """);
      }
    }
  }

  /** Creates only the schema structure (no data) — simulates Flyway pre-building the schema. */
  private void createSchemaOnly(PostgreSQLContainer<?> container) throws SQLException {
    try (Connection conn =
        DriverManager.getConnection(container.getJdbcUrl(), DB_USER, DB_PASSWORD)) {
      try (Statement st = conn.createStatement()) {
        st.execute("CREATE SCHEMA IF NOT EXISTS platform");
        st.execute(
            """
            CREATE TABLE platform.compass_data (
                id    SERIAL PRIMARY KEY,
                label VARCHAR(100) NOT NULL,
                value INTEGER NOT NULL
            )
            """);
        st.execute(
            """
            CREATE TABLE platform.spring_session (
                primary_id    CHAR(36) PRIMARY KEY,
                session_id    CHAR(36) NOT NULL,
                creation_time BIGINT   NOT NULL
            )
            """);
        st.execute(
            """
            CREATE TABLE platform.spring_session_attributes (
                session_primary_id CHAR(36)     NOT NULL,
                attribute_name     VARCHAR(200) NOT NULL,
                attribute_bytes    BYTEA        NOT NULL,
                PRIMARY KEY (session_primary_id, attribute_name)
            )
            """);
        st.execute(
            """
            CREATE TABLE platform.idempotency_record (
                idempotency_key VARCHAR(255) PRIMARY KEY,
                created_at      TIMESTAMP    NOT NULL
            )
            """);
      }
    }
  }

  // ─── age keypair generation ───────────────────────────────────────────────────

  /**
   * Generates an ephemeral age keypair, writes the private key to {@code privateKeyFile}, and
   * returns the public key string.
   */
  private String generateAgeKeypair(Path privateKeyFile) throws IOException, InterruptedException {
    ProcessBuilder pb =
        new ProcessBuilder("age-keygen", "-o", privateKeyFile.toAbsolutePath().toString());
    pb.redirectErrorStream(true);
    Process proc = pb.start();
    String output = new String(proc.getInputStream().readAllBytes());
    proc.waitFor(30, TimeUnit.SECONDS);

    // Set restrictive permissions so age doesn't complain
    Files.setPosixFilePermissions(
        privateKeyFile,
        EnumSet.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE));

    // age-keygen prints the public key to stdout: "Public key: age1..."
    String publicKey =
        output
            .lines()
            .filter(l -> l.startsWith("Public key:"))
            .map(l -> l.substring("Public key:".length()).strip())
            .findFirst()
            .orElseThrow(
                () ->
                    new AssertionError("age-keygen did not output a public key; output=" + output));

    return publicKey;
  }

  // ─── Process runner ──────────────────────────────────────────────────────────

  private record ScriptRun(int exitCode, String stdout, String stderr) {}

  private ScriptRun runScript(
      String scriptPath, List<String> args, Map<String, String> envOverrides)
      throws IOException, InterruptedException {
    List<String> command = new java.util.ArrayList<>();
    command.add("bash");
    command.add(scriptPath);
    command.addAll(args);

    ProcessBuilder pb = new ProcessBuilder(command);
    pb.directory(work.toFile());
    Map<String, String> env = pb.environment();
    env.putAll(envOverrides);

    Process proc = pb.start();
    String stdout = new String(proc.getInputStream().readAllBytes());
    String stderr = new String(proc.getErrorStream().readAllBytes());
    boolean finished = proc.waitFor(120, TimeUnit.SECONDS);

    if (!finished) {
      proc.destroyForcibly();
      throw new AssertionError(
          "Script timed out after 120s: "
              + scriptPath
              + "\nstdout="
              + stdout
              + "\nstderr="
              + stderr);
    }

    return new ScriptRun(proc.exitValue(), stdout, stderr);
  }

  // ─── Helpers ─────────────────────────────────────────────────────────────────

  private static boolean toolOnPath(String tool) {
    try {
      ProcessBuilder pb = new ProcessBuilder("bash", "-c", "command -v " + tool);
      pb.redirectErrorStream(true);
      Process p = pb.start();
      p.waitFor(5, TimeUnit.SECONDS);
      return p.exitValue() == 0;
    } catch (Exception e) {
      return false;
    }
  }

  private static int queryCount(Connection conn, String sql) throws SQLException {
    try (Statement st = conn.createStatement();
        ResultSet rs = st.executeQuery(sql)) {
      rs.next();
      return rs.getInt(1);
    }
  }

  private static String resolveScriptPath(String relativePath) {
    try {
      Path candidate =
          Path.of(
              BackupRestoreRoundTripIT.class
                  .getProtectionDomain()
                  .getCodeSource()
                  .getLocation()
                  .toURI());
      Path dir = candidate;
      for (int i = 0; i < 10; i++) {
        dir = dir.getParent();
        if (dir == null) break;
        if (Files.exists(dir.resolve("build.gradle.kts"))) {
          return dir.resolve(relativePath).toAbsolutePath().toString();
        }
      }
    } catch (Exception ignored) {
      // fall through
    }
    return Path.of(System.getProperty("user.dir"), relativePath).toAbsolutePath().toString();
  }
}
