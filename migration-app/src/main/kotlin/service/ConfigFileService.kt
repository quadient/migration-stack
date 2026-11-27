package com.quadient.migration.service

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.quadient.migration.api.MigConfig
import com.quadient.migration.api.ProjectConfig
import com.quadient.migration.getConfigsDir
import io.ktor.server.config.ApplicationConfig
import tools.jackson.databind.JsonNode
import tools.jackson.dataformat.toml.TomlMapper
import tools.jackson.module.kotlin.KotlinModule
import java.io.File

enum class ConfigKind(val fileName: String, val templateFileName: String) {
    Project("project-config.toml", "project-config-template.toml"),
    Migration("migration-config.toml", "migration-config-template.toml"),
}

const val DEFAULT_NAME = "default"

open class ConfigFileException(message: String) : IllegalArgumentException(message)

class ConfigFileMissingException(message: String) : ConfigFileException(message)

class ConfigFileService(config: ApplicationConfig) {
    private val dir = File(config.getConfigsDir())

    fun list(kind: ConfigKind) = names(kind.fileName)

    fun listTemplates(kind: ConfigKind) = names(kind.templateFileName)

    fun exists(kind: ConfigKind, name: String) = file(kind, name).isFile

    fun readProject(name: String): ProjectConfig = read(file(ConfigKind.Project, name), ProjectConfig::fromString)

    fun readMigration(name: String): MigConfig = read(file(ConfigKind.Migration, name), MigConfig::fromString)

    fun writeProject(name: String, config: ProjectConfig) = write(ConfigKind.Project, name, config, ProjectConfig::fromString)

    fun writeMigration(name: String, config: MigConfig) = write(ConfigKind.Migration, name, config, MigConfig::fromString)

    fun create(kind: ConfigKind, name: String, cloneFrom: String?, template: String? = null) {
        require(cloneFrom == null || template == null) { "Use either cloneFrom or template, not both" }
        check(!exists(kind, name)) { "Config '$name' already exists" }
        when (kind) {
            ConfigKind.Project -> {
                val source = when {
                    cloneFrom != null -> readProject(cloneFrom)
                    template != null -> read(templateFile(kind, template), ProjectConfig::fromString)
                    else -> ProjectConfig(name, "", null, "", "StandardPackage")
                }
                writeProject(name, source.copy(name = name))
            }

            ConfigKind.Migration -> {
                val source = when {
                    cloneFrom != null -> readMigration(cloneFrom)
                    template != null -> read(templateFile(kind, template), MigConfig::fromString)
                    else -> MigConfig()
                }
                writeMigration(name, source)
            }
        }
    }

    private fun names(fileName: String): List<String> {
        val files = dir.list() ?: return emptyList()
        val prefixed = files.filter { it.endsWith("-$fileName") }
            .map { it.removeSuffix("-$fileName") }
            .filter { it.isNotEmpty() && it != DEFAULT_NAME }
        val default = if (fileName in files) listOf(DEFAULT_NAME) else emptyList()
        return (default + prefixed).sorted()
    }

    private fun file(kind: ConfigKind, name: String) = namedFile(kind.fileName, name)

    private fun templateFile(kind: ConfigKind, template: String) = namedFile(kind.templateFileName, template)

    private fun namedFile(fileName: String, name: String) =
        File(dir, if (name == DEFAULT_NAME) fileName else "$name-$fileName")

    private fun <T> read(file: File, parse: (String) -> T): T {
        if (!file.isFile) {
            throw ConfigFileMissingException("Config file ${file.name} does not exist")
        }
        return try {
            parse(file.readText())
        } catch (e: Exception) {
            throw ConfigFileException("Cannot read ${file.name}: ${e.message}")
        }
    }

    private fun <T : Any> write(kind: ConfigKind, name: String, config: T, parse: (String) -> T) {
        val file = file(kind, name)
        if (file.isFile && sameValues(config, read(file, parse))) {
            return
        }
        dir.mkdirs()
        file.writeText(tomlWriter.writeValueAsString(config))
    }

    private fun sameValues(a: Any, b: Any) =
        tomlWriter.valueToTree<JsonNode>(a).equals(numbersByValue, tomlWriter.valueToTree<JsonNode>(b))
}

private val numbersByValue = Comparator<JsonNode> { a, b ->
    if (a == b || (a.isNumber && b.isNumber && a.doubleValue() == b.doubleValue())) 0 else 1
}

@JsonIgnoreProperties("documentObjectsToProcess")
private abstract class IgnoreComputedProjectFields

private val tomlWriter = TomlMapper.builder()
    .addModule(KotlinModule.Builder().build())
    .changeDefaultPropertyInclusion { it.withValueInclusion(JsonInclude.Include.NON_NULL) }
    .addMixIn(ProjectConfig::class.java, IgnoreComputedProjectFields::class.java)
    .build()
