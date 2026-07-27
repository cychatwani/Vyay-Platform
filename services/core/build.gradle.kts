plugins {
    java
    checkstyle
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
}

// From pom.xml <groupId>/<version>. The Spring Boot plugin's bootJar replaces
// the spring-boot-maven-plugin and produces the executable jar.
group = "com.vyay"
version = "0.0.1"

// Java toolchain (21) is applied centrally by the root build's `subprojects`
// block to every project with a Java plugin, so it is intentionally NOT
// re-declared here (pom.xml pinned 17; step 1 moved the platform to 21).

dependencies {
    // --- Web + Security (versions from the Spring Boot BOM) ---
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-security")

    // --- Persistence (versions from the Spring Boot BOM) ---
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    // pom: <scope>runtime</scope>
    runtimeOnly(libs.postgresql)

    // --- Flyway (versions from the Spring Boot BOM) ---
    implementation(libs.flyway.core)
    implementation(libs.flyway.database.postgresql)

    // --- Validation (version from the Spring Boot BOM) ---
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // --- OpenAPI / Swagger ---
    implementation(libs.springdoc.openapi.starter.webmvc.api)

    // --- JWT (jjwt-impl / jjwt-jackson are runtime-only in the pom) ---
    implementation(libs.jjwt.api)
    runtimeOnly(libs.jjwt.impl)
    runtimeOnly(libs.jjwt.jackson)

    // --- Google API client ---
    implementation(libs.google.api.client)
    implementation(libs.google.http.client.gson)

    // --- ID generators ---
    implementation(libs.uuid.creator)

    // --- Lombok (pom: <scope>provided</scope>) ---
    // compileOnly + annotationProcessor is the idiomatic Gradle equivalent of
    // Maven "provided" for an annotation processor. Maven's provided scope is
    // also visible on the test classpath, so the test wiring is mirrored too.
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
    testCompileOnly(libs.lombok)
    testAnnotationProcessor(libs.lombok)

    // --- Dev tools (pom: <optional>true</optional>) ---
    // The Spring Boot plugin's `developmentOnly` configuration is the direct
    // equivalent: on the runtime classpath for `bootRun`, excluded from the jar.
    developmentOnly("org.springframework.boot:spring-boot-devtools")

    // --- Testing (version from the Spring Boot BOM) ---
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

// Mirrors the maven-checkstyle-plugin: Google Java Style, extracted from the
// Checkstyle tool jar (equivalent of Maven's built-in `google_checks.xml`).
// google_checks.xml reports at "warning" severity, so — like the Maven build
// (violationSeverity=error) — style findings are reported but do not fail the
// build. consoleOutput=true maps to Gradle's default showViolations behavior.
checkstyle {
    toolVersion = "10.21.0"
    // Filter to the checkstyle jar itself: the `checkstyle` configuration also
    // resolves transitive deps, and fromArchiveEntry needs exactly one archive.
    config = resources.text.fromArchiveEntry(
        configurations.checkstyle.get().filter { it.name.startsWith("checkstyle-") },
        "google_checks.xml"
    )
}

