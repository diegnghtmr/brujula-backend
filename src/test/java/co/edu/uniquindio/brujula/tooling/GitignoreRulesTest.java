package co.edu.uniquindio.brujula.tooling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Contract test: verifies that .gitignore rules correctly ignore secrets/plaintext dumps and
 * correctly track migration SQL files and encrypted backup archives.
 *
 * <p>Uses {@code git check-ignore -v} as the oracle. Exit code 0 = file is ignored; exit code 1 =
 * file is NOT ignored (i.e. it would be tracked).
 *
 * <p>Guards with {@code assumeTrue} so the test SKIPS (not fails) in non-git contexts (e.g.
 * exported tarballs or CI without a git work tree).
 */
class GitignoreRulesTest {

  private static Path repoRoot;

  @BeforeAll
  static void setUp() throws IOException, InterruptedException {
    // Guard: git must be on PATH
    assumeTrue(isCommandAvailable("git"), "git not on PATH — skipping gitignore contract test");

    // Guard: we must be inside a git work tree
    Process p =
        new ProcessBuilder("git", "rev-parse", "--show-toplevel")
            .redirectErrorStream(true)
            .start();
    boolean finished = p.waitFor(5, TimeUnit.SECONDS);
    assumeTrue(finished && p.exitValue() == 0, "Not inside a git work tree — skipping");

    String toplevel = new String(p.getInputStream().readAllBytes()).trim();
    repoRoot = Paths.get(toplevel);
  }

  // ─── Files that MUST be ignored (git check-ignore exits 0) ─────────────────

  @Test
  void strayRootDumpSqlIsIgnored() throws IOException, InterruptedException {
    // A stray dump.sql at the repo root must be ignored (secret/plaintext dump)
    assertIgnored("dump.sql");
  }

  @Test
  void brujulaKeyTxtIsIgnored() throws IOException, InterruptedException {
    // The age private key file must be ignored
    assertIgnored("brujula-key.txt");
  }

  @Test
  void dotKeyFileIsIgnored() throws IOException, InterruptedException {
    assertIgnored("secret.key");
  }

  @Test
  void dotEnvIsIgnored() throws IOException, InterruptedException {
    assertIgnored(".env");
  }

  // ─── Files that MUST NOT be ignored (git check-ignore exits 1) ─────────────

  @Test
  void migrationSqlIsNotIgnored() throws IOException, InterruptedException {
    // Migration SQL files must always be tracked
    assertNotIgnored("src/main/resources/db/migration/V026__new_migration.sql");
  }

  @Test
  void encryptedBackupAgeIsNotIgnored() throws IOException, InterruptedException {
    // Encrypted backup archives must be tracked
    assertNotIgnored("backups/brujula-data-2026-06-17-ci-test.sql.age");
  }

  @Test
  void dotEnvExampleIsNotIgnored() throws IOException, InterruptedException {
    // .env.example is the committed template; must not be ignored
    assertNotIgnored(".env.example");
  }

  @Test
  void backupsReadmeIsNotIgnored() throws IOException, InterruptedException {
    // backups/README.md is the backup log template; must be tracked
    assertNotIgnored("backups/README.md");
  }

  // ─── Helpers ────────────────────────────────────────────────────────────────

  /**
   * Asserts that the given relative path is ignored by git (exit code 0 from git check-ignore).
   */
  private void assertIgnored(String relativePath) throws IOException, InterruptedException {
    int exitCode = runCheckIgnore(relativePath);
    assertThat(exitCode)
        .as("Expected '%s' to be IGNORED by .gitignore but git check-ignore returned %d", relativePath, exitCode)
        .isEqualTo(0);
  }

  /**
   * Asserts that the given relative path is NOT ignored by git (exit code 1 from git check-ignore
   * means the path is not matched by any ignore rule).
   */
  private void assertNotIgnored(String relativePath) throws IOException, InterruptedException {
    int exitCode = runCheckIgnore(relativePath);
    assertThat(exitCode)
        .as(
            "Expected '%s' to be NOT IGNORED (tracked) but git check-ignore returned %d",
            relativePath, exitCode)
        .isEqualTo(1);
  }

  private int runCheckIgnore(String relativePath) throws IOException, InterruptedException {
    // Use --no-index so we test rules against the path string itself, not the actual FS entry.
    // This allows testing paths that don't exist on disk yet.
    Process proc =
        new ProcessBuilder("git", "check-ignore", "--no-index", "-q", relativePath)
            .directory(repoRoot.toFile())
            .redirectErrorStream(false)
            .start();
    boolean finished = proc.waitFor(5, TimeUnit.SECONDS);
    assumeTrue(finished, "git check-ignore timed out for path: " + relativePath);
    return proc.exitValue();
  }

  private static boolean isCommandAvailable(String command) {
    try {
      Process p =
          new ProcessBuilder("which", command).redirectErrorStream(true).start();
      return p.waitFor(3, TimeUnit.SECONDS) && p.exitValue() == 0;
    } catch (Exception e) {
      return false;
    }
  }
}
