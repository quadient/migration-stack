package com.quadient.migration.api

import com.akuleshov7.ktoml.Toml
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import java.io.File
import java.net.URI
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@Serializable
data class MigConfig(val dbConfig: DbConfig, val inspireConfig: InspireConfig, val storageRoot: String? = null) {
    companion object {
        @JvmStatic
        fun fromString(input: String): MigConfig = Toml.decodeFromString(input)

        @JvmStatic
        fun read(path: String): MigConfig = fromString(File(path).readText())

        @JvmStatic
        fun read(path: URI): MigConfig = fromString(File(path).readText())
    }
}

enum class InspireOutput {
    Interactive, Designer, Evolve
}

@Serializable
data class ProjectConfig(
    val name: String,
    val baseTemplatePath: String,
    val inputDataPath: String,
    val interactiveTenant: String,
    val defaultTargetFolder: String? = null,
    val inspireOutput: InspireOutput = InspireOutput.Interactive,
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
data class DbConfig(val host: String, val port: Int, val dbName: String, val user: String, val password: String) {
    fun connectionString() = "jdbc:postgresql://$host:$port/$dbName"
}

@Serializable
data class InspireConfig(val ipsConfig: IpsConfig = IpsConfig())

@Serializable
data class IpsConfig(val host: String = "localhost", val port: Int = 30354, val timeoutSeconds: Int = 120)