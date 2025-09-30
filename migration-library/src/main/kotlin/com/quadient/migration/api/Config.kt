package com.quadient.migration.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.toml.TomlFactory
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.quadient.migration.shared.IcmPath
import java.io.File
import java.net.URI

data class MigConfig(
    val dbConfig: DbConfig = DbConfig(),
    val inspireConfig: InspireConfig = InspireConfig(),
    val storageRoot: String? = null
) {
    companion object {
        val objectMapper = ObjectMapper(TomlFactory()).registerKotlinModule()

        @JvmStatic
        fun fromString(input: String): MigConfig = objectMapper.readValue(input, MigConfig::class.java)

        @JvmStatic
        fun read(path: String): MigConfig = fromString(File(path).readText())

        @JvmStatic
        fun read(path: URI): MigConfig = fromString(File(path).readText())
    }
}

enum class InspireOutput {
    Interactive, Designer, Evolve
}

data class ProjectConfig(
    val name: String,
    val baseTemplatePath: String,
    val inputDataPath: String,
    val interactiveTenant: String,
    val defaultTargetFolder: IcmPath? = null,
    val paths: PathsConfig = PathsConfig(),
    val inspireOutput: InspireOutput = InspireOutput.Interactive,
    val sourceBaseTemplatePath: String? = null,
    val defaultVariableStructure: String? = null,
    val context: Map<String, Any> = emptyMap(),
) {
    companion object {
        val objectMapper = ObjectMapper(TomlFactory()).registerKotlinModule()

        @JvmStatic
        fun fromString(input: String): ProjectConfig = objectMapper.readValue(input, ProjectConfig::class.java)

        @JvmStatic
        fun read(path: String): ProjectConfig = fromString(File(path).readText())

        @JvmStatic
        fun read(path: URI): ProjectConfig = fromString(File(path).readText())
    }
}

data class DbConfig(
    val host: String = "localhost",
    val port: Int = 5432,
    val dbName: String = "migrationdb",
    val user: String = "migrationadmin",
    val password: String = "password"
) {
    fun connectionString() = "jdbc:postgresql://$host:$port/$dbName"
}

data class InspireConfig(val ipsConfig: IpsConfig = IpsConfig())

data class IpsConfig(val host: String = "localhost", val port: Int = 30354, val timeoutSeconds: Int = 120)

data class PathsConfig(val images: IcmPath? = null, val fonts: IcmPath? = null)