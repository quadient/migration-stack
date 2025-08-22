import kotlin.io.useLines

data class ScriptMetadata(
    val filename: String,
    val displayName: String?,
    val category: String,
    val sourceFormat: String?,
    val description: String?,
    val pkg: String,
    val needsStdin: Boolean,
    val target: Target,
) {
    companion object {
        fun fromString(input: List<String>, filename: String, packageName: String): ScriptMetadata {
            val map = mutableMapOf<String, String>()
            for (line in input) {
                if (!line.contains(":")) {
                    throw IllegalArgumentException("Invalid frontmatter line: $line")
                }
                val split = line.split(":", limit = 2)
                map.put(split[0].trim(), split[1].trim())
            }

            return ScriptMetadata(
                filename = filename,
                displayName = map["displayName"],
                category = map["category"] ?: throw IllegalArgumentException("Missing 'category' in frontmatter"),
                sourceFormat = map["sourceFormat"],
                description = map["description"],
                needsStdin = map["stdin"]?.toBoolean() ?: false,
                target = map["target"]?.let { target -> Target.fromString(target.trim()) } ?: Target.All,
                pkg = packageName,
            )
        }
    }

    enum class Target {
        All, Gradle, App;

        companion object {
            fun fromString(input: String): Target {
                return when (input.trim().lowercase()) {
                    "all" -> All
                    "gradle" -> Gradle
                    "app" -> App
                    else -> throw IllegalArgumentException("Unknown target: $input")
                }
            }
        }
    }
}

val scripts: List<ScriptMetadata> =
    File("$rootDir/src/main/groovy").walk().asSequence().filter { it.isFile && it.extension == "groovy" }
        .onEach { println("Parsing groovy file: ${it.name}") }.map {
            Pair(it.name, it.useLines { lines ->
                lines.takeWhile { line ->
                    line.startsWith("//") || line.startsWith("package ") || line.isBlank()
                }.map { line -> line.removePrefix("//!").trim() }.filter { line -> !line.startsWith("//") }.toList()
            })
        }.mapNotNull { (filename, input) ->
            val lines = input.filter { !it.isBlank() }

            if (lines.count() <= 3 || (lines[0].startsWith("package") && lines[1] != "---")) {
                println("Skipping script '$filename' without frontmatter")
                return@mapNotNull null
            }

            if (lines[lines.count() - 1].startsWith("package") && lines[lines.count() - 2] != "---") {
                println("Skipping script '$filename' without frontmatter")
                return@mapNotNull null
            }

            println("Parsing script frontmatter: $lines")
            val (pkg, rest) = when {
                lines.firstOrNull()?.startsWith("package ") ?: false -> {
                    lines.first().removePrefix("package ") to lines.drop(1)
                }

                lines.lastOrNull()?.startsWith("package ") ?: false -> {
                    lines.last().removePrefix("package ") to lines.dropLast(1)
                }

                else -> {
                    throw IllegalArgumentException("Invalid frontmatter, missing package declaration: ${lines.firstOrNull()}")
                }
            }

            if (rest.firstOrNull() != "---" && rest.lastOrNull() != "---") {
                throw IllegalArgumentException("Invalid frontmatter fences: ${rest.joinToString("\n")}")
            }

            ScriptMetadata.fromString(rest.drop(1).dropLast(1), filename, pkg)
        }.filter { it.target == ScriptMetadata.Target.All || it.target == ScriptMetadata.Target.Gradle }.toList()

tasks {
    for (script in scripts) {
        val groupValue = "Migration ${if (script.category.lowercase() == "parser") {
            val sourceFormat = script.sourceFormat ?: "Other"
            "${script.category} - $sourceFormat"
        } else script.category}"

        register<JavaExec>(script.filename.removeSuffix(".groovy")) {
            description = script.description
            group = groupValue
            mainClass = "${script.pkg}.${script.filename.removeSuffix(".groovy")}"
            classpath = project.extensions.getByType<JavaPluginExtension>().sourceSets["main"].runtimeClasspath
            args = project.findProperty("scriptArgs")?.toString()?.split(" ") ?: emptyList()
            if (script.needsStdin) {
                standardInput = System.`in`
            }
        }
    }
}
