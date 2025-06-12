package com.quadient.migration.api.dto.migrationmodel.builder

import com.quadient.migration.api.dto.migrationmodel.DisplayRuleRef
import com.quadient.migration.api.dto.migrationmodel.DocumentObjectRef
import com.quadient.migration.api.dto.migrationmodel.Paragraph
import com.quadient.migration.api.dto.migrationmodel.ParagraphStyleRef
import com.quadient.migration.api.dto.migrationmodel.StringValue
import com.quadient.migration.api.dto.migrationmodel.TextContent
import com.quadient.migration.api.dto.migrationmodel.TextStyleRef
import com.quadient.migration.api.dto.migrationmodel.VariableRef

class ParagraphBuilder {
    private var styleRef: ParagraphStyleRef? = null
    private var displayRuleRef: DisplayRuleRef? = null
    private var content: MutableList<TextBuilder> = mutableListOf()

    fun styleRef(styleRef: ParagraphStyleRef) = apply { this.styleRef = styleRef }
    fun styleRef(styleRefId: String) = apply { this.styleRef = ParagraphStyleRef(styleRefId) }
    fun displayRuleRef(displayRuleRef: DisplayRuleRef?) = apply { this.displayRuleRef = displayRuleRef }
    fun displayRuleRef(displayRuleRefId: String) = apply { this.displayRuleRef = DisplayRuleRef(displayRuleRefId) }
    fun addText(): TextBuilder {
        val result = TextBuilder()
        this.content.add(result)
        return result
    }

    fun text(block: TextBuilder.() -> Unit) = apply {
        val result = TextBuilder()
        block(result)
        this.content.add(result)
    }

    fun content(content: List<TextBuilder>) = apply { this.content = content.toMutableList() }
    fun content(content: String) = apply {
        val textBuilder = TextBuilder()
        textBuilder.content(StringValue(content))
        this.content.add(textBuilder)
    }
    fun variableRef(variableId: String) = apply {
        val textBuilder = TextBuilder()
        textBuilder.content(VariableRef(variableId))
        this.content.add(textBuilder)
    }

    fun build(): Paragraph {
        return Paragraph(
            content = content.map {
                Paragraph.Text(
                    content = it.content,
                    styleRef = it.styleRef,
                    displayRuleRef = it.displayRuleRef
                )
            },
            styleRef = styleRef,
            displayRuleRef = displayRuleRef,
        )
    }

    class TextBuilder() {
        var styleRef: TextStyleRef? = null
        var displayRuleRef: DisplayRuleRef? = null
        var content: MutableList<TextContent> = mutableListOf()

        fun styleRef(styleRef: TextStyleRef) = apply { this.styleRef = styleRef }
        fun styleRef(styleRefId: String) = apply { this.styleRef = TextStyleRef(styleRefId) }
        fun displayRuleRef(displayRuleRef: DisplayRuleRef) = apply { this.displayRuleRef = displayRuleRef }
        fun displayRuleRef(displayRuleRefId: String) = apply { this.displayRuleRef = DisplayRuleRef(displayRuleRefId) }
        fun content(content: TextContent) = apply { this.content = mutableListOf(content) }
        fun content(content: String) = apply { this.content = mutableListOf(StringValue(content)) }
        fun appendContent(content: TextContent) = apply { this.content.add(content) }
        fun appendContent(content: String) = apply { this.content.add(StringValue(content)) }
        fun content(content: List<TextContent>) = apply { this.content = content.toMutableList() }
        fun documentObjectRef(documentObjectId: String) = apply {
            content.add(DocumentObjectRef(documentObjectId, null))
        }
        fun documentObjectRef(documentObjectId: String, displayRuleId: String) = apply {
            content.add(DocumentObjectRef(documentObjectId, DisplayRuleRef(displayRuleId)))
        }

        fun firstMatch(builder: FirstMatchBuilder.() -> Unit) = apply {
            val firstMatchBuilder = FirstMatchBuilder().apply(builder)
            content.add(firstMatchBuilder.build())
        }
    }
}
