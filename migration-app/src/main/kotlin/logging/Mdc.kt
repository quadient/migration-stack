package com.quadient.migration.logging

import kotlinx.coroutines.runBlocking
import org.slf4j.MDC

fun <T> withMdc(key: String, value: String, block: () -> T): T {
    return runBlocking { runWithMdc(key, value, block) }
}

suspend fun <T> runWithMdc(key: String, value: String, block: suspend () -> T): T {
    val prev = MDC.get(key)
    MDC.put(key, value)
    try {
        return block()
    } finally {
        setMdc(key, prev)
    }
}

fun setMdc(key: String, value: String?) {
    if (value.isNullOrBlank()) {
        MDC.remove(key)
    } else {
        MDC.put(key, value)
    }
}