package com.quadient.migration.service

import com.quadient.migration.api.Migration
import groovy.lang.Binding
import groovy.lang.GroovyShell
import java.io.File

class GroovyService() {

    fun runScript(script: ScriptMetadata, migration: Migration): GroovyResult {
        return try {
            val binding = Binding()
            binding.setVariable("migration", migration)
            val shell = GroovyShell(binding)
            val path = File(script.path)
            // TODO capture stdout and stderr anyway
            shell.evaluate(path)
            GroovyResult.Ok()
        } catch (ex: Exception) {
            GroovyResult.Err(ex)
        }
    }
}

sealed interface GroovyResult {
    class Ok(): GroovyResult
    data class Err(val ex: Throwable): GroovyResult
}