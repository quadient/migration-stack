package com.quadient.migration.cli

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.filter.ThresholdFilter
import ch.qos.logback.core.ConsoleAppender
import com.github.ajalt.clikt.command.SuspendingCliktCommand
import com.github.ajalt.clikt.core.requireObject
import org.koin.core.component.KoinComponent
import org.slf4j.Logger
import org.slf4j.LoggerFactory

abstract class ServiceCommand(name: String? = null) : SuspendingCliktCommand(name), KoinComponent {
    private val cli by requireObject<CliContext>()

    open val consoleLogLevel: Level = Level.WARN

    override fun getKoin() = cli.koin

    internal fun setConsoleLogLevel() {
        val filter = ThresholdFilter().apply {
            setLevel(consoleLogLevel.levelStr)
            start()
        }
        val root = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as ch.qos.logback.classic.Logger
        root.iteratorForAppenders().asSequence().filter { it is ConsoleAppender<*> }.forEach { it.addFilter(filter) }
    }
}
