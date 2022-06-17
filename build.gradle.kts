/*
 * This file was generated by the Gradle 'init' task.
 */

plugins {
    java
    `maven-publish`
    id("org.openapi.generator") version "5.3.1"
    id("com.github.davidmc24.gradle.plugin.avro") version "1.3.0"
    id("project-report")
    id ("jacoco")
    id("org.sonarqube") version "3.4.0.2513"
}

dependencyLocking {
    lockAllConfigurations()
}

sonarqube {
    properties {
        property("sonar.projectKey", "navikt_veilarbaktivitet")
        property( "sonar.organization", "navikt")
        property( "sonar.host.url", "https://sonarcloud.io")
    }
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
    }
}

tasks.sonarqube {
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
    source("src/main/java/no/nav/veilarbaktivitet/brukernotifikasjon/kvitering/", "src/main/java/no/nav/veilarbaktivitet/stilling_fra_nav")
}

tasks.compileJava {
    dependsOn(tasks.openApiGenerate, tasks.generateAvroJava)
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


dependencies {

    implementation("org.apache.kafka:kafka-clients:3.0.1")  {
        version {
            strictly("3.0.1")
            because("fellesbibloteket og avro serializer drar inn ny version som ikke fungerer med spring boot  2.66")
        }
    }
    compileOnly("org.projectlombok:lombok:1.18.24")
    annotationProcessor("org.projectlombok:lombok:1.18.24")
    testCompileOnly("org.projectlombok:lombok:1.18.24")
    testAnnotationProcessor("org.projectlombok:lombok:1.18.24")

    implementation("org.apache.avro:avro:1.10.2")
    implementation("com.github.ben-manes.caffeine:caffeine:2.8.1")
    implementation("org.springframework.boot:spring-boot-starter-cache:2.6.6")
    implementation("org.springframework.boot:spring-boot-starter-actuator:2.6.6")
    implementation("org.springframework.boot:spring-boot-starter-web:2.6.6")
    implementation("org.springframework.boot:spring-boot-starter-validation:2.6.6")
    implementation("org.springframework.boot:spring-boot-starter-logging:2.6.6")
    implementation("org.springframework.boot:spring-boot-starter-data-jdbc:2.6.6")
    implementation("org.springframework.boot:spring-boot-starter-aop:2.6.6")
    implementation("net.javacrumbs.shedlock:shedlock-provider-jdbc-template:4.21.0")
    implementation("org.springframework.kafka:spring-kafka:2.8.4")
    implementation("com.squareup.okhttp3:okhttp:4.9.3")
    implementation("io.micrometer:micrometer-registry-prometheus:1.8.4")
    implementation("io.springfox:springfox-swagger2:2.9.2")
    implementation("io.springfox:springfox-swagger-ui:2.9.2")
    implementation("com.zaxxer:HikariCP:3.2.0")
    implementation("org.flywaydb:flyway-core:8.4.2")
    implementation("com.oracle.database.jdbc:ojdbc11:21.3.0.0")
    implementation("no.nav.common:abac:2.2022.05.05_06.41-84855089824b")
    implementation("no.nav.common:kafka:2.2022.05.05_06.41-84855089824b")
    implementation("io.confluent:kafka-avro-serializer:6.1.1")
    implementation("no.nav.common:sts:2.2022.05.05_06.41-84855089824b")
    implementation("no.nav.common:token-client:2.2022.05.05_06.41-84855089824b")
    implementation("no.nav.common:auth:2.2022.05.05_06.41-84855089824b")
    implementation("no.nav.common:log:2.2022.05.05_06.41-84855089824b")
    implementation("no.nav.common:health:2.2022.05.05_06.41-84855089824b")
    implementation("no.nav.common:cxf:2.2022.05.05_06.41-84855089824b")
    implementation("no.nav.common:feature-toggle:2.2022.05.05_06.41-84855089824b")
    implementation("no.nav.common:metrics:2.2022.05.05_06.41-84855089824b")
    implementation("no.nav.common:job:2.2022.05.05_06.41-84855089824b")
    implementation("no.nav.common:rest:2.2022.05.05_06.41-84855089824b")
    implementation("no.nav.common:client:2.2022.05.05_06.41-84855089824b")
    implementation("no.nav.common:util:2.2022.05.05_06.41-84855089824b")
    implementation("no.nav.common:types:2.2022.05.05_06.41-84855089824b")
    implementation("net.sourceforge.collections:collections-generic:4.01")
    implementation("org.quartz-scheduler:quartz:2.3.2")
    implementation("net.javacrumbs.shedlock:shedlock-spring:4.21.0")
    implementation("org.springframework.boot:spring-boot-configuration-processor:2.6.6")
    implementation("com.github.navikt:brukernotifikasjon-schemas:v2.5.1")
    runtimeOnly("org.springframework.boot:spring-boot-devtools:2.6.6")
    testImplementation("no.nav.common:test:2.2022.05.05_06.41-84855089824b")
    testImplementation("io.rest-assured:rest-assured:3.3.0")
    testImplementation("org.awaitility:awaitility:4.1.0")
    testImplementation("com.h2database:h2:1.4.200")
    testImplementation("org.junit.vintage:junit-vintage-engine:5.7.1")
    testImplementation("org.springframework.boot:spring-boot-starter-test:2.6.6")
    testImplementation("org.springframework.kafka:spring-kafka-test:2.8.4")
    testImplementation("com.github.tomakehurst:wiremock-jre8:2.33.2")
    testImplementation("org.springframework.cloud:spring-cloud-starter-contract-stub-runner:3.0.1")
}

group = "no.nav"
version = "1"
description = "veilarbaktivitet"
java.sourceCompatibility = JavaVersion.VERSION_17

publishing {
    publications.create<MavenPublication>("maven") {
        from(components["java"])
    }
}

tasks.withType<JavaCompile>() {
    options.encoding = "UTF-8"
}
