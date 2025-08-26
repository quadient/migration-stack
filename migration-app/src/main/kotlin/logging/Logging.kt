package com.quadient.migration.logging

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.filter.LevelFilter
import org.slf4j.LoggerFactory

const val ScriptIdMdcKey = "scriptRunId"

class Logging {
    val appender = PerIdInMemoryAppender(ScriptIdMdcKey)

    init {
        val logger = LoggerFactory.getLogger("root")
        val logbackLogger = logger as? ch.qos.logback.classic.Logger
        appender.addFilter(ExistingMdcValueFilter(ScriptIdMdcKey))
        val filter = LevelFilter()
        filter.setLevel(Level.DEBUG)
        appender.addFilter(filter)

        appender.start()
        logbackLogger?.addAppender(appender)
    }

    fun getLogsForScript(path: String): List<String> {
        return appender.map[path] ?: emptyList()
    }

    fun clearLogsForScript(path: String) {
        appender.map.remove(path)
    }
}