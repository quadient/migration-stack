package com.quadient.migration.api.dto.migrationmodel.builder

import com.quadient.migration.api.dto.migrationmodel.builder.documentcontent.ColumnLayoutBuilder
import com.quadient.migration.api.dto.migrationmodel.DisplayRuleRef
import com.quadient.migration.api.dto.migrationmodel.Variable
import com.quadient.migration.api.dto.migrationmodel.DocumentObjectRef
import com.quadient.migration.api.dto.migrationmodel.Hyperlink
import com.quadient.migration.api.dto.migrationmodel.ImageRef
import com.quadient.migration.api.dto.migrationmodel.AttachmentRef
import com.quadient.migration.api.dto.migrationmodel.DocumentObject
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

    class TextBuilder : HasDisplayRuleRef<TextBuilder>, HasTextStyleRef<TextBuilder> {
        override var styleRef: TextStyleRef? = null
        override var displayRuleRef: DisplayRuleRef? = null
        var content: MutableList<TextContent> = mutableListOf()


        /**
         * Replaces all content with a single [TextContent] item.
         * @param content A [TextContent] instance to set as the content.
         * @return The current instance of [TextBuilder] for method chaining.
         */
        fun content(content: TextContent) = apply { this.content = mutableListOf(content) }

        /**
         * Replaces all content with a string.
         * @param content The string content to set.
         * @return The current instance of [TextBuilder] for method chaining.
         * @deprecated Use [string] for appending strings. This method replaces content which is inconsistent with other specific methods.
         */
        @Deprecated("Use string() to append string content", ReplaceWith("string(content)"))
        fun content(content: String) = apply { this.content = mutableListOf(StringValue(content)) }

        /**
         * Appends a [TextContent] to the existing content.
         * @param content The [TextContent] to append.
         * @return The current instance of [TextBuilder] for method chaining.
         */
        fun appendContent(content: TextContent) = apply { this.content.add(content) }

        /**
         * Replaces all content with a list of [TextContent].
         * @param content A list of [TextContent] to set as the content.
         * @return The current instance of [TextBuilder] for method chaining.
         */
        fun content(content: List<TextContent>) = apply { this.content = content.toMutableList() }

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
         * Adds a document object reference to the text content.
         * @param documentObjectId The ID of the document object to reference.
         * @return The current instance of [TextBuilder] for method chaining.
         */
        fun documentObjectRef(documentObjectId: String) = apply {
            content.add(DocumentObjectRef(documentObjectId, null))
        }

        /**
         * Adds a conditional document object reference with a display rule to the text content.
         * @param documentObjectId The ID of the document object to reference.
         * @param displayRuleId The ID of the display rule to apply.
         * @return The current instance of [TextBuilder] for method chaining.
         */
        fun documentObjectRef(documentObjectId: String, displayRuleId: String) = apply {
            content.add(DocumentObjectRef(documentObjectId, DisplayRuleRef(displayRuleId)))
        }

        /**
         * Adds a document object reference to the content.
         * @param documentObject The [DocumentObject] to reference.
         * @return This builder instance for method chaining.
         */
        fun documentObjectRef(documentObject: DocumentObject) = apply {
            this.content.add(DocumentObjectRef(documentObject.id, null))
        }

        /**
         * Adds first match block to the text content.
         * @param builder A builder function to configure the [FirstMatchBuilder].
         * @return The current instance of [TextBuilder] for method chaining.
         */
        fun firstMatch(builder: FirstMatchBuilder.() -> Unit) = apply {
            val firstMatchBuilder = FirstMatchBuilder().apply(builder)
            content.add(firstMatchBuilder.build())
        }

        /**
         * Adds a variable reference to the text content.
         * @param variableId The ID of the variable to reference.
         * @return The current instance of [TextBuilder] for method chaining.
         */
        fun variableRef(variableId: String) = apply {
            content.add(VariableRef(variableId))
        }

        /**
         * Adds a variable reference to the text content.
         * @param ref The variable reference to add.
         * @return The current instance of [TextBuilder] for method chaining.
         */
        fun variableRef(ref: VariableRef) = apply {
            content.add(ref)
        }

        /**
         * Adds a variable reference to the text content.
         * @param variable The [Variable] object to reference.
         * @return The current instance of [TextBuilder] for method chaining.
         */
        fun variableRef(variable: Variable) = apply {
            content.add(VariableRef(variable.id))
        }

        /**
         * Adds an image reference to the text content.
         * @param imageId The ID of the image to reference.
         * @return The current instance of [TextBuilder] for method chaining.
         */
        fun imageRef(imageId: String) = apply {
            content.add(ImageRef(imageId))
        }

        /**
         * Adds an image reference to the text content.
         * @param ref The image reference to add.
         * @return The current instance of [TextBuilder] for method chaining.
         */
        fun imageRef(ref: ImageRef) = apply {
            content.add(ref)
        }

        /**
         * Adds an attachment reference to the text content.
         * @param attachmentId The ID of the attachment to reference.
         * @return The current instance of [TextBuilder] for method chaining.
         */
        fun attachmentRef(attachmentId: String) = apply {
            content.add(AttachmentRef(attachmentId))
        }

        /**
         * Adds an attachment reference to the text content.
         * @param ref The attachment reference to add.
         * @return The current instance of [TextBuilder] for method chaining.
         */
        fun attachmentRef(ref: AttachmentRef) = apply {
            content.add(ref)
        }

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

        /**
         * Adds a table to the text content.
         * @param builder A builder function to configure the [TableBuilder].
         * @return The current instance of [TextBuilder] for method chaining.
         */
        fun table(builder: TableBuilder.() -> Unit) = apply {
            content.add(TableBuilder().apply(builder).build())
        }

        /**
         * Defines a column layout modifier that affects sibling content within the current paragraph.
         * @param builder A builder function to configure the [ColumnLayoutBuilder].
         * @return The current instance of [TextBuilder] for method chaining.
         */
        fun columnLayout(builder: ColumnLayoutBuilder.() -> Unit = {}) = apply {
            content.add(ColumnLayoutBuilder().apply(builder).build())
        }
    }
}
