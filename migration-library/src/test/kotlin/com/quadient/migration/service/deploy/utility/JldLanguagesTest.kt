package com.quadient.migration.service.deploy.utility

import com.quadient.migration.tools.shouldBeEqualTo
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class JldLanguagesTest {
    private fun registeredLanguages(jld: String) =
        Json.parseToJsonElement(jld).jsonObject["InteractivePlusJsonDefinition"]!!.jsonObject["CustomProperties"]!!
            .jsonObject["Languages"]!!.jsonArray.map { it.jsonPrimitive.content }

    @Test
    fun `appends missing languages after the detected ones`() {
        val jld = """{"InteractivePlusJsonDefinition":{"Type":"Template","Nodes":[],"CustomProperties":{"Languages":["en_us"]}}}"""

        val result = JldLanguages.addLanguages(jld, listOf("en_us", "de", "es"))

        registeredLanguages(result).shouldBeEqualTo(listOf("en_us", "de", "es"))
    }

    @Test
    fun `creates custom properties when they are missing and keeps other content intact`() {
        val jld = """{"InteractivePlusJsonDefinition":{"Type":"Template","Nodes":[{"Width":0.00338666666666667,"Script":"a < b"}]}}"""

        val result = JldLanguages.addLanguages(jld, listOf("en_us", "es"))

        result.shouldBeEqualTo(
            """{"InteractivePlusJsonDefinition":{"Type":"Template","Nodes":[{"Width":0.00338666666666667,"Script":"a < b"}],"CustomProperties":{"Languages":["en_us","es"]}}}"""
        )
    }

    @Test
    fun `keeps other custom properties`() {
        val jld = """{"InteractivePlusJsonDefinition":{"CustomProperties":{"Other":true,"Languages":["en_us"]}}}"""

        val result = JldLanguages.addLanguages(jld, listOf("es"))

        result.shouldBeEqualTo("""{"InteractivePlusJsonDefinition":{"CustomProperties":{"Other":true,"Languages":["en_us","es"]}}}""")
    }

    @Test
    fun `returns original jld when all languages are already registered`() {
        val jld = """{ "InteractivePlusJsonDefinition": { "CustomProperties": { "Languages": ["en_us", "es"] } } }"""

        JldLanguages.addLanguages(jld, listOf("es", "en_us")).shouldBeEqualTo(jld)
    }

    @Test
    fun `handles byte order mark`() {
        val jld = "﻿" + """{"InteractivePlusJsonDefinition":{}}"""

        registeredLanguages(JldLanguages.addLanguages(jld, listOf("es"))).shouldBeEqualTo(listOf("es"))
    }

    @Test
    fun `fails when the jld has no interactive definition`() {
        assertThrows<IllegalArgumentException> { JldLanguages.addLanguages("""{"Other":{}}""", listOf("es")) }
    }
}
