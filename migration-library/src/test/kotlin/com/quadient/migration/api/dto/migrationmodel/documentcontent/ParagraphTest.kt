package com.quadient.migration.api.dto.migrationmodel.documentcontent

import com.quadient.migration.api.dto.migrationmodel.DocumentContent
import com.quadient.migration.api.dto.migrationmodel.VariableRef
import com.quadient.migration.api.dto.migrationmodel.builder.ParagraphBuilder
import com.quadient.migration.tools.shouldBeEqualTo
import org.junit.jupiter.api.Test

class ParagraphTest {

    @Test
    fun `toPreview concatenates strings and variable refs`() {
        val paragraph = ParagraphBuilder()
            .string("Dear ")
            .variableRef("firstName")
            .string(" ")
            .variableRef("lastName")
            .build()

        paragraph.toPreview().shouldBeEqualTo($$"par: Dear $firstName$ $lastName$")
    }

    @Test
    fun `toPreview resolves variable names via nameResolver`() {
        val nameResolver: (DocumentContent) -> String? = { content ->
            when (content) {
                is VariableRef if content.id == "firstName" -> "First Name"
                is VariableRef if content.id == "lastName" -> "Last Name"
                else -> null
            }
        }
        val paragraph = ParagraphBuilder()
            .string("Dear ")
            .variableRef("firstName")
            .string(" ")
            .variableRef("lastName")
            .build()

        paragraph.toPreview(nameResolver).shouldBeEqualTo($$"par: Dear $First Name$ $Last Name$")
    }

    @Test
    fun `toPreview truncates long concatenated content`() {
        val paragraph = ParagraphBuilder()
            .string("This is a very long string that exceeds the preview limit and should be truncated at some point")
            .build()

        paragraph.toPreview().shouldBeEqualTo("par: This is a very long string tha")
    }
}
