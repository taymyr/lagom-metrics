import org.gradle.api.publish.maven.MavenPom

@Suppress("UnstableApiUsage")
object Publishing {
    val pom: (MavenPom).() -> Unit = {
        name.set("Lagom Metrics")
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
