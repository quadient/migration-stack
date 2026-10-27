package com.quadient.migration.cli

import com.quadient.migration.service.ScriptDiscoveryService
import org.koin.core.component.inject
import ch.qos.logback.classic.Level
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.option
import com.quadient.migration.logging.Logging
import com.quadient.migration.service.GroovyService
import com.quadient.migration.service.RunScriptResult
import com.quadient.migration.service.SettingsService
import com.quadient.migration.service.toScriptId

class RunCommand : ServiceCommand() {
    override val consoleLogLevel = Level.DEBUG
    private val scriptDiscoveryService by inject<ScriptDiscoveryService>()
    private val groovyService by inject<GroovyService>()
    private val logging by inject<Logging>()
    private val settingsService by inject<SettingsService>()
    private val id by argument(help = "One or more module id(s) to run").multiple(required = true)
    private val overrides by ProjectConfigOverrides()
    private val scriptArgs by option("--script-args", help = "Arguments passed to the modules, split on spaces")

    override fun help(context: Context) = "Run a module by id"

    override suspend fun run() {
        val scripts = scriptDiscoveryService.getScripts()
        val queue = id.map { id ->
            scripts.firstOrNull { it.id == id.toScriptId() } ?: run {
                echo("Module with id '$id' not found")
                throw ProgramResult(1)
            }
        }

        val baseSettings = settingsService.getSettings()
        val settings = baseSettings.copy(projectConfig = overrides.applyTo(baseSettings.projectConfig))
        val args = scriptArgs?.split(" ")?.filter { it.isNotEmpty() } ?: emptyList()

        for (script in queue) {
            echo("Running module ${script.displayName ?: script.filename}, id: ${script.id}")

            val result = logging.capture(script.id.toString(), {
                echo(it)
            }) {
                groovyService.dispatchScript(script, settings, args)
            }

            when (result) {
                is RunScriptResult.Ok -> {
                    echo("Module '${script.displayName ?: script.filename}' completed successfully, id: ${script.id}")
                }

                is RunScriptResult.Err -> {
                    val message = result.ex.message ?: "Unknown error"
                    echo("Module '${script.displayName ?: script.filename}' failed with error: $message, id: ${script.id}")
                    throw ProgramResult(1)
                }
            }
        }
    }
}