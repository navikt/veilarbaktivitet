import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val spring_version: String by project
val common_version: String by project
val lombok_version: String by project
val spring_kafka_version: String by project
val springfox_version: String by project
val shedlock_version: String by project
val okhttp_version: String by project
val micrometer_version: String by project
val _version: String by project


plugins {
    id("java")
    id("application")
    id("maven-publish")
    kotlin("jvm") version "1.8.0"
    id("org.openapi.generator") version "5.3.1"
    id("com.github.davidmc24.gradle.plugin.avro") version "1.3.0"
    id("project-report")
    id("jacoco")
    id("org.sonarqube") version "4.0.0.2929"
    id("org.springframework.boot") version "2.7.7"
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

kotlin {
    jvmToolchain(17)
}

configurations.all {
    resolutionStrategy.failOnNonReproducibleResolution()
}

tasks.test {
    useJUnitPlatform()
}

application {
    mainClass.set("no.nav.veilarbaktivitet.VeilarbaktivitetApp")
}

sonar {
    properties {
        property("sonar.projectKey", "navikt_veilarbaktivitet")
        property("sonar.organization", "navikt")
        property("sonar.host.url", "https://sonarcloud.io")
    }
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
    }
}

tasks.sonar {
    dependsOn(tasks.jacocoTestReport)
}

repositories {
    mavenCentral()

    maven {
        url = uri("https://packages.confluent.io/maven/")
    }

    maven {
        url = uri("https://jitpack.io")
    }

    maven {
        url = uri("https://repo.maven.apache.org/maven2/")
    }
}

tasks.generateAvroJava {
    source(
        "src/main/java/no/nav/veilarbaktivitet/brukernotifikasjon/kvittering",
        "src/main/java/no/nav/veilarbaktivitet/stilling_fra_nav"
    )
}

tasks.compileKotlin {
    dependsOn(tasks.openApiGenerate, tasks.generateAvroJava)
}
tasks.compileTestKotlin {
    dependsOn(tasks.generateTestAvroJava)
}

openApiGenerate {
    inputSpec.set("$projectDir/src/main/resources/openapi/AktivitetsplanV1.yaml")
    generatorName.set("spring")
    packageName.set("no.nav.veilarbaktivitet.internapi")
    apiPackage.set("no.nav.veilarbaktivitet.internapi.api")
    modelPackage.set("no.nav.veilarbaktivitet.internapi.model")
    configOptions.put("openApiNullable", "false")
    configOptions.put("interfaceOnly", "true")
    configOptions.put("skipDefaultInterface", "true")
    configOptions.put("additionalModelTypeAnnotations", "@lombok.experimental.SuperBuilder @lombok.NoArgsConstructor")
    outputDir.set("$buildDir/generated")
}

java.sourceSets["main"].java.srcDir("$buildDir/generated/src/main/java")

group = "no.nav"
description = "veilarbaktivitet"

publishing {
    publications.create<MavenPublication>("maven") {
        from(components["java"])
    }
}

tasks.withType<JavaCompile>() {
    options.encoding = "UTF-8"
}


if (hasProperty("buildScan")) {
    extensions.findByName("buildScan")?.withGroovyBuilder {
        setProperty("termsOfServiceUrl", "https://gradle.com/terms-of-service")
        setProperty("termsOfServiceAgree", "yes")
    }
}

dependencies {

    implementation("org.apache.kafka:kafka-clients:3.0.1") {
        version {
            strictly("3.0.1")
            because("fellesbibloteket og avro serializer drar inn ny version som ikke fungerer med spring boot  2.66")
        }
    }
    implementation("net.minidev:json-smart:2.4.8") {
        because(
            """
            Could not resolve net.minidev:json-smart:[1.3.3,2.4.8].
            Required by:
                project : > no.nav.common:sts:2.2022.10.28_07.53-25ffb4a980b5 > com.nimbusds:oauth2-oidc-sdk:9.41
        """)
    }
    compileOnly("org.projectlombok:lombok:$lombok_version")
    annotationProcessor("org.projectlombok:lombok:$lombok_version")
    testCompileOnly("org.projectlombok:lombok:$lombok_version")
    testAnnotationProcessor("org.projectlombok:lombok:$lombok_version")

    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor:$spring_version")

    implementation("org.apache.avro:avro:1.10.2")
    implementation("com.github.ben-manes.caffeine:caffeine:2.9.3")
    implementation("org.springframework.boot:spring-boot-starter-cache:$spring_version")
    implementation("org.springframework.boot:spring-boot-starter-actuator:$spring_version")
    implementation("org.springframework.boot:spring-boot-starter-web:$spring_version")
    implementation("org.springframework.boot:spring-boot-starter-validation:$spring_version")
    implementation("org.springframework.boot:spring-boot-starter-logging:$spring_version")
    implementation("org.springframework.boot:spring-boot-starter-data-jdbc:$spring_version")
    implementation("org.springframework.boot:spring-boot-starter-aop:$spring_version")
    implementation("org.springframework.boot:spring-boot-configuration-processor:$spring_version")
    implementation("org.springframework.kafka:spring-kafka:$spring_kafka_version")
    implementation("net.javacrumbs.shedlock:shedlock-provider-jdbc-template:$shedlock_version")
    implementation("net.javacrumbs.shedlock:shedlock-spring:$shedlock_version")
    implementation("com.squareup.okhttp3:okhttp:$okhttp_version")
    implementation("io.micrometer:micrometer-registry-prometheus:$micrometer_version")
    implementation("io.springfox:springfox-swagger2:$springfox_version")
    implementation("io.springfox:springfox-swagger-ui:$springfox_version")
    implementation("com.zaxxer:HikariCP:3.4.5")
    implementation("org.flywaydb:flyway-core:8.5.13")
    implementation("com.oracle.database.jdbc:ojdbc11:21.3.0.0")
    implementation("io.confluent:kafka-avro-serializer:6.1.1")
    implementation("no.nav.common:abac:$common_version")
    implementation("no.nav.common:kafka:$common_version")
    implementation("no.nav.common:sts:$common_version")
    implementation("no.nav.common:token-client:$common_version")
    implementation("no.nav.common:auth:$common_version")
    implementation("no.nav.common:log:$common_version")
    implementation("no.nav.common:health:$common_version")
    implementation("no.nav.common:cxf:$common_version")
    implementation("no.nav.common:feature-toggle:$common_version")
    implementation("no.nav.common:metrics:$common_version")
    implementation("no.nav.common:job:$common_version")
    implementation("no.nav.common:rest:$common_version")
    implementation("no.nav.common:client:$common_version")
    implementation("no.nav.common:util:$common_version")
    implementation("no.nav.common:types:$common_version")
    implementation("net.sourceforge.collections:collections-generic:4.01")
    implementation("org.quartz-scheduler:quartz:2.3.2")
    implementation("com.github.navikt:brukernotifikasjon-schemas:v2.5.1")
    implementation("com.github.navikt.dab:spring-auth:2023.01.26-15.29.bdcfc1bfb316")
    implementation("com.github.navikt.dab:spring-a2-annotations:2023.01.26-15.29.bdcfc1bfb316")
    runtimeOnly("org.springframework.boot:spring-boot-devtools:$spring_version")
    testImplementation("no.nav.common:test:$common_version")
    testImplementation("io.rest-assured:rest-assured:3.3.0")
    testImplementation("org.awaitility:awaitility:4.1.0")
    testImplementation("com.h2database:h2:1.4.200")
    testImplementation("org.junit.vintage:junit-vintage-engine:5.8.2")
    testImplementation("org.springframework.boot:spring-boot-starter-test:$spring_version")
    testImplementation("org.springframework.kafka:spring-kafka-test:$spring_kafka_version")
    testImplementation("com.github.tomakehurst:wiremock-jre8:2.29.1")
    testImplementation("org.springframework.cloud:spring-cloud-starter-contract-stub-runner:3.0.1")
    testImplementation("com.networknt:json-schema-validator:1.0.73")
    testImplementation("de.mkammerer.wiremock-junit5:wiremock-junit5:1.1.0")
    testImplementation("org.mockito:mockito-core:5.0.0")
}
