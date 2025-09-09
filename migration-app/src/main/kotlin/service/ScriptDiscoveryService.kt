package com.quadient.migration.service

import com.quadient.migration.getScriptDir
import com.quadient.migration.log
import io.ktor.server.config.ApplicationConfig
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.serialization.Serializable
import java.io.File
import kotlin.io.encoding.Base64

class ScriptDiscoveryService(val cfg: ApplicationConfig) {
    val semaphore = Semaphore(1)

    private var _scripts: List<ScriptMetadata>? = null

    suspend fun getScripts(): List<ScriptMetadata> {
        return _scripts ?: loadScripts()
    }

    suspend fun loadScripts(): List<ScriptMetadata> {
        if (!semaphore.tryAcquire()) {
            return semaphore.withPermit {
                _scripts ?: emptyList()
            }
        }

        val scripts = File(cfg.getScriptDir()).walk().filter { it.isFile && it.extension == "groovy" }
                .onEach { log.trace("Parsing groovy file: ${it.name}") }.map {
                    LoadedScriptFile(it.name, it.absolutePath, it.useLines { lines ->
                        lines.takeWhile { line ->
                                line.startsWith("//") || line.startsWith("package ") || line.isBlank()
                            }.map { line -> line.removePrefix("//!").trim() }.filter { line -> !line.startsWith("//") }
                            .toList()
                    })
                }.mapNotNull { (filename, absolutePath, input) ->
                    val lines = input.filter { !it.isBlank() }

                    if (lines.count() <= 3 || (lines[0].startsWith("package") && lines[1] != "---")) {
                        log.trace("Skipping script '$filename' without frontmatter")
                        return@mapNotNull null
                    }

                    if (lines[lines.count() - 1].startsWith("package") && lines[lines.count() - 2] != "---") {
                        log.trace("Skipping script '$filename' without frontmatter")
                        return@mapNotNull null
                    }

                    log.trace("Parsing script module frontmatter: {}", lines)
                    val (pkg, rest) = when {
                        lines.firstOrNull()?.startsWith("package ") ?: false -> {
                            lines.first().removePrefix("package ") to lines.drop(1)
                        }

                        lines.lastOrNull()?.startsWith("package ") ?: false -> {
                            lines.last().removePrefix("package ") to lines.dropLast(1)
                        }

                        else -> {
                            log.error("Invalid frontmatter, missing package declaration: ${lines.firstOrNull()}")
                            return@mapNotNull null
                        }
                    }

                    if (rest.firstOrNull() != "---" && rest.lastOrNull() != "---") {
                        log.error("Invalid frontmatter fences: ${rest.joinToString("\n")}")
                        return@mapNotNull null
                    }

                    log.debug("Successfully parsed script module $filename")
                    ScriptMetadata.fromString(rest.drop(1).dropLast(1), filename, absolutePath, pkg)
                }.filter { it.target == ScriptMetadata.Target.All || it.target == ScriptMetadata.Target.App }
                .toList()

        _scripts = scripts
        semaphore.release()
        return scripts
    }

    data class LoadedScriptFile(val filename: String, val absolutePath: String, val content: List<String>)
}

@Serializable
@JvmInline
value class ScriptId(val value: String) {
    override fun toString(): String = value
}

@Serializable
data class ScriptMetadata(
    val id: ScriptId,
    val filename: String,
    val path: String,
    val displayName: String?,
    val category: String,
    val sourceFormat: String?,
    val description: String?,
    val pkg: String,
    val needsStdin: Boolean,
    val target: Target,
    val order: Int?
) {
    companion object {
        fun fromString(input: List<String>, filename: String, path: String, packageName: String): ScriptMetadata {
            val map = mutableMapOf<String, String>()
            for (line in input) {
                if (!line.contains(":")) {
                    throw IllegalArgumentException("Invalid frontmatter line: $line")
                }
                val split = line.split(":", limit = 2)
                map[split[0].trim()] = split[1].trim()
            }

            val id = Base64.encode((map["category"] + map["sourceFormat"] + filename).toByteArray())

            return ScriptMetadata(
                id = ScriptId(id),
                filename = filename,
                path = path,
                displayName = map["displayName"],
                order = map["order"]?.toIntOrNull(),
                category = map["category"] ?: throw IllegalArgumentException("Missing 'category' in frontmatter"),
                sourceFormat = map["sourceFormat"],
                description = map["description"],
                needsStdin = map["stdin"]?.toBoolean() ?: false,
                target = map["target"]?.let { target -> Target.fromString(target.trim()) } ?: Target.All,
                pkg = packageName,
            )
        }
    }

    @Serializable
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
