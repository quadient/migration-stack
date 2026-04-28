package com.quadient.migration.shared

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@ConsistentCopyVisibility
@JsonSerialize(using = IcmPath.JacksonSerializer::class)
@JsonDeserialize(using = IcmPath.JacksonDeserializer::class)
@Serializable(with = IcmPathSerializer::class)
data class IcmPath private constructor(val path: String) {
    companion object {
        private const val SCHEMA = "icm://"

        @JvmStatic
        fun from(path: String): IcmPath {
            var sanitized = path.replace("vcs:", "icm:").removePrefix("/").replace("\\", "/")
            if (sanitized.endsWith("/") && !sanitized.endsWith("//")) {
                sanitized = sanitized.removeSuffix("/")
            }

            return IcmPath(sanitized)
        }

        fun root(): IcmPath {
            return IcmPath(SCHEMA)
        }
    }

    fun isAbsolute() = path.startsWith(SCHEMA)
    override fun toString() = path

    fun join(other: IcmPath?): IcmPath {
        if (other.isNullOrBlank()) {
            return this
        }

        if (other?.isAbsolute() == true) {
            throw IllegalArgumentException("Cannot join with absolute path '${other.path}'")
        }

        if (other != null && this.path == SCHEMA) {
            return IcmPath("${this.path}${other.path}")
        }

        return IcmPath("${this.path}/${other?.path}")
    }

    fun join(other: String?): IcmPath {
        return this.join(other?.let(::from))
    }

    fun toMapInteractive(tenant: String): String {
        if (!this.isAbsolute()) {
            throw IllegalStateException("Cannot convert relative path '$path' to map://interactive format")
        }

        if (!path.startsWith("icm://Interactive/$tenant/")) {
            throw IllegalStateException("Cannot convert path '$path' to map://interactive format because it does not start with 'icm://Interactive/$tenant/'")
        }

        return "map://interactive/" + this.toString().removePrefix("icm://Interactive/$tenant/")
    }

    fun extension(ext: String): IcmPath {
        val normalizedExt = if (ext.startsWith(".")) ext else ".$ext"
        val lastSlash = path.lastIndexOf('/')
        val lastDot = path.lastIndexOf('.')
        return if (lastDot > lastSlash && lastSlash != -1) {
            // Path has an existing extension after the last path separator — replace it
            IcmPath(path.substring(0, lastDot) + normalizedExt)
        } else {
            // No extension found — append
            IcmPath(path + normalizedExt)
        }
    }

    operator fun plus(other: IcmPath?): IcmPath {
        return this.join(other)
    }

    operator fun plus(other: String?): IcmPath {
        return this.join(other)
    }

    override fun equals(other: Any?): Boolean {
        if (other is String) {
            return this.path == other
        }
        if (other is IcmPath) {
            return this.path == other.path
        }
        return false
    }

    override fun hashCode(): Int {
        return path.hashCode()
    }

    object JacksonSerializer : JsonSerializer<IcmPath>() {
        override fun serialize(value: IcmPath, gen: JsonGenerator, serializers: SerializerProvider) {
            gen.writeString(value.toString())
        }
    }

    object JacksonDeserializer : JsonDeserializer<IcmPath>() {
        override fun deserialize(p: JsonParser, ctxt: DeserializationContext): IcmPath {
            return from(p.valueAsString)
        }
    }
}

fun IcmPath?.isNullOrBlank(): Boolean {
    return this == null || this.toString().isBlank()
}

fun IcmPath?.orDefault(default: IcmPath): IcmPath {
    return this.orDefault(default.path)
}

fun IcmPath?.orDefault(default: String): IcmPath {
    return if (this.isNullOrBlank()) {
        IcmPath.from(default)
    } else {
        // Non-null checked above so it is safe to assert
        this!!
    }
}

fun String.toIcmPath() = IcmPath.from(this)

object IcmPathSerializer : KSerializer<IcmPath> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("IcmPath", PrimitiveKind.STRING)

    override fun serialize(
        encoder: Encoder,
        value: IcmPath
    ) {
        encoder.encodeString(value.path)
    }

    override fun deserialize(decoder: Decoder): IcmPath {
        val path = decoder.decodeString()
        return IcmPath.from(path)
    }
}
