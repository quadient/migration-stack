package com.quadient.migration.service

import com.quadient.migration.getAppDataDir
import com.quadient.migration.getModulesDataDir
import com.quadient.migration.log
import io.ktor.server.config.*
import kotlinx.io.files.FileNotFoundException
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.nio.file.Path
import kotlin.io.path.Path

class FileStorageService(val config: ApplicationConfig) {
    inline fun <reified T> readAppJson(vararg subpaths: String): T? {
        return loadAppFile(*subpaths)?.let { Json.decodeFromString(it.reader().readText()) }
    }

    inline fun <reified T> writeAppJson(content: T, vararg subpaths: String) {
        val content = Json.encodeToString(content)
        writeAppFile(content.toByteArray(), *subpaths)
    }

    fun loadAppFile(vararg subpaths: String): InputStream? {
        return try {
            val path = getStoragePath(StorageType.App, *subpaths)
            path.toFile().inputStream()
        } catch (_: FileNotFoundException) {
            log.warn("App file ${subpaths.joinToString("/")} not found")
            return null
        }
    }

    fun writeAppFile(content: ByteArray, vararg subpaths: String) {
        val file = getStoragePath(StorageType.App, *subpaths).toFile()
        file.parentFile?.mkdirs()
        file.writeBytes(content)
        log.debug("Written app file: ${file.absolutePath}")
    }

    inline fun <reified T> readModuleJson(vararg subpaths: String): T? {
        return loadModuleFile(*subpaths)?.let { Json.decodeFromString(it.reader().readText()) }
    }

    inline fun <reified T> writeModuleJson(content: T, vararg subpaths: String) {
        val content = Json.encodeToString(content)
        writeAppFile(content.toByteArray(), *subpaths)
    }

    fun loadModuleFile(vararg subpaths: String): InputStream? {
        return try {
            val path = getStoragePath(StorageType.Modules, *subpaths)
            path.toFile().inputStream()
        } catch (_: FileNotFoundException) {
            log.warn("Module file ${subpaths.joinToString("/")} not found")
            return null
        }
    }

    fun writeModuleFile(content: String, vararg subpaths: String) {
        val file = getStoragePath(StorageType.Modules, *subpaths).toFile()
        file.parentFile?.mkdirs()
        file.writeText(content)
        log.debug("Written module file: ${file.absolutePath}")
    }

    fun getStoragePath(type: StorageType, vararg subpaths: String): Path {
        val dataDir = when (type) {
            StorageType.App -> config.getAppDataDir()
            StorageType.Modules -> config.getModulesDataDir()
        }
        return Path(dataDir, *subpaths)
    }

    fun list(type: StorageType, vararg subpaths: String): List<Path> {
        val dir = getStoragePath(type, *subpaths).toFile()
        if (!dir.exists() || !dir.isDirectory) {
            return emptyList()
        }

        return dir.listFiles().map { it.toPath() }
    }
}

enum class StorageType {
    App, Modules
}