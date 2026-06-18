package co.edu.uniquindio.brujula.imports.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * ArchUnit tests for the {@code imports} module (T3.4).
 *
 * <p>Enforces structural constraints:
 *
 * <ul>
 *   <li>T3.4-A: XLSX and JSON parser adapters live in {@code infrastructure.adapter.in.parsing}
 *   <li>T3.4-B: {@code ImportValidationProperties} uses {@code @ConfigurationProperties}
 * </ul>
 */
@AnalyzeClasses(
    packages = "co.edu.uniquindio.brujula.imports",
    importOptions = {ImportOption.DoNotIncludeTests.class})
class ImportsModuleArchitectureTest {

  /**
   * T3.4-A: parser adapters must reside in {@code infrastructure.adapter.in.parsing}.
   *
   * <p>This prevents accidental placement of parsing concerns in the application layer, which would
   * introduce framework (Apache POI, Jackson) dependencies there.
   */
  @ArchTest
  static final ArchRule T3_4_A_parser_adapters_reside_in_infrastructure_adapter_in_parsing =
      classes()
          .that()
          .haveSimpleNameEndingWith("ParserAdapter")
          .should()
          .resideInAPackage("..infrastructure.adapter.in.parsing..");

  /**
   * T3.4-B: {@code ImportValidationProperties} must be annotated with
   * {@code @ConfigurationProperties}.
   *
   * <p>This ensures the properties class is bound via Spring's type-safe properties mechanism
   * rather than scattered {@code @Value} annotations.
   */
  @ArchTest
  static final ArchRule T3_4_B_import_validation_properties_uses_configuration_properties =
      classes()
          .that()
          .haveSimpleName("ImportValidationProperties")
          .should()
          .beAnnotatedWith(ConfigurationProperties.class);
}
