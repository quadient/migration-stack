import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.*

plugins {
    id("build.repositories")
    java
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
    withSourcesJar()
    withJavadocJar()
}

tasks {
    jar {
        manifest {
            attributes(
                mapOf(
                    "Implementation-Title" to project.name,
                    "VCS-Revision" to getGitRevision(),
                    "Build-Date" to getCurrentTime()
                )
            )
        }
    }
    javadoc {
        options {
            (this as CoreJavadocOptions).addBooleanOption("Xdoclint:none", true)
        }
    }
}

fun getGitRevision(): String {
    val stdout = ByteArrayOutputStream()
    exec {
        commandLine("git", "rev-parse", "HEAD")
        standardOutput = stdout
    }
    return stdout.toString().trim()
}

fun getCurrentTime(): String =
    SimpleDateFormat("dd.MM.yyyy HH:mm:ss z", Locale.US).format(Calendar.getInstance().time)
