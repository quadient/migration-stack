package com.quadient.migration.logging

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.filter.LevelFilter
import org.slf4j.LoggerFactory

const val mdcId = "runId"

class Logging {
    val appender = PerIdInMemoryAppender(mdcId)

    init {
        val logger = LoggerFactory.getLogger("root")
        val logbackLogger = logger as ch.qos.logback.classic.Logger
        appender.addFilter(ExistingMdcValueFilter(mdcId))
        val filter = LevelFilter()
        filter.setLevel(Level.DEBUG)
        appender.addFilter(filter)

        appender.start()
        logbackLogger.addAppender(appender)
    }

    suspend fun <T> capture(id: String, block: suspend () -> T): ResultWithLogs<T> {
        return try {
            ResultWithLogs(runWithMdc(mdcId, id, block), appender.map[id] ?: emptyList())
        } finally {
            appender.map.remove(id)
        }
    }
}

data class ResultWithLogs<T>(val result: T, val logs: List<String>)