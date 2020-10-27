import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jlleitschuh.gradle.ktlint.reporter.ReporterType
import java.net.URL

plugins {
    kotlin("jvm")
    id("org.jetbrains.dokka") version Versions.dokka
    id("org.jlleitschuh.gradle.ktlint") version Versions.`ktlint-plugin`
    signing
}

val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions.jvmTarget = "1.8"
compileKotlin.kotlinOptions.freeCompilerArgs += listOf("-Xjvm-default=enable", "-Xjsr305=strict")

val compileTestKotlin: KotlinCompile by tasks
compileTestKotlin.kotlinOptions.jvmTarget = "1.8"
compileTestKotlin.kotlinOptions.freeCompilerArgs += listOf("-Xjvm-default=enable", "-Xjsr305=strict")

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))
    api("io.github.microutils", "kotlin-logging", Versions.`kotlin-logging`)
    api("io.github.config4k", "config4k", Versions.config4k)
    api("io.dropwizard.metrics", "metrics-core", Versions.metrics)
    compileOnly("com.lightbend.lagom", "lagom-javadsl-server_$scalaBinaryVersion", lagomVersion)
    compileOnly("com.lightbend.lagom", "lagom-javadsl-persistence-cassandra_$scalaBinaryVersion", lagomVersion)
    compileOnly("com.typesafe.play", "play-jdbc-api_$scalaBinaryVersion", playVersion)
    compileOnly("io.dropwizard.metrics", "metrics-jvm", Versions.metrics)
    compileOnly("io.dropwizard.metrics", "metrics-graphite", Versions.metrics)
    compileOnly("com.zaxxer", "HikariCP", Versions.hikari)

    testImplementation("io.kotlintest", "kotlintest-runner-junit5", Versions.kotlintest)
    testImplementation("com.nhaarman.mockitokotlin2", "mockito-kotlin", Versions.`mockito-kotlin`)
    testImplementation("com.lightbend.lagom", "lagom-javadsl-testkit_$scalaBinaryVersion", lagomVersion)
    testImplementation("com.typesafe.akka", "akka-stream-testkit_$scalaBinaryVersion", Versions.akka)
    testImplementation("com.typesafe.play", "play-akka-http-server_$scalaBinaryVersion", playVersion)
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
}

val sourcesJar by tasks.creating(Jar::class) {
    archiveClassifier.set("sources")
    from(sourceSets.main.get().allSource)
}

val dokkaJar by tasks.creating(Jar::class) {
    group = JavaBasePlugin.DOCUMENTATION_GROUP
    archiveClassifier.set("javadoc")
    from(tasks.dokka)
}

tasks.dokka {
    outputFormat = "javadoc"
    outputDirectory = "$buildDir/javadoc"
    configuration {
        jdkVersion = 8
        reportUndocumented = false
        externalDocumentationLink {
            url = URL("https://www.lagomframework.com/documentation/1.6.x/java/api/")
        }
    }
    impliedPlatforms = mutableListOf("JVM")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            artifactId = "${project.name}_$scalaBinaryVersion"
            from(components["java"])
            artifact(sourcesJar)
            artifact(dokkaJar)
            pom(Publishing.pom)
        }
    }
}

signing {
    isRequired = isRelease
    sign(publishing.publications["maven"])
}
