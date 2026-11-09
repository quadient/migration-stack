package com.quadient.migration.service.deploy.utility

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Registers languages in an Interactive JLD (`InteractivePlusJsonDefinition.CustomProperties.Languages`).
 *
 * IPS detects the languages of a template or block only from its own select-by-language flows. Languages used solely
 * in referenced external document objects are therefore not registered, and Interactive hides those language variants.
 * The wfd-xml layout cannot declare template languages, so they are added to the extracted JLD instead.
 */
object JldLanguages {
    private const val DEFINITION = "InteractivePlusJsonDefinition"
    private const val CUSTOM_PROPERTIES = "CustomProperties"
    private const val LANGUAGES = "Languages"

    private val json = Json { prettyPrint = false }

    /**
     * Returns the JLD with [languages] added to the registered languages. Already registered languages keep their
     * position, missing ones are appended. When nothing is missing, the original JLD is returned untouched.
     */
    fun addLanguages(jld: String, languages: List<String>): String {
        val root = json.parseToJsonElement(jld.removePrefix("﻿")).jsonObject
        val definition = root[DEFINITION] as? JsonObject
            ?: throw IllegalArgumentException("JLD does not contain '$DEFINITION'.")
        val customProperties = definition[CUSTOM_PROPERTIES] as? JsonObject ?: JsonObject(emptyMap())

        val registered = (customProperties[LANGUAGES] as? JsonArray)?.map { it.jsonPrimitive.content } ?: emptyList()
        val merged = (registered + languages).distinct()
        if (merged == registered) return jld

        val updatedCustomProperties = JsonObject(customProperties + (LANGUAGES to JsonArray(merged.map(::JsonPrimitive))))
        val updatedDefinition = JsonObject(definition + (CUSTOM_PROPERTIES to updatedCustomProperties))
        return json.encodeToString(JsonObject.serializer(), JsonObject(root + (DEFINITION to updatedDefinition)))
    }
}
