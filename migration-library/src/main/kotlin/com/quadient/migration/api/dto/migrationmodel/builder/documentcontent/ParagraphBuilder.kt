package com.quadient.migration.api.dto.migrationmodel.builder

import com.quadient.migration.api.dto.migrationmodel.DisplayRuleRef
import com.quadient.migration.api.dto.migrationmodel.Hyperlink
import com.quadient.migration.api.dto.migrationmodel.Paragraph
import com.quadient.migration.api.dto.migrationmodel.ParagraphStyle
import com.quadient.migration.api.dto.migrationmodel.ParagraphStyleRef
import com.quadient.migration.api.dto.migrationmodel.StringValue
import com.quadient.migration.api.dto.migrationmodel.TextContent
import com.quadient.migration.api.dto.migrationmodel.TextStyleRef
import com.quadient.migration.api.dto.migrationmodel.VariableRef
import com.quadient.migration.api.dto.migrationmodel.builder.components.HasDisplayRuleRef
import com.quadient.migration.api.dto.migrationmodel.builder.components.HasTextStyleRef

class ParagraphBuilder : HasDisplayRuleRef<ParagraphBuilder> {
    private var styleRef: ParagraphStyleRef? = null
    override var displayRuleRef: DisplayRuleRef? = null
    private var content: MutableList<TextBuilder> = mutableListOf()

    /**
     * Sets the style reference for the paragraph.
     * @param styleRef The style reference to set.
     * @return The current instance of [ParagraphBuilder] for method chaining.
     */
    fun styleRef(styleRef: ParagraphStyleRef) = apply { this.styleRef = styleRef }

    /**
     * Sets the style reference for the paragraph using a string ID.
     * @param styleRefId The ID of the style reference to set.
     * @return The current instance of [ParagraphBuilder] for method chaining.
     */
    fun styleRef(styleRefId: String) = apply { this.styleRef = ParagraphStyleRef(styleRefId) }

    /**
     * Sets the style reference for the paragraph using a [ParagraphStyle] model object.
     * @param style The paragraph style whose ID will be used as the reference.
     * @return The current instance of [ParagraphBuilder] for method chaining.
     */
    fun styleRef(style: ParagraphStyle) = apply { this.styleRef = ParagraphStyleRef(style.id) }

    /**
     * Adds a new text builder to the paragraph content.
     * @return A new instance of [TextBuilder] to build the text content.
     */
    fun addText(): TextBuilder {
        val result = TextBuilder()
        this.content.add(result)
        return result
    }

    /**
     * Adds a text block to the paragraph content using a builder function.
     * @param block A builder function to configure the [TextBuilder].
     * @return The current instance of [ParagraphBuilder] for method chaining.
     */
    fun text(block: TextBuilder.() -> Unit) = apply {
        val result = TextBuilder()
        block(result)
        this.content.add(result)
    }

    /**
     * Replaces the current content of the paragraph with a new list of text builders.
     * @param content A list of [TextBuilder] instances to set as the new content.
     * @return The current instance of [ParagraphBuilder] for method chaining.
     */
    fun content(content: List<TextBuilder>) = apply { this.content = content.toMutableList() }

    /**
     * Adds a string content to the paragraph.
     * @param content The string content to add.
     * @return The current instance of [ParagraphBuilder] for method chaining.
     * @deprecated Use [string] instead for consistency. This method uses confusing naming.
     */
    @Deprecated("Use string() instead", ReplaceWith("string(content)"))
    fun content(content: String) = apply {
        val textBuilder = TextBuilder()
        textBuilder.content(StringValue(content))
        this.content.add(textBuilder)
    }

    /**
     * Adds a string to the paragraph content (creates a text block with StringValue).
     * @param text The string to add.
     * @return The current instance of [ParagraphBuilder] for method chaining.
     */
    fun string(text: String) = apply {
        val textBuilder = TextBuilder()
        textBuilder.content(StringValue(text))
        this.content.add(textBuilder)
    }

    /**
     * Adds a variable reference to the paragraph content.
     * @param variableId The ID of the variable to reference.
     * @return The current instance of [ParagraphBuilder] for method chaining.
     */
    fun variableRef(variableId: String) = apply {
        val textBuilder = TextBuilder()
        textBuilder.content(VariableRef(variableId))
        this.content.add(textBuilder)
    }

    /**
     * Builds the [Paragraph] object with the current configuration.
     * @return A [Paragraph] instance containing the configured content,
     * style reference, and display rule reference.
     */
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

    class TextBuilder :
        HasDisplayRuleRef<TextBuilder>,
        HasTextStyleRef<TextBuilder>,
        HasBarcodeContent<TextContent, TextBuilder>,
        HasGenericContent<TextContent, TextBuilder>,
        HasDocumentObjectRefContent<TextContent, TextBuilder>,
        HasFirstMatchContent<TextContent, TextBuilder>,
        HasVariableRefContent<TextContent, TextBuilder>,
        HasImageRefContent<TextContent, TextBuilder>,
        HasAttachmentRefContent<TextContent, TextBuilder>,
        HasTableContent<TextContent, TextBuilder>,
        HasColumnLayoutContent<TextContent, TextBuilder>
    {
        override var styleRef: TextStyleRef? = null
        override var displayRuleRef: DisplayRuleRef? = null
        override var content: MutableList<TextContent> = mutableListOf()

        /**
         * Replaces all content with a string.
         * @param content The string content to set.
         * @return The current instance of [TextBuilder] for method chaining.
         * @deprecated Use [string] for appending strings. This method replaces content which is inconsistent with other specific methods.
         */
        @Deprecated("Use string() to append string content", ReplaceWith("string(content)"))
        fun content(content: String) = apply { this.content = mutableListOf(StringValue(content)) }

        /**
         * Appends a string to the existing content (creates a StringValue).
         * @param text The string to append.
         * @return The current instance of [TextBuilder] for method chaining.
         */
        fun string(text: String) = apply { this.content.add(StringValue(text)) }

        /**
         * Appends a string to the existing content.
         * @param content The string content to append.
         * @return The current instance of [TextBuilder] for method chaining.
         * @deprecated Use [string] instead for consistency.
         */
        @Deprecated("Use string() instead", ReplaceWith("string(content)"))
        fun appendContent(content: String) = apply { this.content.add(StringValue(content)) }

        /**
         * Adds an inline hyperlink to the text content.
         * @param url The URL to link to (mandatory).
         * @param displayText The text to display (optional - if null, url will be displayed).
         * @param alternateText The accessibility text for screen readers (optional).
         * @return The current instance of [TextBuilder] for method chaining.
         */
        fun hyperlink(url: String, displayText: String? = null, alternateText: String? = null) = apply {
            content.add(Hyperlink(url, displayText, alternateText))
        }

        /**
         * Adds a hyperlink to the text content.
         * @param hyperlink The hyperlink to add.
         * @return The current instance of [TextBuilder] for method chaining.
         */
        fun hyperlink(hyperlink: Hyperlink) = apply {
            content.add(hyperlink)
        }
    }
}
