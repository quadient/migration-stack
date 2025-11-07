package com.quadient.migration.shared

import kotlinx.datetime.Instant
import kotlinx.datetime.toJavaInstant
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@Serializable
data class WriteMetadata(val path: String, val metadata: List<WriteMetadataValue>)

@Serializable
data class WriteMetadataValue(val key: String, val values: List<MetadataPrimitive>)

@Serializable
data class IcmFileMetadata(val path: String, val metadata: Map<String, MetadataValue>) {
    fun toWrite(): WriteMetadata {
        val writeMetadataValues = metadata.map { (key, value) ->
            WriteMetadataValue(
                key = key, values = value.values
            )
        }
        return WriteMetadata(
            path = path, metadata = writeMetadataValues
        )
    }
}

@Serializable
data class MetadataValue(val values: List<MetadataPrimitive>, val system: Boolean = false)

fun Map<String, List<MetadataPrimitive>>.toMetadata(system: Boolean = false): Map<String, MetadataValue> {
    return mapValues { (_, v) -> MetadataValue(v, system) }
}

@Serializable
sealed interface MetadataPrimitive {
    @Serializable
    @SerialName("string")
    data class Str(val value: String) : MetadataPrimitive

    @Serializable
    @SerialName("bool")
    data class Bool(val value: Boolean) : MetadataPrimitive

    @Serializable
    @SerialName("int")
    data class Integer(val value: Long) : MetadataPrimitive

    @Serializable
    @SerialName("float")
    data class Float(val value: Double) : MetadataPrimitive

    @Serializable
    @SerialName("datetime")
    data class DateTime(var value: IcmDateTime) : MetadataPrimitive {
        init {
            // Truncate the datetime to milliseconds precision because
            // designer does not support more precision in its ISO8601
            // implementation
            this.value = IcmDateTime(Instant.fromEpochMilliseconds(value.value.toEpochMilliseconds()))
        }
    }
}


@Serializable(with = IcmDateTimeSerializer::class)
@JvmInline
value class IcmDateTime(val value: Instant) {
    override fun toString(): String = value.toString()
}

object IcmDateTimeSerializer : KSerializer<IcmDateTime> {
    // Designer has limited ISO-8601 support
    // So the output format has to match exactly: "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
    private val outputFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneOffset.UTC)

    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("IcmDateTime", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: IcmDateTime) {
        encoder.encodeString(outputFormatter.format(value.value.toJavaInstant()))
    }

    override fun deserialize(decoder: Decoder): IcmDateTime {
        val text = decoder.decodeString()
        val inst = try {
            Instant.parse(text)
        } catch (e: Exception) {
            throw SerializationException("Failed to parse Instant from '$text': ${e.message}", e)
        }
        return IcmDateTime(inst)
    }
}
