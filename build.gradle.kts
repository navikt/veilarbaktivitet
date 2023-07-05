
val spring_boot_version = "3.0.2"
val common_version = "3.2023.06.27_13.10-a13ff6f90290"
val dab_common_version = "2023.06.30-12.11.f11e6e6e92ef"
val poao_tilgang_version = "2023.07.03_09.35-6073bc164ee2"
val shedlock_version = "4.42.0"
val _version: String by project


plugins {
    id("java")
    id("application")
    id("maven-publish")
    kotlin("jvm") version "1.8.0"
    id("org.openapi.generator") version "6.4.0"
    id("com.github.davidmc24.gradle.plugin.avro") version "1.3.0"
    id("project-report")
    id("jacoco")
    id("org.sonarqube") version "4.0.0.2929"
    id("org.springframework.boot") version "3.0.2"
    id("io.freefair.lombok") version "6.6.2"
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
    configOptions.put("useSpringBoot3", "true")
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
    implementation(enforcedPlatform("org.springframework.boot:spring-boot-dependencies:$spring_boot_version"))
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor:$spring_boot_version")

    implementation("net.javacrumbs.shedlock:shedlock-provider-jdbc-template:$shedlock_version")
    implementation("net.javacrumbs.shedlock:shedlock-spring:$shedlock_version")
    implementation("com.github.navikt.common-java-modules:abac:$common_version")
    implementation("com.github.navikt.common-java-modules:kafka:$common_version")
    implementation("com.github.navikt.common-java-modules:sts:$common_version")
    implementation("com.github.navikt.common-java-modules:token-client:$common_version")
    implementation("com.github.navikt.common-java-modules:auth:$common_version")
    implementation("com.github.navikt.common-java-modules:log:$common_version")
    implementation("com.github.navikt.common-java-modules:health:$common_version")
    implementation("com.github.navikt.common-java-modules:feature-toggle:$common_version")
    implementation("com.github.navikt.common-java-modules:metrics:$common_version")
    implementation("com.github.navikt.common-java-modules:job:$common_version")
    implementation("com.github.navikt.common-java-modules:rest:$common_version")
    implementation("com.github.navikt.common-java-modules:client:$common_version")
    implementation("com.github.navikt.common-java-modules:util:$common_version")
    implementation("com.github.navikt.common-java-modules:types:$common_version")
    implementation("net.sourceforge.collections:collections-generic:4.01")
    implementation("com.github.navikt:brukernotifikasjon-schemas:v2.5.2")
    implementation("com.github.navikt.dab:spring-auth:$dab_common_version")
    implementation("com.github.navikt.dab:spring-a2-annotations:$dab_common_version")

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
    implementation("org.springframework.kafka:spring-kafka")
    implementation("com.squareup.okhttp3:okhttp")
    implementation("io.micrometer:micrometer-registry-prometheus")
    implementation("org.flywaydb:flyway-core")
    implementation("com.oracle.database.jdbc:ojdbc11")

    // Hvis det ønskes swagger doc, foreslås å bruke springdoc (springdoc-openapi-starter-webmvc-ui - se no.nav.fo.veilarbdialog.rest.SwaggerConfig for eksempelconfig)
    implementation("io.swagger.core.v3:swagger-annotations:2.2.8")

    runtimeOnly("org.springframework.boot:spring-boot-devtools")

    //test dependencys
    testImplementation("com.github.navikt.poao-tilgang:poao-tilgang-test-wiremock:$poao_tilgang_version")

    testImplementation("org.awaitility:awaitility:4.1.0")
    testImplementation("com.github.tomakehurst:wiremock:3.0.0-beta-2")
    testImplementation("org.springframework.cloud:spring-cloud-starter-contract-stub-runner:4.0.1")
    testImplementation("com.networknt:json-schema-validator:1.0.73")
    testImplementation("de.mkammerer.wiremock-junit5:wiremock-junit5:1.1.0")

    //spring managed test dependencies
    testImplementation("io.rest-assured:rest-assured")
    testImplementation("com.h2database:h2")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.kafka:spring-kafka-test")
    testImplementation("org.mockito:mockito-core")
}
