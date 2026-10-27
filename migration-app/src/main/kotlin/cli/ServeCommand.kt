package com.quadient.migration.cli

import com.github.ajalt.clikt.command.SuspendingCliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int

class ServeCommand : SuspendingCliktCommand() {
    private val cli by requireObject<CliContext>()
    private val port by option("--port", "-p", help = "Port to listen on, overrides ktor.deployment.port").int()

    override fun help(context: Context) = "Start the server"

    override suspend fun run() = startServer(cli, port)
}