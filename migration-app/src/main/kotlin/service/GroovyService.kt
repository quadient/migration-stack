package com.quadient.migration.service

import com.quadient.migration.api.Migration
import groovy.lang.Binding
import groovy.lang.GroovyShell
import org.slf4j.LoggerFactory
import java.io.File
import java.io.Writer

class GroovyService() {
    fun runScript(script: ScriptMetadata, settings: Settings): RunScriptResult {
        return try {
            val migration = Migration(settings.migrationConfig, settings.projectConfig)

            val binding = Binding()
            binding.setVariable("migration", migration)
            binding.setVariable("out", LogWriter())

            val shell = GroovyShell(binding)
            val path = File(script.path)

            shell.evaluate(path)

            RunScriptResult.Ok()
        } catch (ex: Exception) {
            RunScriptResult.Err(ex)
        }
    }
}

sealed interface RunScriptResult {
    class Ok() : RunScriptResult
    data class Err(val ex: Throwable) : RunScriptResult
}

class LogWriter() : Writer() {
    val log = LoggerFactory.getLogger("GroovyStdout")!!

    override fun write(cbuf: CharArray?, off: Int, len: Int) {
        val text = String(cbuf ?: CharArray(0), off, len)
        if (text.isNotBlank()) {
            log.info(text)
        }
    }

    override fun flush() {}

    override fun close() {}
}