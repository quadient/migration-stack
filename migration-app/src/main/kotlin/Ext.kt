package com.quadient.migration

import kotlinx.coroutines.sync.Semaphore

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
