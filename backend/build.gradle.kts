plugins {
    java
    jacoco
    id("org.springframework.boot") version "3.5.7"
    id("io.spring.dependency-management") version "1.1.4"
    id("org.sonarqube") version "4.4.1.3373"
}

group = "com.kanban"
version = "0.0.1-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

configurations {
    developmentOnly
    runtimeClasspath {
        extendsFrom(configurations.developmentOnly.get())
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-security")

    // OAuth2 Client
    implementation("org.springframework.boot:spring-boot-starter-oauth2-client")

    implementation("io.jsonwebtoken:jjwt-api:0.11.5")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.11.5")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.11.5")

    // Database
    runtimeOnly("com.h2database:h2")
    implementation("org.flywaydb:flyway-core")

    // Developer experience
    developmentOnly("org.springframework.boot:spring-boot-devtools")

    // API Documentation (OpenAPI/Swagger)
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.3.0")

    // HTML Sanitization
    implementation("com.googlecode.owasp-java-html-sanitizer:owasp-java-html-sanitizer:20220608.1")

    // Environment Variables
    implementation("io.github.cdimascio:dotenv-java:3.0.0")

    // Lombok
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    // Test
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
    useJUnitPlatform()
    finalizedBy(tasks.jacocoTestReport)
}

tasks.withType<JavaCompile> {
    options.compilerArgs.add("-parameters")
}

val defaultSonarBackendToken = "sqp_1d338bbddaf5e05a5782da6b3f4f5118cbf129b6"

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        csv.required.set(false)
        html.required.set(true)
    }
}

sonar {
    properties {
        property("sonar.projectKey", "kanban-backend")
        property("sonar.projectName", "kanban-backend")
        property("sonar.host.url", System.getenv("SONAR_HOST_URL") ?: "http://localhost:9000")
        property("sonar.sourceEncoding", "UTF-8")
        property("sonar.coverage.jacoco.xmlReportPaths", "${layout.buildDirectory.get()}/reports/jacoco/test/jacocoTestReport.xml")
        val token = System.getenv("SONAR_BACKEND_TOKEN")?.takeIf { it.isNotBlank() } ?: defaultSonarBackendToken
        property("sonar.token", token)
    }
}

tasks.named("sonar") {
    doFirst {
        if (System.getenv("SONAR_BACKEND_TOKEN").isNullOrBlank()) {
            logger.lifecycle("SONAR_BACKEND_TOKEN 미설정: 기본 토큰을 사용합니다.")
        }
    }
}
