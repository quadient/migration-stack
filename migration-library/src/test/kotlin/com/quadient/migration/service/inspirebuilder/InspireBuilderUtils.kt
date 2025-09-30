package com.quadient.migration.service.inspirebuilder

import com.quadient.migration.tools.shouldBeEqualTo
import org.junit.jupiter.api.Test

class InspireBuilderUtils {
    @Test
    fun `sanitizeVariablePart basic input`() {
        val result = sanitizeVariablePart("Name-1")

        result.shouldBeEqualTo("Name_1")
    }

    @Test
    fun `sanitizeVariablePart sanitizes more complex input correctly`() {
        val result = sanitizeVariablePart("My  Special Name-1(Joe).:?!")

        result.shouldBeEqualTo("My__Special_Name_1_Joe_____")
    }
}