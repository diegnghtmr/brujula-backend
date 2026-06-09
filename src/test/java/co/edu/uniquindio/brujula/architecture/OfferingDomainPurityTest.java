package co.edu.uniquindio.brujula.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * WU1-TDD RED: ArchUnit test verifying the offering domain is framework-free (ARQ-002).
 *
 * <p>No Spring or JPA annotations/classes may reside in {@code offering.domain}.
 */
@AnalyzeClasses(
    packages = "co.edu.uniquindio.brujula.offering.domain",
    importOptions = ImportOption.DoNotIncludeTests.class)
class OfferingDomainPurityTest {

  @ArchTest
  static final ArchRule domainHasNoSpringDependencies =
      noClasses()
          .that()
          .resideInAPackage("co.edu.uniquindio.brujula.offering.domain..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage(
              "org.springframework..", "jakarta.persistence..", "jakarta.enterprise..");

  @ArchTest
  static final ArchRule domainHasNoJpaDependencies =
      noClasses()
          .that()
          .resideInAPackage("co.edu.uniquindio.brujula.offering.domain..")
          .should()
          .dependOnClassesThat()
          .haveSimpleNameEndingWith("Entity")
          .orShould()
          .dependOnClassesThat()
          .haveSimpleNameEndingWith("Repository");
}
