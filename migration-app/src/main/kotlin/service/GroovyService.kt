package com.quadient.migration.service

import com.quadient.migration.api.Migration
import com.quadient.migration.getScriptDir
import com.quadient.migration.log
import groovy.lang.Binding
import groovy.lang.GroovyClassLoader
import groovy.lang.GroovyShell
import io.ktor.server.config.ApplicationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.Writer

class GroovyService(val fileStorageService: FileStorageService, val config: ApplicationConfig) {
    private val classLoader = GroovyClassLoader(this.javaClass.classLoader).apply {
        addClasspath(config.getScriptDir())
    }

    suspend fun dispatchScript(script: ScriptMetadata, settings: Settings) = withContext(Dispatchers.IO) {
        runScript(script, settings)
    }

    fun runScript(script: ScriptMetadata, settings: Settings): RunScriptResult {
        return try {
            log.debug("Running script: ${script.path}")
            val migration = Migration(settings.migrationConfig, settings.projectConfig)

            val binding = Binding()
            binding.setVariable("migration", migration)
            binding.setVariable("out", LogWriter())
            binding.setVariable("DATA_DIR", fileStorageService.getStoragePath(StorageType.Modules).toString())

            val shell = GroovyShell(classLoader, binding)
            val path = File(script.path)

            shell.evaluate(path)

            RunScriptResult.Ok()
        } catch (ex: Exception) {
            log.error("Script execution failed: ${ex.stackTraceToString()}")
            RunScriptResult.Err(ex)
        }
    }
}

sealed interface RunScriptResult {
    class Ok() : RunScriptResult
    data class Err(val ex: Throwable) : RunScriptResult
}

class LogWriter() : Writer() {

    override fun write(cbuf: CharArray?, off: Int, len: Int) {
        val text = String(cbuf ?: CharArray(0), off, len)
        if (text.isNotBlank()) {
            log.info(text)
        }
    }

    override fun flush() {}

    override fun close() {}
}