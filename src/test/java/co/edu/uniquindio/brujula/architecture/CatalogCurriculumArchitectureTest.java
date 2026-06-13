package co.edu.uniquindio.brujula.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Consolidated ArchUnit coverage for the {@code programcatalog} and {@code curriculum} modules
 * (Spring Modulith + ArchUnit).
 *
 * <h2>Coverage summary for the two modules</h2>
 *
 * <h3>Rules in this file</h3>
 *
 * <ul>
 *   <li>{@link #curriculum_does_not_depend_on_identity} — curriculum boundary denylist (identity
 *       module)
 *   <li>{@link #curriculum_does_not_depend_on_administration} — curriculum boundary denylist
 *       (administration module)
 *   <li>{@link #curriculum_does_not_depend_on_jpa_entities_of_programcatalog} — curriculum may
 *       reference programcatalog application-layer types only (no JPA entity leakage)
 * </ul>
 *
 * <h3>Rules confirmed in sibling tests (no duplication)</h3>
 *
 * <ul>
 *   <li>REST adapters do not access domain — {@code ArchitectureRulesTest}
 *   <li>JPA entities are package-private — {@code ArchitectureRulesTest}
 *   <li>programcatalog boundary denylist — {@code ProgramCatalogBoundaryTest}
 *   <li>PrerequisiteAcyclicityService purity — {@code PrerequisiteAcyclicityServicePurityTest}
 *   <li>Spring Modulith {@code verify()} (authoritative positive allowlist for both modules) —
 *       {@code ModularityTest}
 * </ul>
 *
 * <p>Together with {@code ModularityTest.verifyModularStructure()}, this class provides complete
 * module boundary coverage: {@code programcatalog} depends only on {@code sharedkernel + audit};
 * {@code curriculum} depends only on {@code sharedkernel + audit + programcatalog}.
 */
@AnalyzeClasses(
    packages = "co.edu.uniquindio.brujula.curriculum",
    importOptions = {ImportOption.DoNotIncludeTests.class})
class CatalogCurriculumArchitectureTest {

  private static final String IDENTITY_PKG = "co.edu.uniquindio.brujula.identity..";
  private static final String ADMINISTRATION_PKG = "co.edu.uniquindio.brujula.administration..";
  private static final String PROGRAMCATALOG_PERSISTENCE_PKG =
      "co.edu.uniquindio.brujula.programcatalog..infrastructure.adapter.out.persistence..";

  /**
   * Only the curriculum REST adapter (infrastructure.adapter.in.rest) may import {@code identity}
   * module classes; application and domain layers must not.
   *
   * <p>Write endpoints in {@code CurriculumController} resolve the actor UUID via {@link
   * co.edu.uniquindio.brujula.identity.application.port.in.GetCurrentUserQuery}. The controller
   * lives in {@code infrastructure.adapter.in.rest} — the only layer permitted to cross this
   * boundary (mirrors the offering module's pattern). Application services and domain objects must
   * remain identity-free.
   *
   * <p>The authoritative positive constraint is {@code ModularityTest.verifyModularStructure()},
   * backed by the {@code allowedDependencies} declaration in {@code
   * co.edu.uniquindio.brujula.curriculum.package-info}.
   */
  @ArchTest
  static final ArchRule curriculum_does_not_depend_on_identity =
      noClasses()
          .that()
          .resideInAPackage("co.edu.uniquindio.brujula.curriculum..")
          .and()
          .resideOutsideOfPackage(
              "co.edu.uniquindio.brujula.curriculum.infrastructure.adapter.in.rest..")
          .should()
          .dependOnClassesThat()
          .resideInAPackage(IDENTITY_PKG)
          .because(
              "CROSS-002: only the curriculum REST adapter may depend on identity (for actor "
                  + "resolution in write endpoints); application and domain layers must not");

  /**
   * {@code curriculum} must not import {@code administration} module classes.
   *
   * <p>Symmetric to {@link #curriculum_does_not_depend_on_identity}. The administration module owns
   * staff/user-management logic; curriculum has no business coupling to it.
   */
  @ArchTest
  static final ArchRule curriculum_does_not_depend_on_administration =
      noClasses()
          .that()
          .resideInAPackage("co.edu.uniquindio.brujula.curriculum..")
          .should()
          .dependOnClassesThat()
          .resideInAPackage(ADMINISTRATION_PKG)
          .because(
              "CROSS-002: curriculum depends only on sharedkernel, audit, and programcatalog; "
                  + "any administration dependency is a boundary violation");

  /**
   * {@code curriculum} must not access {@code programcatalog} JPA entities directly.
   *
   * <p>Even though {@code curriculum} is allowed to depend on {@code programcatalog} (via its
   * application-layer ports and DTOs), it must never import programcatalog's internal persistence
   * classes. Cross-module JPA entity access is forbidden by the adapter-boundary convention.
   *
   * <p>Curriculum accesses programcatalog concepts only via {@code application.dto}, {@code
   * application.port.in}, and {@code application.port.out} — never via JPA entities.
   */
  @ArchTest
  static final ArchRule curriculum_does_not_depend_on_jpa_entities_of_programcatalog =
      noClasses()
          .that()
          .resideInAPackage("co.edu.uniquindio.brujula.curriculum..")
          .should()
          .dependOnClassesThat()
          .resideInAPackage(PROGRAMCATALOG_PERSISTENCE_PKG)
          .because(
              "CROSS-002 / ARQ-006: curriculum must access programcatalog only via its "
                  + "application-layer interfaces and DTOs, never via JPA persistence internals");
}
