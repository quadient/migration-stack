package com.quadient.migration.shared

import kotlinx.datetime.format
import kotlinx.datetime.format.DateTimeComponents
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
@SerialName("Categorization")
data class Categorization(val name: String, val fields: List<CategorizationMetadata>) : MetadataEntry

@Serializable
data class CategorizationMetadata(val key: String, val value: List<CategorizationPrimitive>)

@Serializable
sealed interface CategorizationPrimitive {
    companion object {
        private val INSTANT_FORMAT = DateTimeComponents.Format {
            year()
            monthNumber()
            day()
            hour()
            minute()
            second()
        }
    }

    @Serializable
    @SerialName("string")
    data class Str(val value: String) : CategorizationPrimitive

    @Serializable
    @SerialName("bool")
    data class Bool(val value: Boolean) : CategorizationPrimitive

    @Serializable
    @SerialName("number")
    data class Number(val value: Double) : CategorizationPrimitive

    @Serializable
    @SerialName("date")
    data class Date(val value: Instant) : CategorizationPrimitive

    @Serializable
    @SerialName("validityRange")
    data class ValidityRange(val start: Instant, val end: Instant) : CategorizationPrimitive

    fun serialize(): String {
        return when (this) {
            is Str -> this.value
            is Bool -> this.value.toString()
            is Number -> this.value.toString()
            is Date -> this.value.format(INSTANT_FORMAT)
            is ValidityRange -> "${this.start.format(INSTANT_FORMAT)}-${this.end.format(INSTANT_FORMAT)}"
        }
    }

    fun toMetadataPrimitive(): MetadataPrimitive = MetadataPrimitive.Str(serialize())
}


