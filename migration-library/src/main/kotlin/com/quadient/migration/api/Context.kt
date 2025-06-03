package com.quadient.migration.api

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = ContextNodeSerializer::class)
sealed interface ContextNode

@Serializable(with = ContextMapSerializer::class)
class ContextMap(map: Map<String, Any>) : ContextNode, HashMap<String, Any>(map)

object ContextMapSerializer: KSerializer<ContextMap> {
    val surrogate: KSerializer<Map<String, ContextNode>>
        = MapSerializer(String.serializer(), ContextNode.serializer())
    override val descriptor: SerialDescriptor = surrogate.descriptor

    override fun serialize(encoder: Encoder, value: ContextMap) {}

    override fun deserialize(decoder: Decoder): ContextMap {
        return ContextMap(decoder.decodeSerializableValue(surrogate))
    }
}

object ContextNodeSerializer : KSerializer<ContextNode> {
    override val descriptor = PrimitiveSerialDescriptor("com.quadient.migration.api.ContextNodeSerializer", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: ContextNode) { }
    override fun deserialize(decoder: Decoder): ContextNode {
        return decoder.decodeSerializableValue(ContextMap.serializer())
    }
}