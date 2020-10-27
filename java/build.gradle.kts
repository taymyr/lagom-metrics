import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jlleitschuh.gradle.ktlint.reporter.ReporterType
import java.net.URL

val isReleaseVersion = !version.toString().endsWith("SNAPSHOT")

val ossrhUsername: String? by project
val ossrhPassword: String? by project

object Versions {
    const val scalaBinary = "2.13" // "2.12" // "2.11"
    const val lagom = "1.6.4" // "1.5.5" // "1.4.15"
    const val play = "2.8.3" // "2.7.4" // "2.6.10"
    const val ktlint = "0.30.0"
    const val `kotlin-logging` = "1.6.25"
    const val config4k = "0.4.1"
    const val metrics = "3.2.6"
    const val hikari = "2.7.9"
    const val kotlintest = "3.1.10"
    const val `mockito-kotlin` = "2.1.0"
    const val akka = "2.5.17"
    const val jacoco = "0.8.2"
}

val lagomVersion = project.properties["lagomVersion"] as String? ?: Versions.lagom
val playVersion = project.properties["playVersion"] as String? ?: Versions.play
val scalaBinaryVersion = project.properties["scalaBinaryVersion"] as String? ?: Versions.scalaBinary

plugins {
    kotlin("jvm") version "1.3.21"
    id("org.jetbrains.dokka") version "0.9.17"
    id("org.jlleitschuh.gradle.ktlint") version "8.0.0"
    id("de.marcphilipp.nexus-publish") version "0.2.0"
    signing
    jacoco
}

val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions.jvmTarget = "1.8"
compileKotlin.kotlinOptions.freeCompilerArgs += listOf("-Xjvm-default=enable", "-Xjsr305=strict")

val compileTestKotlin: KotlinCompile by tasks
compileTestKotlin.kotlinOptions.jvmTarget = "1.8"
compileTestKotlin.kotlinOptions.freeCompilerArgs += listOf("-Xjvm-default=enable", "-Xjsr305=strict")

dependencies {
    compile(kotlin("stdlib-jdk8"))
    compile(kotlin("reflect"))
    compile("io.github.microutils", "kotlin-logging", Versions.`kotlin-logging`)
    compile("io.github.config4k", "config4k", Versions.config4k)
    compile("io.dropwizard.metrics", "metrics-core", Versions.metrics)
    compileOnly("com.lightbend.lagom", "lagom-javadsl-server_$scalaBinaryVersion", lagomVersion)
    compileOnly("com.lightbend.lagom", "lagom-javadsl-persistence-cassandra_$scalaBinaryVersion", lagomVersion)
    compileOnly("com.typesafe.play", "play-jdbc-api_$scalaBinaryVersion", playVersion)
    compileOnly("io.dropwizard.metrics", "metrics-jvm", Versions.metrics)
    compileOnly("io.dropwizard.metrics", "metrics-graphite", Versions.metrics)
    compileOnly("com.zaxxer", "HikariCP", Versions.hikari)

    testImplementation("io.kotlintest", "kotlintest-runner-junit5", Versions.kotlintest)
    testCompile("com.nhaarman.mockitokotlin2", "mockito-kotlin", Versions.`mockito-kotlin`)
    testCompile("com.lightbend.lagom", "lagom-javadsl-testkit_$scalaBinaryVersion", lagomVersion)
    testCompile("com.typesafe.akka", "akka-stream-testkit_$scalaBinaryVersion", Versions.akka)
    testCompile("com.typesafe.play", "play-akka-http-server_$scalaBinaryVersion", playVersion)
}

configurations {
    testCompile.get().extendsFrom(compileOnly.get())
}

ktlint {
    version.set(Versions.ktlint)
    outputToConsole.set(true)
    reporters.set(setOf(ReporterType.CHECKSTYLE))
}

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
    finalizedBy("jacocoTestReport")
}

jacoco {
    toolVersion = Versions.jacoco
}
tasks.jacocoTestReport {
    reports {
        xml.isEnabled = true
    }
}

val sourcesJar by tasks.creating(Jar::class) {
    classifier = "sources"
    from(sourceSets.main.get().allSource)
}

val dokkaJar by tasks.creating(Jar::class) {
    group = JavaBasePlugin.DOCUMENTATION_GROUP
    classifier = "javadoc"
    from(tasks.dokka)
}

tasks.dokka {
    outputFormat = "javadoc"
    outputDirectory = "$buildDir/javadoc"
    jdkVersion = 8
    reportUndocumented = true
    impliedPlatforms = mutableListOf("JVM")
    externalDocumentationLink(delegateClosureOf<DokkaConfiguration.ExternalDocumentationLink.Builder> {
        url = URL("https://www.lagomframework.com/documentation/1.4.x/java/api/")
    })
    externalDocumentationLink(delegateClosureOf<DokkaConfiguration.ExternalDocumentationLink.Builder> {
        url = URL("https://metrics.dropwizard.io/4.0.0/apidocs/")
    })
    externalDocumentationLink(delegateClosureOf<DokkaConfiguration.ExternalDocumentationLink.Builder> {
        url = URL("https://www.playframework.com/documentation/2.6.x/api/java/")
    })
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            artifactId = "${project.name}_$scalaBinaryVersion"
            from(components["java"])
            artifact(sourcesJar)
            artifact(dokkaJar)
            pom {
                name.set("Taymyr: Lagom Metrics")
                description.set("Metrics for Lagom framework")
                url.set("https://taymyr.org")
                organization {
                    name.set("Digital Economy League")
                    url.set("https://www.digitalleague.ru/")
                }
                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                developers {
                    developer {
                        id.set("taymyr")
                        name.set("Taymyr Contributors")
                        email.set("contributors@taymyr.org")
                    }
                }
                scm {
                    connection.set("scm:git:https://github.com/taymyr/lagom-metrics.git")
                    developerConnection.set("scm:git:https://github.com/taymyr/lagom-metrics.git")
                    url.set("https://github.com/taymyr/lagom-metrics")
                    tag.set("HEAD")
                }
            }
        }
    }
}

signing {
    isRequired = isReleaseVersion
    sign(publishing.publications["maven"])
}