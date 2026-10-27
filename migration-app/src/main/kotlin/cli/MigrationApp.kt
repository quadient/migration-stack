package com.quadient.migration.cli

import com.github.ajalt.clikt.command.SuspendingCliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.quadient.migration.appModules
import io.ktor.server.config.ApplicationConfig
import io.ktor.server.config.MapApplicationConfig
import io.ktor.server.config.mergeWith
import io.ktor.server.netty.EngineMain
import org.koin.core.Koin
import org.koin.dsl.koinApplication

fun migrationApp() = MigrationApp().subcommands(ServeCommand(), ListCommand(), RunCommand())

class CliContext(val configPath: String, val configOverrides: Map<String, String>) {
    val koin: Koin by lazy {
        val config = ApplicationConfig(configPath).mergeWith(MapApplicationConfig(configOverrides.toList()))
        koinApplication { modules(appModules(config)) }.koin
    }
}

class MigrationApp : SuspendingCliktCommand(name = "migration-app") {
    override val invokeWithoutSubcommand = true

    private val configPath by option("--config", help = "Path to the config file, defaults to application.conf")
        .default("application.conf")
    private val appConfigOverrides by AppConfigOverrides()

    override fun help(context: Context) = "Migration app, starts the server when no command is given"

    override suspend fun run() {
        val cli = currentContext.findOrSetObject { CliContext(configPath, appConfigOverrides.toMap()) }
        when (val command = currentContext.invokedSubcommand) {
            null -> startServer(cli)
            is ServiceCommand -> command.setConsoleLogLevel()
        }
    }
}

internal fun startServer(cli: CliContext, port: Int? = null) {
    val ktorArgs = listOfNotNull("-config=${cli.configPath}", port?.let { "-port=$it" }) +
        cli.configOverrides.map { (key, value) -> "-P:$key=$value" }
    EngineMain.main(ktorArgs.toTypedArray())
}
