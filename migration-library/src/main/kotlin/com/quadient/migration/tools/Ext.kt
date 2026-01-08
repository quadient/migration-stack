package com.quadient.migration.tools

import java.util.*

fun String.substringOrNull(range: IntRange) = try {
    this.substring(range)
} catch (e: Exception) {
    null
}

fun String.escapeQuotes() = this.replace("\"", "\\\"")
fun String.surroundWith(seq: CharSequence) = "$seq$this$seq"
fun <T> List<T>?.concat(other: List<T>?): List<T> = (this ?: emptyList()) + (other ?: emptyList())

/**
 * Returns the last element of the list or appends the default value and returns it
 */
fun <T> MutableList<T>.lastOrElse(default: () -> T): T {
    val last = this.lastOrNull()
    return if (last == null) {
        val default = default()
        this.add(default)
        default
    } else {
        last
    }
}

fun <K, V> MutableMap<K, V>.getOrPutOrNull(key: K, block: () -> V?): V? {
    val stored = this[key]
    if (stored != null) {
        return stored
    }

    val value = block()
    if (value != null) {
        this.put(key, value)
    }
    return value
}

fun <K, V> MutableMap<K, V>.computeIfPresentOrPut(key: K, defaultValue: V, computeFun: (V) -> V): V {
    val stored = this[key]
    return if (stored != null) {
        val computed = computeFun(stored)
        this[key] = computed
        computed
    } else {
        this[key] = defaultValue
        defaultValue
    }

}

inline fun <reified T: Enum<T>> Array<T>.toEnumSet(): EnumSet<T> {
    val result = EnumSet.noneOf(T::class.java)
    for (enum in this) {
        result.add(enum)
    }
    return result
}

fun caseInsensitiveSetOf(vararg elements: String): Set<String> {
    val set = TreeSet(String.CASE_INSENSITIVE_ORDER)
    if (elements.isNotEmpty()) {
        set.addAll(elements)
    }

    return set
}

