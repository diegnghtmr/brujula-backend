package co.edu.uniquindio.brujula.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * {@code programcatalog} module boundary enforcement.
 *
 * <p>The {@code programcatalog} module MUST only depend on {@code sharedkernel} and {@code audit}.
 * It MUST NOT import anything from {@code curriculum} (or any other module) directly.
 *
 * <p>This mirrors the {@code package-info.java} {@code allowedDependencies} declaration, which
 * Spring Modulith enforces at runtime; here ArchUnit enforces at test time.
 *
 * <p><strong>Supplementary denylist — NOT the authoritative enforcement.</strong> These three rules
 * check that known sibling modules are not imported, but they do NOT form a positive allowlist: a
 * new module (e.g. {@code enrollment}) added in the future would pass silently. The authoritative
 * positive constraint is {@code ModularityTest.verifyModularStructure()}, which uses Spring
 * Modulith's {@code allowedDependencies = {"sharedkernel", "audit"}} declaration in {@code
 * package-info.java} to reject ANY dependency not explicitly permitted. Run {@code ModularityTest}
 * alongside these rules for complete coverage.
 */
@AnalyzeClasses(
    packages = "co.edu.uniquindio.brujula.programcatalog",
    importOptions = {ImportOption.DoNotIncludeTests.class})
class ProgramCatalogBoundaryTest {

  private static final String CURRICULUM_PKG = "co.edu.uniquindio.brujula.curriculum..";
  private static final String IDENTITY_PKG = "co.edu.uniquindio.brujula.identity..";
  private static final String ADMINISTRATION_PKG = "co.edu.uniquindio.brujula.administration..";

  /**
   * programcatalog must not import curriculum classes directly.
   *
   * <p>The module boundary ensures no coupling from catalog to curriculum (curriculum READS from
   * catalog, not the other way around).
   */
  @ArchTest
  static final ArchRule programcatalog_does_not_depend_on_curriculum =
      noClasses()
          .that()
          .resideInAPackage("co.edu.uniquindio.brujula.programcatalog..")
          .should()
          .dependOnClassesThat()
          .resideInAPackage(CURRICULUM_PKG)
          .because(
              "CROSS-002: programcatalog depends only on sharedkernel and audit; "
                  + "curriculum depends on programcatalog, not vice versa");

  /** programcatalog must not import identity module classes directly. */
  @ArchTest
  static final ArchRule programcatalog_does_not_depend_on_identity =
      noClasses()
          .that()
          .resideInAPackage("co.edu.uniquindio.brujula.programcatalog..")
          .should()
          .dependOnClassesThat()
          .resideInAPackage(IDENTITY_PKG)
          .because("CROSS-002: programcatalog must only depend on sharedkernel and audit");

  /** programcatalog must not import administration module classes directly. */
  @ArchTest
  static final ArchRule programcatalog_does_not_depend_on_administration =
      noClasses()
          .that()
          .resideInAPackage("co.edu.uniquindio.brujula.programcatalog..")
          .should()
          .dependOnClassesThat()
          .resideInAPackage(ADMINISTRATION_PKG)
          .because("CROSS-002: programcatalog must only depend on sharedkernel and audit");
}
