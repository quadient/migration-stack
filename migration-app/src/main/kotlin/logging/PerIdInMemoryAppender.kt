package com.quadient.migration.logging

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.AppenderBase
import co.touchlab.stately.collections.ConcurrentMutableList
import java.time.Instant
import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class PerIdInMemoryAppender(private val id: String) : AppenderBase<ILoggingEvent>() {
    val map = ConcurrentHashMap<String, ConcurrentMutableList<String>>()
    val onLogCallbacks = ConcurrentHashMap<String, (String) -> Unit>()

    override fun append(event: ILoggingEvent) {
        val id = event.mdcPropertyMap[id] ?: return
        val log = "${event.date} ${event.level} ${event.message}"

        onLogCallbacks.get(id)?.invoke(log)
        map.getOrPut(id) { ConcurrentMutableList() }.add(log)
    }

    val ILoggingEvent.date: LocalDateTime
        get() = LocalDateTime.ofInstant(Instant.ofEpochMilli(this.timeStamp), TimeZone.getDefault().toZoneId())
}
