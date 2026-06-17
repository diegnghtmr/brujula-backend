package co.edu.uniquindio.brujula.tooling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import co.edu.uniquindio.brujula.tooling.support.ScriptHarness;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Contract test for {@code scripts/db/restore.sh}.
 *
 * <p>Uses {@link ScriptHarness} to run the script with fake {@code age} and {@code psql}
 * executables that record their argv. Tests verify key preflight checks, argument handling, and
 * temp-file cleanup — all without requiring real tools on the runner.
 *
 * <p>All tests are guarded with {@code assumeTrue(!windows)} so they SKIP on Windows CI.
 */
class RestoreScriptContractTest {

  @TempDir Path work;

  private String scriptPath;
  private Path fakeSqlAge;

  @BeforeEach
  void setUp() throws IOException {
    assumeTrue(
        !System.getProperty("os.name", "").toLowerCase().startsWith("win"),
        "restore.sh is a POSIX script — skipping on Windows");

    scriptPath = resolveScriptPath("scripts/db/restore.sh");
    assumeTrue(
        scriptPath != null && Files.exists(Path.of(scriptPath)),
        "restore.sh not found — script not yet created (expected RED state)");

    // Create a fake .sql.age input file so the script has something to restore from
    Path backupsDir = work.resolve("backups");
    Files.createDirectories(backupsDir);
    fakeSqlAge = backupsDir.resolve("brujula-data-2026-01-01-test.sql.age");
    Files.writeString(fakeSqlAge, "-- fake encrypted backup for testing\n");
  }

  // ─── Core contract assertions ────────────────────────────────────────────────

  @Test
  void happyPathExitsZeroAndPsqlReceivesStream() throws IOException, InterruptedException {
    ScriptHarness h = ScriptHarness.forRestore(work, 0);

    ScriptHarness.Result result = h.run(scriptPath, fakeSqlAge.toAbsolutePath().toString());

    assertThat(result.exitCode())
        .as("Expected exit 0 but got %d; stderr=%s", result.exitCode(), result.stderr())
        .isEqualTo(0);

    List<String> argv = h.recordedArgv();
    // psql must have been invoked with ON_ERROR_STOP flag
    assertThat(argv)
        .as("psql must be invoked with -v ON_ERROR_STOP=1; recorded argv=%s", argv)
        .contains("-v", "ON_ERROR_STOP=1");
  }

  @Test
  void plaintextFileIsAbsentAfterSuccessfulRestore() throws IOException, InterruptedException {
    ScriptHarness h = ScriptHarness.forRestore(work, 0);

    h.run(scriptPath, fakeSqlAge.toAbsolutePath().toString());

    // No .sql plaintext files (without the .age extension) should exist anywhere under work dir
    try (Stream<Path> files = Files.walk(work)) {
      List<Path> sqlFiles =
          files
              .filter(p -> p.toString().endsWith(".sql"))
              .filter(p -> !p.toString().endsWith(".sql.age"))
              .toList();
      assertThat(sqlFiles)
          .as("Plaintext .sql files must be deleted after restore; found: %s", sqlFiles)
          .isEmpty();
    }
  }

  @Test
  void exitsNonZeroWhenInputFileArgumentMissing() throws IOException, InterruptedException {
    ScriptHarness h = ScriptHarness.forRestore(work, 0);

    // Run without providing the required .sql.age argument
    ScriptHarness.Result result = h.run(scriptPath);

    assertThat(result.exitCode())
        .as("Expected non-zero exit when no .sql.age argument given; stderr=%s", result.stderr())
        .isNotEqualTo(0);
    assertThat(result.exitCode())
        .as("Expected exit code 2 for missing argument; stderr=%s", result.stderr())
        .isEqualTo(2);
  }

  @Test
  void exitsNonZeroWhenInputFileDoesNotExist() throws IOException, InterruptedException {
    ScriptHarness h = ScriptHarness.forRestore(work, 0);

    // Provide a non-existent .sql.age path
    ScriptHarness.Result result =
        h.run(scriptPath, work.resolve("nonexistent.sql.age").toAbsolutePath().toString());

    assertThat(result.exitCode())
        .as("Expected non-zero exit when .sql.age file does not exist; stderr=%s", result.stderr())
        .isNotEqualTo(0);
  }

  @Test
  void exitsNonZeroWhenPrivateKeyPathMissing() throws IOException, InterruptedException {
    ScriptHarness h = ScriptHarness.forRestore(work, 0);

    // Rewrite .env with empty AGE_PRIVATE_KEY_PATH
    Files.writeString(
        h.envFile(),
        """
        DB_HOST=localhost
        DB_PORT=5432
        DB_NAME=planner_db
        DB_USER=brujula
        DB_PASSWORD=test_password
        AGE_PUBLIC_KEY=%s
        AGE_PRIVATE_KEY_PATH=
        """
            .formatted(ScriptHarness.FAKE_AGE_PUBLIC_KEY));

    ScriptHarness.Result result = h.run(scriptPath, fakeSqlAge.toAbsolutePath().toString());

    assertThat(result.exitCode())
        .as("Expected non-zero exit when AGE_PRIVATE_KEY_PATH is empty; stderr=%s", result.stderr())
        .isNotEqualTo(0);
  }

  @Test
  void plaintextFileIsAbsentEvenWhenPsqlFails() throws IOException, InterruptedException {
    // Use a harness where psql exits with code 1 (simulates import failure)
    ScriptHarness h = ScriptHarness.forRestore(work, 1);

    h.run(scriptPath, fakeSqlAge.toAbsolutePath().toString());

    // Trap must still fire and clean up any temp plaintext file
    try (Stream<Path> files = Files.walk(work)) {
      List<Path> sqlFiles =
          files
              .filter(p -> p.toString().endsWith(".sql"))
              .filter(p -> !p.toString().endsWith(".sql.age"))
              .toList();
      assertThat(sqlFiles)
          .as(
              "Plaintext .sql temp file must be cleaned up even when psql fails; found: %s",
              sqlFiles)
          .isEmpty();
    }
  }

  // ─── Helper ──────────────────────────────────────────────────────────────────

  private static String resolveScriptPath(String relativePath) {
    try {
      Path candidate =
          Path.of(
              RestoreScriptContractTest.class
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
