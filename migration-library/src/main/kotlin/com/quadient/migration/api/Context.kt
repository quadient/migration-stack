package com.quadient.migration.api

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = ContextNodeSerializer::class)
sealed interface ContextNode

class ContextInt(val value: Int) : ContextNode

class ContextString(val value: String) : ContextNode

class ContextBoolean(val value: Boolean) : ContextNode

@Serializable(with = ContextArraySerializer::class)
class ContextArray(val value: List<Any>) : ContextNode

class ContextNull : ContextNode

@Serializable(with = ContextMapSerializer::class)
class ContextMap(map: Map<String, Any>) : ContextNode, HashMap<String, Any>(map)

object ContextMapSerializer : KSerializer<ContextMap> {
    val surrogate: KSerializer<Map<String, ContextNode>> = MapSerializer(String.serializer(), ContextNode.serializer())
    override val descriptor: SerialDescriptor = surrogate.descriptor

    @Suppress("UNCHECKED_CAST", "SENSELESS_NULL_IN_WHEN", "DuplicatedCode")
    override fun serialize(encoder: Encoder, value: ContextMap) {
        encoder.encodeSerializableValue(surrogate, value.mapValues { (_, v) ->
            when (v) {
                is Map<*, *> -> ContextMap(v as Map<String, Any>)
                is Int -> ContextInt(v)
                is String -> ContextString(v)
                is Boolean -> ContextBoolean(v)
                is List<*> -> ContextArray(v as List<Any>)
                null -> ContextNull()
                else -> throw IllegalArgumentException("Unsupported type in ContextMap: ${v::class.simpleName}")
            }
        })
    }

    override fun deserialize(decoder: Decoder): ContextMap {
        return ContextMap(decoder.decodeSerializableValue(surrogate))
    }
}

object ContextArraySerializer : KSerializer<ContextArray> {
    val surrogate: KSerializer<List<ContextNode>> = ListSerializer(ContextNode.serializer())
    override val descriptor: SerialDescriptor = surrogate.descriptor

    @Suppress("UNCHECKED_CAST", "SENSELESS_NULL_IN_WHEN", "DuplicatedCode")
    override fun serialize(encoder: Encoder, value: ContextArray) {
        encoder.encodeSerializableValue(surrogate, value.value.map { v ->
            when (v) {
                is Map<*, *> -> ContextMap(v as Map<String, Any>)
                is Int -> ContextInt(v)
                is String -> ContextString(v)
                is Boolean -> ContextBoolean(v)
                is List<*> -> ContextArray(v as List<Any>)
                null -> ContextNull()
                else -> throw IllegalArgumentException("Unsupported type in ContextMap: ${v::class.simpleName}")
            }
        })
    }

    override fun deserialize(decoder: Decoder): ContextArray {
        return ContextArray(emptyList())
    }
}

object ContextNodeSerializer : KSerializer<ContextNode> {
    override val descriptor =
        PrimitiveSerialDescriptor("com.quadient.migration.api.ContextNodeSerializer", PrimitiveKind.STRING)

    @OptIn(ExperimentalSerializationApi::class)
    override fun serialize(encoder: Encoder, value: ContextNode) {
        when (value) {
            is ContextMap -> encoder.encodeSerializableValue(ContextMap.serializer(), value)
            is ContextInt -> encoder.encodeInt(value.value)
            is ContextBoolean -> encoder.encodeBoolean(value.value)
            is ContextString -> encoder.encodeString(value.value)
            is ContextArray -> encoder.encodeSerializableValue(ContextArray.serializer(), value)
            is ContextNull -> encoder.encodeNull()
        }
    }

    override fun deserialize(decoder: Decoder): ContextNode {
        return decoder.decodeSerializableValue(ContextMap.serializer())
    }
}