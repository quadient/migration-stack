package com.quadient.migration.api

import com.akuleshov7.ktoml.Toml
import com.quadient.migration.shared.IcmPath
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import java.io.File
import java.net.URI

@Serializable
data class MigConfig(
    val dbConfig: DbConfig = DbConfig(),
    val inspireConfig: InspireConfig = InspireConfig(),
    val storageRoot: String? = null
) {
    companion object {
        @JvmStatic
        fun fromString(input: String): MigConfig = Toml.decodeFromString(input)

        @JvmStatic
        fun read(path: String): MigConfig = fromString(File(path).readText())

        @JvmStatic
        fun read(path: URI): MigConfig = fromString(File(path).readText())
    }
}

@Serializable
enum class InspireOutput {
    Interactive, Designer, Evolve
}

@Serializable
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
    val context: ContextMap = ContextMap(emptyMap()),
) {
    companion object {
        @JvmStatic
        fun fromString(input: String): ProjectConfig = Toml.decodeFromString(input)

        @JvmStatic
        fun read(path: String): ProjectConfig = fromString(File(path).readText())

        @JvmStatic
        fun read(path: URI): ProjectConfig = fromString(File(path).readText())
    }
}

@Serializable
data class DbConfig(
    val host: String = "localhost",
    val port: Int = 5432,
    val dbName: String = "migrationdb",
    val user: String = "migrationadmin",
    val password: String = "password"
) {
    fun connectionString() = "jdbc:postgresql://$host:$port/$dbName"
}

@Serializable
data class InspireConfig(val ipsConfig: IpsConfig = IpsConfig())

@Serializable
data class IpsConfig(val host: String = "localhost", val port: Int = 30354, val timeoutSeconds: Int = 120)

@Serializable
data class PathsConfig(val images: IcmPath? = null)