package com.quadient.migration.service

import com.quadient.migration.api.Migration
import groovy.lang.Binding
import groovy.lang.GroovyShell
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream

class GroovyService() {

    fun runScript(script: ScriptMetadata, migration: Migration): GroovyResult {
        return try {
            val binding = Binding()
            binding.setVariable("migration", migration)
            val shell = GroovyShell(binding)
            val path = File(script.path)
            val (stdout, stderr) = captureOut { shell.evaluate(path) }
            GroovyResult.Ok(stdout, stderr)
        } catch (ex: Exception) {
            GroovyResult.Err(ex)
        }
    }
}

sealed interface GroovyResult {
    data class Ok(val stdout: String, val stderr: String): GroovyResult
    data class Err(val ex: Throwable): GroovyResult
}

private fun captureOut(body: () -> Unit): Pair<String, String> {
    val outStream = ByteArrayOutputStream()
    val errStream = ByteArrayOutputStream()
    val prevOut = System.out
    val prevErr = System.err
    System.setOut(PrintStream(outStream))
    System.setErr(PrintStream(errStream))
    try {
        body()
    } finally {
        System.out.flush()
        System.err.flush()
        System.setOut(prevOut)
        System.setErr(prevErr)
    }
    return Pair(outStream.toString().trim(), errStream.toString().trim())
}