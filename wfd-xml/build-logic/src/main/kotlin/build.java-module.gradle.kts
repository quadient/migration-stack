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

fun getCurrentTime(): String =
    SimpleDateFormat("dd.MM.yyyy HH:mm:ss z", Locale.US).format(Calendar.getInstance().time)
