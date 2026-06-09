import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    java
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
    alias(libs.plugins.openapi.generator)
    alias(libs.plugins.spotless)
    jacoco
}

group = "co.edu.uniquindio"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(libs.versions.java.get().toInt())
    }
}

repositories {
    mavenCentral()
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.boot:spring-boot-dependencies:${libs.versions.springBoot.get()}")
        mavenBom("org.springframework.modulith:spring-modulith-bom:${libs.versions.springModulith.get()}")
        mavenBom("org.testcontainers:testcontainers-bom:${libs.versions.testcontainers.get()}")
    }
}

dependencies {
    // Spring Boot starters
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.spring.boot.starter.security)
    implementation(libs.spring.boot.starter.oauth2.client)
    implementation(libs.spring.boot.starter.session.jdbc)
    implementation(libs.spring.session.jdbc)

    // Spring Modulith
    implementation(libs.spring.modulith.starter.core)

    // springdoc-openapi (local profile only — always on classpath, disabled by config)
    implementation(libs.springdoc.openapi.starter.webmvc.ui)

    // Flyway
    // spring-boot-flyway provides FlywayAutoConfiguration (Boot 4 decomposed autoconfigure into modules)
    implementation(libs.spring.boot.flyway)
    implementation(libs.flyway.core)
    runtimeOnly(libs.flyway.database.postgresql)

    // Apache POI — XLSX parsing (not in Boot BOM; first explicit non-BOM dep; ADR-F6-004)
    implementation(libs.poi.ooxml)

    // Database
    runtimeOnly(libs.postgresql)

    // OpenAPI generated code dependencies
    implementation(libs.swagger.annotations)
    implementation(libs.jakarta.annotation.api)
    implementation(libs.jackson.databind.nullable)

    // Test dependencies
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.spring.security.test)
    testImplementation(libs.spring.modulith.starter.test)
    testImplementation(libs.archunit.junit5)
    testImplementation(libs.testcontainers.junit.jupiter)
    testImplementation(libs.testcontainers.postgresql)
    // Force newer docker-java to fix Docker 29.x API version incompatibility (3.4.2 hardcodes v1.32)
    testImplementation(libs.docker.java.api)
    testImplementation(libs.docker.java.transport.zerodep)
    // H2 in-memory database for tests that do not need a full PostgreSQL container (e.g. profile guard tests)
    testRuntimeOnly(libs.h2)
}

// ============================================================
// OpenAPI Generator configuration
// ============================================================
openApiGenerate {
    generatorName.set("spring")
    library.set("spring-boot")
    inputSpec.set("$projectDir/openapi/brujula-api.v1.yaml")
    outputDir.set(layout.buildDirectory.dir("generated/openapi").get().asFile.absolutePath)
    apiPackage.set("co.edu.uniquindio.brujula.generated.api")
    modelPackage.set("co.edu.uniquindio.brujula.generated.model")
    invokerPackage.set("co.edu.uniquindio.brujula.generated.invoker")
    configOptions.set(
        mapOf(
            "interfaceOnly" to "true",
            "useSpringBoot3" to "true",
            // DO NOT set useJackson3=true — bug openapi-generator#23289
            "useJakartaEe" to "true",
            "useTags" to "true",
            "documentationProvider" to "none",
            "openApiNullable" to "false",
            "performBeanValidation" to "true",
            "useBeanValidation" to "true",
            "skipDefaultInterface" to "true",
            "hideGenerationTimestamp" to "true",
        )
    )
    // Validation is performed separately by the openApiValidate task (task 2.4)
    validateSpec.set(false)
}

// ============================================================
// Task 2.4 — Contract validation gate
// A schema-invalid YAML causes a non-zero exit (fails the build).
// The openapi-generator plugin provides a built-in validate task.
// We wire it so 'check' always validates the contract first.
// ============================================================
tasks.register<org.openapitools.generator.gradle.plugin.tasks.ValidateTask>("openApiValidateContract") {
    inputSpec.set("$projectDir/openapi/brujula-api.v1.yaml")
    recommend.set(true)
}

// Wire validation into the build: check → openApiValidateContract → openApiGenerate
tasks.named("openApiGenerate") {
    dependsOn("openApiValidateContract")
}
tasks.named("check") {
    dependsOn("openApiValidateContract")
}

// Add generated sources to the main source set
sourceSets {
    main {
        java {
            srcDir(layout.buildDirectory.dir("generated/openapi/src/main/java"))
        }
    }
}

// Wire openApiGenerate before compileJava
tasks.named("compileJava") {
    dependsOn("openApiGenerate")
}

// ============================================================
// Spotless
// ============================================================
spotless {
    java {
        googleJavaFormat(libs.versions.googleJavaFormat.get())
        removeUnusedImports()
        trimTrailingWhitespace()
        endWithNewline()
        // Exclude generated sources
        targetExclude("build/generated/**")
    }
    kotlinGradle {
        trimTrailingWhitespace()
        endWithNewline()
    }
}

// Wire spotlessCheck into check
tasks.named("check") {
    dependsOn("spotlessCheck")
}

// ============================================================
// JaCoCo
// ============================================================
jacoco {
    toolVersion = libs.versions.jacoco.get()
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
    classDirectories.setFrom(
        files(
            classDirectories.files.map { fileTree(it) {
                // Include only domain and application.service for coverage
                include("**/domain/**", "**/application/service/**")
                // Exclude generated code, configuration, and adapters
                exclude("**/generated/**", "**/configuration/**", "**/adapter/**")
            }}
        )
    )
}

tasks.jacocoTestCoverageVerification {
    dependsOn(tasks.jacocoTestReport)
    // Mirror the same classDirectories filter as jacocoTestReport so verification
    // scope is **/domain/** + **/application/service/** only, excluding generated/
    // configuration/adapter code that is genuinely untestable infrastructure.
    classDirectories.setFrom(
        files(
            classDirectories.files.map { fileTree(it) {
                // Include only domain and application.service for coverage gate
                include("**/domain/**", "**/application/service/**")
                // EXCL: **/generated/** — OpenAPI-generated sources, no testable logic — WU5
                // EXCL: **/configuration/** — Spring @Configuration bean wiring, framework glue — WU5
                // EXCL: **/adapter/** — infrastructure adapters; covered by integration/TC tests not
                //        counted here; excluding prevents JPA/REST boilerplate from dragging line% — WU5
                exclude("**/generated/**", "**/configuration/**", "**/adapter/**")
            }}
        )
    )
    violationRules {
        rule {
            // Gate: 80% line coverage + 70% branch coverage on domain + application/service bundle
            // Measured before locking (WU5 T-WU5.1): LINE=89.6%, BRANCH=73.8% — both exceed threshold.
            // Element=BUNDLE (aggregate of all filtered classes) matches classDirectories scope above.
            // DEBT-F7-003: Gate is BUNDLE-level per DEC-1 (intentional). Some packages individually fall
            // below threshold — worst: programcatalog/domain branch~61%, imports/application/service
            // line~75%/branch~66%. Tracked as DEBT-F7-003. Future pass: add targeted branch tests or
            // switch to per-package enforcement via Element=PACKAGE rules in a separate violationRules block.
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = "0.80".toBigDecimal()
            }
        }
        rule {
            limit {
                counter = "BRANCH"
                value = "COVEREDRATIO"
                minimum = "0.70".toBigDecimal()
            }
        }
    }
}

// Wire jacocoTestCoverageVerification into check so `./gradlew check` enforces the gate
tasks.named("check") {
    dependsOn(tasks.jacocoTestCoverageVerification)
}

// ============================================================
// Test configuration
// ============================================================
tasks.test {
    useJUnitPlatform()
    // Testcontainers stability: run test JVM forks sequentially so that each Spring
    // ApplicationContext spin-up gets a fresh, non-contested Postgres container.
    // Without this, parallel forks reuse cached JDBC URLs pointing to dead containers
    // (HikariPool interruption on context cache miss). maxParallelForks=1 is the
    // standard Testcontainers fix for single-machine developer builds.
    maxParallelForks = 1
    // Reduce HikariCP pool size in tests to avoid "too many clients already" when multiple
    // Spring ApplicationContext instances (each with distinct @MockitoBean combinations) share
    // a single Testcontainers PostgreSQL container. Default pool=10 × N contexts can exceed
    // the Postgres max_connections limit. Pool=2 keeps peak connections well below the limit.
    systemProperty("spring.datasource.hikari.maximum-pool-size", "2")
    systemProperty("spring.datasource.hikari.minimum-idle", "1")
    // Pass Docker environment to forked JVM for Testcontainers
    // docker-java 3.x hardcodes API path /v1.32/ — must use DOCKER_HOST env var so that
    // the EnvironmentAndSystemPropertyClientProviderStrategy picks up the socket override.
    environment("DOCKER_HOST", System.getenv("DOCKER_HOST") ?: "unix:///var/run/docker.sock")
    // Disable Ryuk in CI environments where it may cause issues
    environment("TESTCONTAINERS_RYUK_DISABLED", System.getenv("TESTCONTAINERS_RYUK_DISABLED") ?: "false")
    testLogging {
        events = setOf(
            TestLogEvent.PASSED,
            TestLogEvent.SKIPPED,
            TestLogEvent.FAILED,
        )
        exceptionFormat = TestExceptionFormat.FULL
        showStackTraces = true
    }
    finalizedBy(tasks.jacocoTestReport)
}
