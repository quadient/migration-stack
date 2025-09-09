package com.quadient.migration.logging

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.filter.Filter
import ch.qos.logback.core.spi.FilterReply

class ExistingMdcValueFilter(private val id: String): Filter<ILoggingEvent>() {
    override fun decide(event: ILoggingEvent): FilterReply {
        return if (event.mdcPropertyMap.containsKey(id)) {
            FilterReply.ACCEPT
        } else {
            FilterReply.DENY
        }
    }
}
