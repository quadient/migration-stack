package com.quadient.migration.shared

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize

@ConsistentCopyVisibility
@JsonSerialize(using = IcmPath.JacksonSerializer::class)
@JsonDeserialize(using = IcmPath.JacksonDeserializer::class)
data class IcmPath private constructor(val path: String) {
    companion object {
        private const val SCHEMA = "icm://"

        @JvmStatic
        fun from(path: String): IcmPath {
            return IcmPath(
                path.replace("vcs:", "icm:").removePrefix("/").removeSuffix("/").replace("\\", "/")
            )
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

    operator fun plus(other: IcmPath?): IcmPath {
        return this.join(other)
    }

    operator fun plus(other: String?): IcmPath {
        return this.join(other)
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