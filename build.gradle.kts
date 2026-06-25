plugins {
    kotlin("jvm") version "2.4.0"
    id("io.ktor.plugin") version "3.5.1"
    kotlin("plugin.serialization") version "2.4.0"
    id("org.jlleitschuh.gradle.ktlint") version "14.2.0"
}

group = "no.nav.emottak"
version = "0.0.1"

application {
    mainClass = "no.nav.trekkapi.AppKt"
}

tasks {
    shadowJar {
        archiveFileName.set("app.jar")
        mergeServiceFiles()
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
    }
    ktlintFormat {
        this.enabled = true
    }
    ktlintCheck {
        dependsOn("ktlintFormat")
    }
    build {
        dependsOn("ktlintCheck")
    }
    test {
        useJUnitPlatform()
        testLogging {
            events("passed", "skipped", "failed")
            showExceptions = true
            showCauses = true
            showStackTraces = true
            exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        }
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().all {
    compilerOptions {
        optIn.add("kotlin.uuid.ExperimentalUuidApi")
        optIn.add("com.sksamuel.hoplite.ExperimentalHoplite")
    }
}

kotlin {
    jvmToolchain(21)
}

repositories {
    mavenCentral()
    maven {
        name = "GitHub Packages NAV"
        url = uri("https://maven.pkg.github.com/navikt/token-support")
        credentials {
            username = "token"
            password = System.getenv("GITHUB_TOKEN")
        }
    }
}

dependencies {
    implementation(libs.logback)
    implementation(libs.logstash)
    implementation(libs.bundles.ktor)
    implementation(libs.server.swagger)
    implementation(libs.server.status.pages)
    implementation(libs.bundles.arrow)
    implementation(libs.bundles.exposed)
    implementation(libs.bundles.hoplite)
    implementation(libs.bundles.suspendapp)
    implementation(libs.micrometer.registry.prometheus)
    implementation(libs.hikari)
    implementation(libs.flyway.core)
    implementation(libs.flyway.database.postgresql)
    implementation(libs.postgresql)
    implementation(libs.kotlin.kafka)
    implementation(libs.token.validation.ktor.v3)
    implementation(libs.ibm.mq)

    testImplementation(kotlin("test"))
    testImplementation(testLibs.mockk)
    testImplementation(testLibs.bundles.kotest)
    testImplementation(testLibs.ktor.server.test)
    testImplementation(testLibs.testcontainers.postgresql)
    testImplementation(testLibs.testcontainers.kafka)
    testImplementation(testLibs.mock.oauth2.server)
}
