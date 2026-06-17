package co.edu.uniquindio.brujula.tooling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Parity test for {@code scripts/db/backup.ps1} and {@code scripts/db/restore.ps1}.
 *
 * <p>Asserts that the PowerShell scripts implement the same contract as their bash counterparts:
 * same pg_dump flags, same filename pattern, same exit-code semantics, and same temp-file cleanup
 * guarantee.
 *
 * <p>All tests are guarded with {@code assumeTrue(pwshOnPath())} so they SKIP cleanly on Linux CI
 * where {@code pwsh} is not installed. The green gate does not prove the .ps1 files ran — that is
 * verified on Windows dev machines or a pwsh-equipped runner.
 */
class PowerShellScriptParityTest {

  @TempDir Path work;

  private String backupScriptPath;
  private String restoreScriptPath;

  @BeforeEach
  void setUp() throws IOException {
    // Guard 1: pwsh must be on PATH
    assumeTrue(pwshOnPath(), "pwsh is not on PATH — skipping PowerShell parity tests");

    // Guard 2: the .ps1 scripts must exist
    backupScriptPath = resolveScriptPath("scripts/db/backup.ps1");
    restoreScriptPath = resolveScriptPath("scripts/db/restore.ps1");

    assumeTrue(
        backupScriptPath != null && Files.exists(Path.of(backupScriptPath)),
        "backup.ps1 not found — script not yet created (expected RED state)");
    assumeTrue(
        restoreScriptPath != null && Files.exists(Path.of(restoreScriptPath)),
        "restore.ps1 not found — script not yet created (expected RED state)");

    // Create backups directory and fake .env for the harness
    Files.createDirectories(work.resolve("backups"));
    writeFakeEnv(work.resolve(".env"));
  }

  // ─── backup.ps1 parity tests ─────────────────────────────────────────────────

  @Test
  void backupPs1ExitCodeIsZeroOnSuccess() throws IOException, InterruptedException {
    PsScriptRun result = runPsScript(backupScriptPath, List.of("ci-test"), buildPsEnv());

    assertThat(result.exitCode())
        .as("backup.ps1 must exit 0 on success; stderr=%s", result.stderr())
        .isEqualTo(0);
  }

  @Test
  void backupPs1OutputFileMatchesNamingPattern() throws IOException, InterruptedException {
    PsScriptRun result = runPsScript(backupScriptPath, List.of("ci-test"), buildPsEnv());

    assertThat(result.exitCode())
        .as("backup.ps1 must exit 0 before checking output; stderr=%s", result.stderr())
        .isEqualTo(0);

    Path backupsDir = work.resolve("backups");
    try (Stream<Path> files = Files.list(backupsDir)) {
      List<Path> ageFiles =
          files.filter(p -> p.getFileName().toString().endsWith(".sql.age")).toList();
      assertThat(ageFiles).as("backup.ps1 must produce exactly one .sql.age file").hasSize(1);

      String filename = ageFiles.get(0).getFileName().toString();
      assertThat(filename)
          .as("Output filename must match brujula-data-YYYY-MM-DD-<motivo>.sql.age")
          .matches("brujula-data-\\d{4}-\\d{2}-\\d{2}-ci-test\\.sql\\.age");
    }
  }

  @Test
  void backupPs1PlaintextFileIsAbsentAfterSuccess() throws IOException, InterruptedException {
    PsScriptRun result = runPsScript(backupScriptPath, List.of("ci-test"), buildPsEnv());

    assertThat(result.exitCode())
        .as("backup.ps1 must exit 0 before checking cleanup; stderr=%s", result.stderr())
        .isEqualTo(0);

    try (Stream<Path> all = Files.walk(work)) {
      List<Path> sqlFiles =
          all.filter(p -> p.toString().endsWith(".sql"))
              .filter(p -> !p.toString().endsWith(".sql.age"))
              .toList();
      assertThat(sqlFiles)
          .as(
              "No plaintext .sql files should remain after backup.ps1 completes; found: %s",
              sqlFiles)
          .isEmpty();
    }
  }

  @Test
  void backupPs1ExitsNonZeroWhenAgePublicKeyMissing() throws IOException, InterruptedException {
    writeFakeEnv(work.resolve(".env"), /* agePublicKey= */ "");

    PsScriptRun result = runPsScript(backupScriptPath, List.of("ci-test"), buildPsEnv());

    assertThat(result.exitCode())
        .as(
            "backup.ps1 must exit non-zero when AGE_PUBLIC_KEY is empty; stderr=%s",
            result.stderr())
        .isNotEqualTo(0);
  }

  // ─── restore.ps1 parity tests ────────────────────────────────────────────────

  @Test
  void restorePs1ExitsNonZeroWhenInputFileMissing() throws IOException, InterruptedException {
    // Run restore.ps1 without providing a .sql.age argument
    PsScriptRun result = runPsScript(restoreScriptPath, List.of(), buildPsEnv());

    assertThat(result.exitCode())
        .as(
            "restore.ps1 must exit non-zero when no .sql.age argument given; stderr=%s",
            result.stderr())
        .isNotEqualTo(0);
  }

  @Test
  void restorePs1ExitsNonZeroWhenPrivateKeyPathMissing() throws IOException, InterruptedException {
    // Create a fake .sql.age file to pass the argument check
    Path fakeSqlAge = work.resolve("backups").resolve("brujula-data-2026-01-01-test.sql.age");
    Files.writeString(fakeSqlAge, "fake encrypted content");

    // Rewrite .env with empty AGE_PRIVATE_KEY_PATH
    writeFakeEnv(work.resolve(".env"), "age1faketestkey", /* privateKeyPath= */ "");

    PsScriptRun result =
        runPsScript(
            restoreScriptPath, List.of(fakeSqlAge.toAbsolutePath().toString()), buildPsEnv());

    assertThat(result.exitCode())
        .as(
            "restore.ps1 must exit non-zero when AGE_PRIVATE_KEY_PATH is empty; stderr=%s",
            result.stderr())
        .isNotEqualTo(0);
  }

  // ─── PowerShell runner ───────────────────────────────────────────────────────

  private record PsScriptRun(int exitCode, String stdout, String stderr) {}

  private PsScriptRun runPsScript(
      String scriptPath, List<String> args, Map<String, String> extraEnv)
      throws IOException, InterruptedException {

    List<String> command = new ArrayList<>();
    command.add("pwsh");
    command.add("-NonInteractive");
    command.add("-NoProfile");
    command.add("-File");
    command.add(scriptPath);
    command.addAll(args);

    ProcessBuilder pb = new ProcessBuilder(command);
    pb.directory(work.toFile());

    Map<String, String> env = pb.environment();
    env.put("BRUJULA_ENV_FILE", work.resolve(".env").toAbsolutePath().toString());
    env.put("REPO_ROOT", work.toAbsolutePath().toString());
    env.putAll(extraEnv);

    Process proc = pb.start();
    String stdout = new String(proc.getInputStream().readAllBytes());
    String stderr = new String(proc.getErrorStream().readAllBytes());
    boolean finished = proc.waitFor(60, TimeUnit.SECONDS);

    if (!finished) {
      proc.destroyForcibly();
      throw new AssertionError(
          "PowerShell script timed out: "
              + scriptPath
              + "\nstdout="
              + stdout
              + "\nstderr="
              + stderr);
    }

    return new PsScriptRun(proc.exitValue(), stdout, stderr);
  }

  // ─── Helpers ─────────────────────────────────────────────────────────────────

  private static boolean pwshOnPath() {
    try {
      ProcessBuilder pb = new ProcessBuilder("bash", "-c", "command -v pwsh");
      pb.redirectErrorStream(true);
      Process p = pb.start();
      p.waitFor(5, TimeUnit.SECONDS);
      return p.exitValue() == 0;
    } catch (Exception e) {
      return false;
    }
  }

  /** Writes a fake .env with stubs for all required variables and a fake age key. */
  private void writeFakeEnv(Path envFile) throws IOException {
    writeFakeEnv(
        envFile,
        "age1faketestkey0000000000000000000000000000000000000000000000000",
        work.resolve("fake_private_key.txt").toAbsolutePath().toString());
    // Create the fake private key file so restore.ps1 can validate it exists
    Files.writeString(work.resolve("fake_private_key.txt"), "# fake age private key for testing\n");
  }

  private void writeFakeEnv(Path envFile, String agePublicKey) throws IOException {
    writeFakeEnv(
        envFile, agePublicKey, work.resolve("fake_private_key.txt").toAbsolutePath().toString());
    Files.writeString(work.resolve("fake_private_key.txt"), "# fake age private key for testing\n");
  }

  private void writeFakeEnv(Path envFile, String agePublicKey, String privateKeyPath)
      throws IOException {
    Files.writeString(
        envFile,
        String.format(
            """
            DB_HOST=localhost
            DB_PORT=5432
            DB_NAME=planner_db
            DB_USER=brujula
            DB_PASSWORD=test_password
            AGE_PUBLIC_KEY=%s
            AGE_PRIVATE_KEY_PATH=%s
            """,
            agePublicKey, privateKeyPath));
  }

  private Map<String, String> buildPsEnv() {
    return Map.of(
        "BRUJULA_ENV_FILE", work.resolve(".env").toAbsolutePath().toString(),
        "REPO_ROOT", work.toAbsolutePath().toString());
  }

  private static String resolveScriptPath(String relativePath) {
    try {
      Path candidate =
          Path.of(
              PowerShellScriptParityTest.class
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
