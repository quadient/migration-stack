package com.quadient.migration.shared

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@ConsistentCopyVisibility
@Serializable(with = IcmPathSerializer::class)
data class IcmPath private constructor(internal val path: String) {
    companion object {
        private const val SCHEMA = "icm://"

        @JvmStatic
        fun from(path: String): IcmPath {
            return IcmPath(path.replace("vcs:", "icm:").removePrefix("/").removeSuffix("/").replace("\\", "/"))
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
        PrimitiveSerialDescriptor("com.quadient.migration.shared.IcmPath", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: IcmPath) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): IcmPath {
        return IcmPath.from(decoder.decodeString())
    }
}
