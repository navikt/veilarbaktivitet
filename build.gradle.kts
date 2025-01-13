
val spring_boot_version = "3.4.1"
val common_version = "3.2024.11.26_16.35-432a29107830"
val dab_common_version = "2024.11.14-10.46.174740baf5c7"
val poao_tilgang_version = "2024.11.26_08.36-ad014162ce23"
val shedlock_version = "6.2.0"
val _version: String by project

plugins {
    id("java")
    id("org.jetbrains.kotlin.plugin.spring") version "2.1.0"
    id("application")
    id("maven-publish")
    kotlin("jvm") version "2.1.0"
    id("org.openapi.generator") version "7.10.0"
    id("com.github.davidmc24.gradle.plugin.avro") version "1.9.1"
    id("project-report")
    id("jacoco")
    id("org.sonarqube") version "6.0.1.5171"
    id("org.springframework.boot") version "3.4.1"
    kotlin("plugin.lombok") version "2.1.0"
    id("io.freefair.lombok") version "8.11"
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

kotlin.sourceSets["main"].kotlin.srcDir("$buildDir/generated/src/main/kotlin")

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
    annotationProcessor("org.projectlombok:lombok:1.18.36")
    testAnnotationProcessor("org.projectlombok:lombok:1.18.36")

    implementation(enforcedPlatform("org.springframework.boot:spring-boot-dependencies:$spring_boot_version"))
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor:$spring_boot_version")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1")

    implementation("net.javacrumbs.shedlock:shedlock-provider-jdbc-template:$shedlock_version")
    implementation("net.javacrumbs.shedlock:shedlock-spring:$shedlock_version")
    implementation("no.nav.common:abac:$common_version")
    implementation("no.nav.common:kafka:$common_version")
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
    implementation("org.apache.commons:commons-collections4:4.4")
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
    implementation("io.micrometer:micrometer-registry-prometheus-simpleclient")
    implementation("org.flywaydb:flyway-database-postgresql:11.1.1")
    implementation("org.postgresql:postgresql:42.7.4")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.18.2")

    // BigQuery
    implementation(platform("com.google.cloud:libraries-bom:26.52.0"))
    implementation("com.google.cloud:google-cloud-bigquery")

    implementation("io.getunleash:unleash-client-java:9.2.6")

    runtimeOnly("org.springframework.boot:spring-boot-devtools")

//test dependencies
    testImplementation("no.nav.poao-tilgang:poao-tilgang-test-wiremock:$poao_tilgang_version")

    testImplementation("org.awaitility:awaitility:4.2.2")
//    testImplementation("com.github.tomakehurst:wiremock:3.0.0-beta-2")
//    testImplementation("org.springframework.cloud:spring-cloud-starter-contract-stub-runner:4.0.1")
    
//    testImplementation("org.springframework.cloud:spring-cloud-contract-wiremock:4.1.3")

    testImplementation("com.networknt:json-schema-validator:1.5.4")

//    testImplementation("de.mkammerer.wiremock-junit5:wiremock-junit5:1.1.0")
//    testImplementation("io.github.ricall.junit5-wiremock:junit5-wiremock:2.0.0")

    testImplementation("org.mockito.kotlin:mockito-kotlin:5.4.0")

//spring managed test dependencies
    testImplementation("io.rest-assured:rest-assured")
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "com.vaadin.external.google", module = "android-json")
    }
    testImplementation("org.springframework.kafka:spring-kafka-test")
    testImplementation("org.mockito:mockito-core")
    testImplementation("io.zonky.test:embedded-database-spring-test:2.6.0")
    testImplementation("io.zonky.test:embedded-postgres:2.1.0")
}
