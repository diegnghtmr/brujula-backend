package co.edu.uniquindio.brujula.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.Architectures;

/**
 * ArchUnit baseline ruleset for hexagonal architecture boundary enforcement.
 *
 * <p>All rules should pass with zero violations.
 */
@AnalyzeClasses(
    packages = "co.edu.uniquindio.brujula",
    importOptions = {ImportOption.DoNotIncludeTests.class})
class ArchitectureRulesTest {

  private static final String BASE_PKG = "co.edu.uniquindio.brujula";

  /**
   * Controllers must not access domain classes directly. Controllers may only call application
   * port/in interfaces.
   */
  @ArchTest
  static final ArchRule ARQ_001_controllers_do_not_access_domain_directly =
      noClasses()
          .that()
          .resideInAPackage("..adapter.in.rest..")
          .should()
          .dependOnClassesThat()
          .resideInAPackage("..domain..");

  /**
   * Domain classes are framework-free. Domain must not depend on Spring, Jakarta Persistence,
   * servlet types, or generated API model DTOs.
   *
   * <p>package-info files are excluded because they carry package-level annotations (e.g.
   * {@code @NamedInterface}) that are module-structure metadata, not domain logic.
   */
  @ArchTest
  static final ArchRule ARQ_002_domain_is_framework_free =
      noClasses()
          .that()
          .resideInAPackage("..domain..")
          .and()
          .doNotHaveSimpleName("package-info")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage(
              "org.springframework..",
              "jakarta.persistence..",
              "jakarta.servlet..",
              "..generated.model..",
              "..adapter..");

  /**
   * Application port/in interfaces must be interfaces (not classes or enums). All use-case entry
   * points are interfaces.
   */
  @ArchTest
  static final ArchRule ARQ_003_application_ports_in_are_interfaces =
      classes().that().resideInAPackage("..application.port.in..").should().beInterfaces();

  /**
   * Application layer must not depend on persistence adapters. Application depends on port.out
   * interfaces, not on concrete adapter implementations.
   */
  @ArchTest
  static final ArchRule ARQ_004_application_does_not_depend_on_persistence_adapters =
      noClasses()
          .that()
          .resideInAPackage("..application..")
          .should()
          .dependOnClassesThat()
          .resideInAPackage("..adapter.out.persistence..");

  /**
   * Generated API DTOs must not leak into application or domain layers. Generated model DTOs are
   * confined to infrastructure/adapter/in/rest adapters.
   */
  @ArchTest
  static final ArchRule ARQ_005_generated_dtos_confined_to_adapters =
      noClasses()
          .that()
          .resideInAnyPackage("..application..", "..domain..")
          .should()
          .dependOnClassesThat()
          .resideInAPackage("..generated..");

  /**
   * No cross-module access to internal persistence adapters. A module's adapter.out.persistence
   * sub-packages are internal; other modules may not import them.
   */
  @ArchTest
  static final ArchRule ARQ_006_no_cross_module_persistence_access =
      noClasses()
          .that()
          .resideInAPackage(BASE_PKG + "..")
          .and()
          .resideOutsideOfPackage("..adapter.out.persistence..")
          .should()
          .dependOnClassesThat()
          .resideInAPackage("..adapter.out.persistence..");

  /**
   * Module cycle enforcement is covered by ModularityTest (Spring Modulith verify()). This ArchTest
   * serves as a documentation marker; the actual enforcement is in ModularityTest.
   */
  @ArchTest
  static final ArchRule ARQ_007_no_module_cycles_see_modularity_test =
      noClasses()
          .that()
          .resideInAPackage(BASE_PKG + ".*")
          .should()
          .dependOnClassesThat()
          .resideInAPackage(BASE_PKG + ".*")
          // Allow cross-module access only to sharedkernel (open module) and audit (sink)
          .because(
              "ARQ-007 cycle enforcement is delegated to Spring Modulith verify() in"
                  + " ModularityTest");

  /**
   * Layered architecture: domain is innermost, application next, infrastructure outermost.
   * Infrastructure may depend on application; application may depend on domain; domain depends on
   * nothing outside itself.
   *
   * <p>allowEmptyShould(true) is set to avoid failures when a layer has no implementation classes
   * yet.
   */
  @ArchTest
  static final ArchRule ARQ_008_layered_architecture =
      Architectures.layeredArchitecture()
          .consideringOnlyDependenciesInLayers()
          .layer("Domain")
          .definedBy("..domain..")
          .layer("Application")
          .definedBy("..application..")
          .layer("Infrastructure")
          .definedBy("..infrastructure..")
          .whereLayer("Domain")
          .mayOnlyBeAccessedByLayers("Application", "Infrastructure")
          .whereLayer("Application")
          .mayOnlyBeAccessedByLayers("Infrastructure")
          .whereLayer("Infrastructure")
          .mayNotBeAccessedByAnyLayer()
          .allowEmptyShould(true);

  /**
   * JPA entities must be package-private (not public).
   *
   * <p>Strengthens the convention that adapter internals (JPA entities) are never public.
   * Package-private entities prevent accidental access from other modules and enforce the adapter
   * boundary (hexagonal architecture: only the persistence adapter touches JPA entities directly).
   *
   * <p>All 10 existing entities (programcatalog: 3, curriculum: 7) must pass this rule.
   */
  @ArchTest
  static final ArchRule ARQ_006b_jpa_entities_must_not_be_public =
      noClasses()
          .that()
          .areAnnotatedWith("jakarta.persistence.Entity")
          .should()
          .bePublic()
          .because("JPA entities are adapter internals and must be package-private (ARQ-006)");

  /**
   * Domain and application layers must not access external client adapters. {@code
   * adapter.out.client} packages contain driven adapters that call external services (e.g. Keycloak
   * Admin API). Only infrastructure/configuration classes may wire these adapters; domain and
   * application depend only on port interfaces, never on concrete client implementations.
   */
  @ArchTest
  static final ArchRule ARQ_009_no_domain_or_application_access_to_client_adapters =
      noClasses()
          .that()
          .resideInAnyPackage("..domain..", "..application..")
          .should()
          .dependOnClassesThat()
          .resideInAPackage("..adapter.out.client..");
}
