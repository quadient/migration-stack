package com.quadient.migration

import kotlinx.coroutines.sync.Semaphore
import org.slf4j.LoggerFactory
import java.io.BufferedWriter

suspend inline fun <T> Semaphore.withPermitOrElse(
    onUnavailable: suspend () -> Unit, action: suspend () -> T
): T? {
    if (!tryAcquire()) {
        onUnavailable()
        return null
    }
    return try {
        action()
    } finally {
        release()
    }
}

fun BufferedWriter.tryWriteLine(text: String) {
    try {
        write(text)
        write("\n")
        flush()
    } catch (ex: Exception) {
        log.warn("Failed to write to output stream", ex)
    }
}

val loggerCache = mutableMapOf<Class<*>, org.slf4j.Logger>()
val Any.log: org.slf4j.Logger
    get() = loggerCache.getOrPut(this::class.java) { LoggerFactory.getLogger(this::class.java)!! }