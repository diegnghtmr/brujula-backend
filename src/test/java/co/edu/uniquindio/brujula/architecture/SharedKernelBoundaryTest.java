package co.edu.uniquindio.brujula.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * sharedkernel.IdempotencyStorePort boundary enforcement.
 *
 * <p>Asserts that {@code sharedkernel.application.port.out.IdempotencyStorePort} is an interface
 * and imports nothing from any module domain (zero coupling to any bounded context).
 */
@AnalyzeClasses(
    packages = "co.edu.uniquindio.brujula.sharedkernel",
    importOptions = {ImportOption.DoNotIncludeTests.class})
class SharedKernelBoundaryTest {

  /**
   * IdempotencyStorePort must be an interface (not a class or enum). Declared in sharedkernel
   * application port.out to serve as a cross-module output port.
   */
  @ArchTest
  static final ArchRule sharedkernel_idempotency_port_is_an_interface =
      classes()
          .that()
          .haveSimpleName("IdempotencyStorePort")
          .should()
          .beInterfaces()
          .because(
              "ADR-F3-001: IdempotencyStorePort is an output port and must be an interface"
                  + " (ARQ-003 extended to sharedkernel ports)");

  /**
   * IdempotencyStorePort must not import any administration domain classes.
   *
   * <p>A pure relocation of the typed port was impossible (it was coupled to StaffAccount); the
   * generalized opaque-JSON contract removes all module-domain coupling.
   */
  @ArchTest
  static final ArchRule sharedkernel_idempotency_port_has_no_administration_imports =
      noClasses()
          .that()
          .haveSimpleName("IdempotencyStorePort")
          .should()
          .dependOnClassesThat()
          .resideInAPackage("co.edu.uniquindio.brujula.administration..")
          .because(
              "ADR-F3-001: the generalized port must be opaque (no module-domain coupling);"
                  + " each caller owns its own (de)serialization");

  /**
   * sharedkernel must not depend on any other module domain. Its {@code allowedDependencies = {}}
   * means it imports only {@code java.*} in port interfaces.
   */
  @ArchTest
  static final ArchRule sharedkernel_ports_do_not_depend_on_module_domains =
      noClasses()
          .that()
          .resideInAPackage("co.edu.uniquindio.brujula.sharedkernel.application..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage(
              "co.edu.uniquindio.brujula.administration..",
              "co.edu.uniquindio.brujula.audit..",
              "co.edu.uniquindio.brujula.curriculum..",
              "co.edu.uniquindio.brujula.programcatalog..",
              "co.edu.uniquindio.brujula.identity..",
              "co.edu.uniquindio.brujula.progress..")
          .because(
              "ADR-F3-001: sharedkernel allowedDependencies={} — the application layer"
                  + " must import only java.* types");
}
