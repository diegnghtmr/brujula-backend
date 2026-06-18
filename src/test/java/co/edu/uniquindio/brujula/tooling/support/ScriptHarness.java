package co.edu.uniquindio.brujula.tooling.support;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Hermetic test harness for shell script contract tests.
 *
 * <p>Creates a temporary work directory containing:
 *
 * <ul>
 *   <li>{@code work/bin/} — fake {@code age} and {@code pg_dump} POSIX stub executables that record
 *       their argv to {@code ARGV_LOG} and simulate success (exit 0).
 *   <li>{@code work/.env} — a minimal env file with fake DB vars and a test {@code AGE_PUBLIC_KEY}.
 * </ul>
 *
 * <p>The stub executables prepend {@code work/bin} to {@code PATH} so that the script under test
 * picks up the fakes instead of the real tools. This keeps {@code ./gradlew check} green even when
 * {@code age} and {@code pg_dump} are not installed on the CI runner.
 *
 * <p>Usage:
 *
 * <pre>{@code
 * @TempDir Path work;
 *
 * @Test
 * void someScriptTest() throws Exception {
 *     ScriptHarness h = ScriptHarness.forBackup(work);
 *     ScriptHarness.Result r = h.run("/path/to/backup.sh", "ci-test");
 *     assertThat(r.exitCode()).isEqualTo(0);
 *     assertThat(h.recordedArgv()).contains("--data-only");
 * }
 * }</pre>
 *
 * <p>Design constraints: no Spring, no Testcontainers, pure JDK + JUnit 5.
 */
public final class ScriptHarness {

  /** The fake public key written to the temp .env — safe placeholder value. */
  public static final String FAKE_AGE_PUBLIC_KEY =
      "age1faketestkey0000000000000000000000000000000000000000000000000";

  private final Path workDir;
  private final Path binDir;
  private final Path argvLog;
  private final Path envFile;

  private ScriptHarness(Path workDir, Path binDir, Path argvLog, Path envFile) {
    this.workDir = workDir;
    this.binDir = binDir;
    this.argvLog = argvLog;
    this.envFile = envFile;
  }

  /**
   * Creates a harness with {@code age} and {@code pg_dump} stubs suitable for backup script tests.
   *
   * @param workDir the {@code @TempDir} provided by JUnit 5
   * @return a configured {@code ScriptHarness}
   */
  public static ScriptHarness forBackup(Path workDir) throws IOException {
    Path binDir = workDir.resolve("bin");
    Files.createDirectories(binDir);

    Path argvLog = workDir.resolve("argv.log");
    Path envFile = workDir.resolve(".env");

    // Write pg_dump stub: records argv, exits 0
    writeStub(
        binDir.resolve("pg_dump"),
        """
        #!/usr/bin/env bash
        printf '%s\\n' "$@" >> "$ARGV_LOG"
        exit 0
        """);

    // Write age stub: records argv, touches the -o target so output-file assertions pass, exits 0
    writeStub(
        binDir.resolve("age"),
        """
        #!/usr/bin/env bash
        printf '%s\\n' "$@" >> "$ARGV_LOG"
        # Simulate age creating the output file by touching the -o argument
        out=""
        for arg; do
          if [ "$prev" = "-o" ]; then out="$arg"; fi
          prev="$arg"
        done
        if [ -n "$out" ]; then
          mkdir -p "$(dirname "$out")"
          touch "$out"
        fi
        exit 0
        """);

    // Write .env with fake DB vars + fake age public key
    Files.writeString(
        envFile,
        """
        DB_HOST=localhost
        DB_PORT=5432
        DB_NAME=planner_db
        DB_USER=brujula
        DB_PASSWORD=test_password
        AGE_PUBLIC_KEY=%s
        AGE_PRIVATE_KEY_PATH=%s/fake_private_key.txt
        """
            .formatted(FAKE_AGE_PUBLIC_KEY, workDir.toAbsolutePath()));

    // Write a fake private key placeholder (restore tests need a readable file)
    Path fakePrivateKey = workDir.resolve("fake_private_key.txt");
    Files.writeString(fakePrivateKey, "# fake age private key for testing\n");

    return new ScriptHarness(workDir, binDir, argvLog, envFile);
  }

  /**
   * Creates a harness with {@code age}, {@code pg_dump}, AND {@code psql} stubs for restore script
   * tests.
   *
   * @param workDir the {@code @TempDir} provided by JUnit 5
   * @param psqlExitCode the exit code the {@code psql} stub should return (0 = success, 1 =
   *     failure)
   * @return a configured {@code ScriptHarness}
   */
  public static ScriptHarness forRestore(Path workDir, int psqlExitCode) throws IOException {
    ScriptHarness base = forBackup(workDir);

    // Add age -d (decrypt) stub behaviour: on restore age reads stdin/file, writes to -o target
    // Reuse the same age stub (already handles -o), but also add psql stub
    writeStub(
        base.binDir.resolve("psql"),
        """
        #!/usr/bin/env bash
        printf '%%s\\n' "$@" >> "$ARGV_LOG"
        exit %d
        """
            .formatted(psqlExitCode));

    return base;
  }

  /** Convenience factory for restore harness with a succeeding {@code psql} stub (exit 0). */
  public static ScriptHarness forRestore(Path workDir) throws IOException {
    return forRestore(workDir, 0);
  }

  /**
   * Removes the {@code age} stub from {@code work/bin/} to simulate age not being on PATH. Call
   * this AFTER creating the harness when you need to test missing-tool scenarios.
   */
  public void removeAgeBinary() throws IOException {
    Files.deleteIfExists(binDir.resolve("age"));
  }

  /**
   * Removes the {@code pg_dump} stub from {@code work/bin/} to simulate pg_dump not being on PATH.
   */
  public void removePgDumpBinary() throws IOException {
    Files.deleteIfExists(binDir.resolve("pg_dump"));
  }

  /** Returns the contents of the work directory, useful for asserting produced files. */
  public Path workDir() {
    return workDir;
  }

  /** Returns the path to the temp {@code .env} file used by this harness. */
  public Path envFile() {
    return envFile;
  }

  /**
   * Returns the path to the {@code argv.log} file where stubs record their arguments. The file may
   * not exist if no stub was invoked.
   */
  public Path argvLog() {
    return argvLog;
  }

  /** Returns all lines recorded in {@code argv.log}, or an empty list if the log does not exist. */
  public List<String> recordedArgv() throws IOException {
    if (!Files.exists(argvLog)) {
      return List.of();
    }
    return Files.readAllLines(argvLog);
  }

  /**
   * Runs the given shell script with optional arguments, returning a {@link Result}.
   *
   * <p>Environment configured:
   *
   * <ul>
   *   <li>{@code PATH=work/bin:$ORIGINAL_PATH} — stubs shadow real tools
   *   <li>{@code BRUJULA_ENV_FILE=work/.env} — script reads this instead of repo-root .env
   *   <li>{@code ARGV_LOG=work/argv.log} — stubs write recorded args here
   *   <li>{@code REPO_ROOT=work/} — script writes output files under work/backups/
   * </ul>
   *
   * @param scriptPath absolute path to the shell script to execute
   * @param args additional arguments passed to the script (e.g. motivo)
   * @return the {@link Result} with exit code, stdout, and stderr
   */
  public Result run(String scriptPath, String... args) throws IOException, InterruptedException {
    return runWithEnvOverrides(Map.of(), scriptPath, args);
  }

  /**
   * Runs the shell script with additional environment variable overrides on top of the default
   * harness environment. Useful for testing missing-var scenarios (e.g. empty AGE_PUBLIC_KEY).
   */
  public Result runWithEnvOverrides(Map<String, String> extraEnv, String scriptPath, String... args)
      throws IOException, InterruptedException {

    String originalPath = System.getenv("PATH");
    String newPath = binDir.toAbsolutePath() + ":" + (originalPath != null ? originalPath : "");

    // Ensure backups/ subdirectory exists so the script can write output there
    Files.createDirectories(workDir.resolve("backups"));

    // Build command: bash <script> [args...]
    List<String> command = new java.util.ArrayList<>();
    command.add("bash");
    command.add(scriptPath);
    command.addAll(List.of(args));

    ProcessBuilder pb = new ProcessBuilder(command);
    pb.directory(workDir.toFile());

    Map<String, String> env = pb.environment();
    env.put("PATH", newPath);
    env.put("BRUJULA_ENV_FILE", envFile.toAbsolutePath().toString());
    env.put("ARGV_LOG", argvLog.toAbsolutePath().toString());
    env.put("REPO_ROOT", workDir.toAbsolutePath().toString());
    env.put("HOME", workDir.toAbsolutePath().toString()); // isolate ~ expansion

    // Apply any caller-provided overrides last (allows blanking out vars)
    env.putAll(extraEnv);

    Process proc = pb.start();
    String stdout = new String(proc.getInputStream().readAllBytes());
    String stderr = new String(proc.getErrorStream().readAllBytes());
    boolean finished = proc.waitFor(30, TimeUnit.SECONDS);

    if (!finished) {
      proc.destroyForcibly();
      throw new AssertionError(
          "Script timed out after 30 seconds: "
              + scriptPath
              + "\nstdout="
              + stdout
              + "\nstderr="
              + stderr);
    }

    return new Result(proc.exitValue(), stdout, stderr);
  }

  // ─── Private helpers ────────────────────────────────────────────────────────

  private static void writeStub(Path path, String content) throws IOException {
    Files.writeString(path, content);
    Set<PosixFilePermission> perms =
        EnumSet.of(
            PosixFilePermission.OWNER_READ,
            PosixFilePermission.OWNER_WRITE,
            PosixFilePermission.OWNER_EXECUTE,
            PosixFilePermission.GROUP_READ,
            PosixFilePermission.GROUP_EXECUTE,
            PosixFilePermission.OTHERS_READ,
            PosixFilePermission.OTHERS_EXECUTE);
    Files.setPosixFilePermissions(path, perms);
  }

  // ─── Result record ───────────────────────────────────────────────────────────

  /**
   * Immutable result of running a shell script via the harness.
   *
   * @param exitCode the process exit code
   * @param stdout captured standard output
   * @param stderr captured standard error
   */
  public record Result(int exitCode, String stdout, String stderr) {}
}
