package co.edu.uniquindio.brujula.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * {@code progress.domain} must have zero Spring/JPA/HTTP/generated imports.
 *
 * <p>Mirrors {@link PrerequisiteAcyclicityServicePurityTest} for the {@code progress} module.
 */
@AnalyzeClasses(
    packages = "co.edu.uniquindio.brujula.progress.domain",
    importOptions = {ImportOption.DoNotIncludeTests.class})
class ProgressDomainPurityTest {

  /** progress.domain must not import Spring Framework, JPA, servlet, or generated model types. */
  @ArchTest
  static final ArchRule progress_domain_must_not_import_spring_jpa_or_generated =
      noClasses()
          .that()
          .resideInAPackage("co.edu.uniquindio.brujula.progress.domain..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage(
              "org.springframework..",
              "jakarta.persistence..",
              "jakarta.servlet..",
              "..generated.model..",
              "..adapter..");
}
