package co.edu.uniquindio.brujula.tooling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import co.edu.uniquindio.brujula.tooling.support.ScriptHarness;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Contract test for {@code scripts/db/backup.sh}.
 *
 * <p>Uses {@link ScriptHarness} to run the script with fake {@code age} and {@code pg_dump}
 * executables that record their argv. Tests verify pg_dump flags, output filename pattern, exit
 * codes, and plaintext cleanup — all without requiring real tools on the runner.
 *
 * <p>All tests are guarded with {@code assumeTrue(!windows)} so they SKIP on Windows CI (the bash
 * script is a POSIX artifact).
 */
class BackupScriptContractTest {

  @TempDir Path work;

  private String scriptPath;

  @BeforeEach
  void setUp() {
    assumeTrue(
        !System.getProperty("os.name", "").toLowerCase().startsWith("win"),
        "backup.sh is a POSIX script — skipping on Windows");

    // Resolve script path relative to the project root (two levels up from src/test)
    // The script lives at <repo-root>/scripts/db/backup.sh
    scriptPath = resolveScriptPath("scripts/db/backup.sh");
    assumeTrue(
        scriptPath != null && Files.exists(Path.of(scriptPath)),
        "backup.sh not found — script not yet created (expected RED state)");
  }

  // ─── Core contract assertions ────────────────────────────────────────────────

  @Test
  void excludeTableFlagsAreCorrect() throws IOException, InterruptedException {
    ScriptHarness h = ScriptHarness.forBackup(work);

    ScriptHarness.Result result = h.run(scriptPath, "ci-test");

    assertThat(result.exitCode())
        .as("Expected exit 0 but got %d; stderr=%s", result.exitCode(), result.stderr())
        .isEqualTo(0);

    List<String> argv = h.recordedArgv();
    assertThat(argv).contains("--exclude-table=platform.spring_session");
    assertThat(argv).contains("--exclude-table=platform.spring_session_attributes");
    assertThat(argv).contains("--exclude-table=platform.idempotency_record");
    assertThat(argv).contains("--data-only");
    assertThat(argv).contains("--column-inserts");
  }

  @Test
  void plaintextFileIsAbsentAfterBackup() throws IOException, InterruptedException {
    ScriptHarness h = ScriptHarness.forBackup(work);

    ScriptHarness.Result result = h.run(scriptPath, "ci-test");

    assertThat(result.exitCode())
        .as("Expected exit 0 but got %d; stderr=%s", result.exitCode(), result.stderr())
        .isEqualTo(0);

    // No .sql plaintext files should exist anywhere under the work dir
    try (Stream<Path> files = Files.walk(work)) {
      List<Path> sqlFiles =
          files
              .filter(p -> p.toString().endsWith(".sql"))
              .filter(p -> !p.toString().endsWith(".sql.age"))
              .toList();
      assertThat(sqlFiles)
          .as("Plaintext .sql files must be deleted after backup; found: %s", sqlFiles)
          .isEmpty();
    }
  }

  @Test
  void outputFileMatchesNamingPattern() throws IOException, InterruptedException {
    ScriptHarness h = ScriptHarness.forBackup(work);

    ScriptHarness.Result result = h.run(scriptPath, "ci-test");

    assertThat(result.exitCode())
        .as("Expected exit 0 but got %d; stderr=%s", result.exitCode(), result.stderr())
        .isEqualTo(0);

    Path backupsDir = work.resolve("backups");
    assertThat(backupsDir).isDirectory();

    try (Stream<Path> files = Files.list(backupsDir)) {
      List<Path> ageFiles =
          files.filter(p -> p.getFileName().toString().endsWith(".sql.age")).toList();

      assertThat(ageFiles).as("Expected exactly one .sql.age file in backups/").hasSize(1);

      String filename = ageFiles.get(0).getFileName().toString();
      // Pattern: brujula-data-YYYY-MM-DD-<motivo>.sql.age
      assertThat(filename)
          .as("Output file must match brujula-data-YYYY-MM-DD-<motivo>.sql.age pattern")
          .matches("brujula-data-\\d{4}-\\d{2}-\\d{2}-ci-test\\.sql\\.age");
    }
  }

  @Test
  void exitCodeIsZeroOnSuccess() throws IOException, InterruptedException {
    ScriptHarness h = ScriptHarness.forBackup(work);

    ScriptHarness.Result result = h.run(scriptPath, "ci-test");

    assertThat(result.exitCode()).as("stderr=%s", result.stderr()).isEqualTo(0);
  }

  // ─── Preflight failure scenarios ─────────────────────────────────────────────

  @Test
  void exitsNonZeroWhenEnvMissing() throws IOException, InterruptedException {
    ScriptHarness h = ScriptHarness.forBackup(work);

    // Override BRUJULA_ENV_FILE to a non-existent path
    ScriptHarness.Result result =
        h.runWithEnvOverrides(
            Map.of("BRUJULA_ENV_FILE", work.resolve("nonexistent.env").toString()),
            scriptPath,
            "ci-test");

    assertThat(result.exitCode())
        .as("Expected non-zero exit when .env is missing; stderr=%s", result.stderr())
        .isNotEqualTo(0);
  }

  @Test
  void exitsNonZeroWhenAgeNotOnPath() throws IOException, InterruptedException {
    ScriptHarness h = ScriptHarness.forBackup(work);
    h.removeAgeBinary(); // simulate age not installed

    ScriptHarness.Result result = h.run(scriptPath, "ci-test");

    assertThat(result.exitCode())
        .as("Expected non-zero exit when age is not on PATH; stderr=%s", result.stderr())
        .isNotEqualTo(0);
  }

  @Test
  void exitsNonZeroWhenPgDumpFails() throws IOException, InterruptedException {
    ScriptHarness h = ScriptHarness.forBackup(work);

    // Replace pg_dump stub with a failing version (exit 1)
    Path pgDumpPath = work.resolve("bin").resolve("pg_dump");
    Files.writeString(
        pgDumpPath,
        """
        #!/usr/bin/env bash
        printf '%s\\n' "$@" >> "$ARGV_LOG"
        exit 1
        """);
    // Re-apply exec permissions
    pgDumpPath.toFile().setExecutable(true);

    ScriptHarness.Result result = h.run(scriptPath, "ci-test");

    assertThat(result.exitCode())
        .as("Expected non-zero exit when pg_dump fails; stderr=%s", result.stderr())
        .isNotEqualTo(0);

    // Also assert no .sql.age was produced
    Path backupsDir = work.resolve("backups");
    if (Files.exists(backupsDir)) {
      try (Stream<Path> files = Files.list(backupsDir)) {
        List<Path> ageFiles =
            files.filter(p -> p.getFileName().toString().endsWith(".sql.age")).toList();
        assertThat(ageFiles).as("No .sql.age file should be created when pg_dump fails").isEmpty();
      }
    }
  }

  @Test
  void exitsNonZeroWhenAgePublicKeyMissing() throws IOException, InterruptedException {
    ScriptHarness h = ScriptHarness.forBackup(work);

    // Rewrite .env with empty AGE_PUBLIC_KEY
    Files.writeString(
        h.envFile(),
        """
        DB_HOST=localhost
        DB_PORT=5432
        DB_NAME=planner_db
        DB_USER=brujula
        DB_PASSWORD=test_password
        AGE_PUBLIC_KEY=
        AGE_PRIVATE_KEY_PATH=%s/fake_private_key.txt
        """
            .formatted(work.toAbsolutePath()));

    ScriptHarness.Result result = h.run(scriptPath, "ci-test");

    assertThat(result.exitCode())
        .as("Expected non-zero exit when AGE_PUBLIC_KEY is empty; stderr=%s", result.stderr())
        .isNotEqualTo(0);
  }

  // ─── Helper ──────────────────────────────────────────────────────────────────

  /**
   * Resolves a script path by walking up from the current class location to the repo root. Falls
   * back to using {@code user.dir} if class-location traversal fails.
   */
  private static String resolveScriptPath(String relativePath) {
    try {
      // Try to find the repo root by looking for build.gradle.kts
      Path candidate =
          Path.of(
              BackupScriptContractTest.class
                  .getProtectionDomain()
                  .getCodeSource()
                  .getLocation()
                  .toURI());
      // Walk up until we find build.gradle.kts (repo root marker)
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
    // Fallback: use working directory
    return Path.of(System.getProperty("user.dir"), relativePath).toAbsolutePath().toString();
  }
}
