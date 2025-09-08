val spring_boot_version = "3.5.5"
val common_version = "3.2025.09.03_08.33-728ff4acbfdb"
val dab_common_version = "2024.11.14-10.46.174740baf5c7"
val poao_tilgang_version = "2025.07.04_08.56-814fa50f6740"
val shedlock_version = "6.10.0"
val avroVersion = "1.12.0"
val confluentKafkaAvroVersion = "8.0.0"
val _version: String by project

plugins {
    val kotlinVersion = "2.2.10"
    id("java")
    kotlin("jvm") version kotlinVersion
    id("org.jetbrains.kotlin.plugin.spring") version kotlinVersion
    kotlin("plugin.lombok") version kotlinVersion
    id("application")
    id("maven-publish")
    id("org.openapi.generator") version "7.15.0"
    id("com.github.davidmc24.gradle.plugin.avro") version "1.9.1"
    id("project-report")
    id("jacoco")
    id("org.sonarqube") version "6.3.1.5724"
    id("org.springframework.boot") version "3.5.5"
    id("io.freefair.lombok") version "8.14.2"
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

kotlin {
    jvmToolchain(21)
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

    maven("https://github-package-registry-mirror.gc.nav.no/cached/maven-release")

    maven {
        url = uri("https://jitpack.io")
    }

}

tasks.generateAvroJava {
    source(
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
    generatorName.set("kotlin-spring")
    library.set("spring-boot")
    packageName.set("no.nav.veilarbaktivitet.internapi")
    apiPackage.set("no.nav.veilarbaktivitet.internapi.api")
    modelPackage.set("no.nav.veilarbaktivitet.internapi.model")
    configOptions.put("interfaceOnly", "true")
    configOptions.put("useSpringBoot3", "true")
    configOptions.put("annotationLibrary", "none")
    configOptions.put("documentationProvider", "none")
    configOptions.put("enumPropertyNaming", "UPPERCASE")
    outputDir.set("$buildDir/generated")
}

kotlin.sourceSets["main"].kotlin.srcDir(
    project.layout.buildDirectory.dir("generated/src/main/kotlin")
)

group = "no.nav"
description = "veilarbaktivitet"

publishing {
    publications.create<MavenPublication>("maven") {
        from(components["java"])
    }
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}


if (hasProperty("buildScan")) {
    extensions.findByName("buildScan")?.withGroovyBuilder {
        setProperty("termsOfServiceUrl", "https://gradle.com/terms-of-service")
        setProperty("termsOfServiceAgree", "yes")
    }
}

dependencies {
    annotationProcessor("org.projectlombok:lombok:1.18.40")
    testAnnotationProcessor("org.projectlombok:lombok:1.18.40")

    implementation(enforcedPlatform("org.springframework.boot:spring-boot-dependencies:$spring_boot_version"))
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor:$spring_boot_version")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core") // Versjon 1.8.0 enforced by spring-boot-dependencies

    implementation("net.javacrumbs.shedlock:shedlock-provider-jdbc-template:$shedlock_version")
    implementation("net.javacrumbs.shedlock:shedlock-spring:$shedlock_version")
    implementation("no.nav.common:kafka:$common_version")
    implementation("org.apache.avro:avro:$avroVersion") // skriver avro til pto.deling-av-stilling-fra-nav-forespurt-v2
    implementation("io.confluent:kafka-avro-serializer:$confluentKafkaAvroVersion") {
        exclude(group = "io.swagger.core.v3")
    }
    implementation("no.nav.common:token-client:$common_version")
    implementation("no.nav.common:auth:$common_version")
    implementation("no.nav.common:log:$common_version")
    implementation("no.nav.common:health:$common_version")
    implementation("no.nav.common:feature-toggle:$common_version")
    implementation("no.nav.common:metrics:$common_version")
    implementation("no.nav.common:job:$common_version")
    implementation("no.nav.common:client:$common_version")
    implementation("no.nav.common:util:$common_version")
    implementation("no.nav.common:types:$common_version")
    implementation("org.apache.commons:commons-collections4:4.5.0")
    implementation("no.nav.tms.varsel:kotlin-builder:2.1.1")
    implementation("no.nav.poao.dab:spring-auth:$dab_common_version")
    implementation("no.nav.poao.dab:spring-a2-annotations:$dab_common_version")

//spring managed runtime/compile dependencies
    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-logging")
    implementation("org.springframework.boot:spring-boot-starter-data-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-aop")
    implementation("org.springframework.boot:spring-boot-configuration-processor")
    implementation("org.springframework.boot:spring-boot-starter-quartz")
    implementation("org.springframework.boot:spring-boot-starter-graphql")
    implementation("org.springframework.kafka:spring-kafka")
    implementation("com.squareup.okhttp3:okhttp")
    implementation("io.micrometer:micrometer-registry-prometheus")
    implementation("org.flywaydb:flyway-database-postgresql:11.12.0")
    implementation("org.postgresql:postgresql:42.7.7")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.20.0")

    // BigQuery
    implementation(platform("com.google.cloud:libraries-bom:26.67.0"))
    implementation("com.google.cloud:google-cloud-bigquery")

    implementation("io.getunleash:unleash-client-java:11.1.0")

    runtimeOnly("org.springframework.boot:spring-boot-devtools")

//test dependencies
    testImplementation("no.nav.poao-tilgang:poao-tilgang-test-wiremock:$poao_tilgang_version")
    testImplementation("org.awaitility:awaitility:4.3.0")
    testImplementation("com.networknt:json-schema-validator:1.5.8")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.4.0")

//spring managed test dependencies
    testImplementation("io.rest-assured:rest-assured")
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "com.vaadin.external.google", module = "android-json")
    }
    testImplementation("org.springframework.kafka:spring-kafka-test")
    testImplementation("org.mockito:mockito-core")
    testImplementation("io.zonky.test:embedded-database-spring-test:2.6.0")
    testImplementation("io.zonky.test:embedded-postgres:2.1.1")
}
