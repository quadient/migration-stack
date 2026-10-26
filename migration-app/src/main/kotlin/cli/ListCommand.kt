package com.quadient.migration.cli

import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.quadient.migration.service.ScriptDiscoveryService
import kotlinx.serialization.json.Json
import org.koin.core.component.inject

class ListCommand : ServiceCommand() {
    private val scriptDiscoveryService by inject<ScriptDiscoveryService>()
    private val json by option("-j", "--json", help = "Output in JSON format, defaults to false").flag()

    override fun help(context: Context) = "List available modules"

    override suspend fun run() {
        val scripts = scriptDiscoveryService.getScripts()
        if (scripts.isEmpty()) {
            echo("No modules found")
            throw ProgramResult(1)
        }

        if (json) {
            echo(Json.encodeToString(scripts))
        } else {
            for ((idx, script) in scripts.withIndex()) {
                if (idx > 0) echo("")
                echo("Id ${script.id}")
                echo("  filename: ${script.filename}")
                echo("  path: ${script.path}")
                echo("  display-name: ${script.displayName}")
                echo("  category: ${script.category}")
                echo("  source-format: ${script.sourceFormat}")
                echo("  description: ${script.description}")
                echo("  package: ${script.pkg}")
                echo("  needs-stdin: ${script.needsStdin}")
                echo("  target: ${script.target}")
                echo("  order: ${script.order}")
            }
        }
    }
}
