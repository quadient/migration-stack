package com.quadient.migration.shared

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

@Serializable(with = VariablePathSerializer::class)
sealed interface VariablePath

@Serializable
data class LiteralPath(val path: String) : VariablePath

@Serializable
data class VariableRefPath(val variableId: String) : VariablePath

object VariablePathSerializer : KSerializer<VariablePath> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("VariablePath")

    override fun deserialize(decoder: Decoder): VariablePath {
        val jsonDecoder = decoder as? JsonDecoder ?: error("VariablePath can only be deserialized from JSON")
        return when (val element = jsonDecoder.decodeJsonElement()) {
            is JsonPrimitive -> LiteralPath(element.content)
            is JsonObject -> when {
                "variableId" in element -> VariableRefPath(element["variableId"]!!.jsonPrimitive.content)
                "path" in element -> LiteralPath(element["path"]!!.jsonPrimitive.content)
                else -> error("Cannot deserialize VariablePath: expected 'path' or 'variableId' field")
            }

            else -> error("Cannot deserialize VariablePath from ${element::class.simpleName}")
        }
    }

    override fun serialize(encoder: Encoder, value: VariablePath) {
        val jsonEncoder = encoder as? JsonEncoder ?: error("VariablePath can only be serialized to JSON")
        val element = when (value) {
            is LiteralPath -> buildJsonObject { put("path", value.path) }
            is VariableRefPath -> buildJsonObject { put("variableId", value.variableId) }
        }
        jsonEncoder.encodeJsonElement(element)
    }
}
