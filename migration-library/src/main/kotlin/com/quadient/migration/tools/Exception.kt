package com.quadient.migration.tools

class UnreachableCodeException(message: Any?): IllegalStateException(message.toString())

/**
 * Throws an [UnreachableCodeException] with the given message.
 * Should be used when kotlin compiler cannot determine when path is unreachable but the
 * developer is confident that it is. Use only when you absolutely have to.
 */
fun unreachable(message: Any): Nothing = throw UnreachableCodeException(message.toString())