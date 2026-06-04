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
    }
}

dependencies {
    // Spring Boot starters
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.spring.boot.starter.security)

    // Spring Modulith
    implementation(libs.spring.modulith.starter.core)

    // springdoc-openapi (local profile only — always on classpath, disabled by config)
    implementation(libs.springdoc.openapi.starter.webmvc.ui)

    // Flyway
    implementation(libs.flyway.core)
    runtimeOnly(libs.flyway.database.postgresql)

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
    // Suppress validation failures on generation — we handle validation separately
    validateSpec.set(false)
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
    violationRules {
        rule {
            // Only enforce on domain + application.service (Fase 0 has no implementations yet)
            // Threshold set to 0 for Fase 0 — real threshold applied in Fase 1
            limit {
                minimum = "0.0".toBigDecimal()
            }
        }
    }
}

// ============================================================
// Test configuration
// ============================================================
tasks.test {
    useJUnitPlatform()
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
