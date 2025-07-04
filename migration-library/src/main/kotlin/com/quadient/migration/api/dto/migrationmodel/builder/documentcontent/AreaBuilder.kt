package com.quadient.migration.api.dto.migrationmodel.builder.documentcontent

import com.quadient.migration.api.dto.migrationmodel.DisplayRuleRef
import com.quadient.migration.api.dto.migrationmodel.DocumentContent
import com.quadient.migration.api.dto.migrationmodel.DocumentObjectRef
import com.quadient.migration.api.dto.migrationmodel.Area
import com.quadient.migration.api.dto.migrationmodel.ImageRef
import com.quadient.migration.api.dto.migrationmodel.builder.ParagraphBuilder
import com.quadient.migration.api.dto.migrationmodel.builder.PositionBuilder
import com.quadient.migration.shared.Position

class AreaBuilder {
    private var content = mutableListOf<DocumentContent>()
    private var position: Position? = null

    /**
     * Appends the content of the flow area.
     * @param content A list of [DocumentContent] to be appended to content of the flow area.
     * @return The [AreaBuilder] instance for method chaining.
     */
    fun content(content: List<DocumentContent>) = apply { this.content.addAll(content) }

    /**
     * Adds a single [DocumentContent] to the flow area.
     * @param content The [DocumentContent] to be added.
     * @return The [AreaBuilder] instance for method chaining.
     */
    fun content(content: DocumentContent) = apply { this.content.add(content) }

    /**
     * Sets the position of the flow area.
     * @param position The [Position] to be set for the flow area.
     * @return The [AreaBuilder] instance for method chaining.
     */
    fun position(position: Position) = apply { this.position = position }

    /**
     * Set the position of the flow area using a builder function.
     * @param block a builder function to build the [Position].
     * @return The constructed [Area] instance.
     * @throws RuntimeException if position is not set.
     * @return The [AreaBuilder] instance for method chaining.
     */
    fun position(block: PositionBuilder.() -> Unit) = apply {
        val position = PositionBuilder().apply(block).build()
        this.position = position
    }

    /**
     * Adds a reference to a document object by its ID to the content.
     * @param documentObjectId The ID of the document object.
     * @return The [AreaBuilder] instance for method chaining.
     */
    fun documentObjectRef(documentObjectId: String) = apply {
        content.add(DocumentObjectRef(documentObjectId, null))
    }

    /**
     * Adds a conditional reference to a document object by its ID and display rule ID to the content.
     * @param documentObjectId The ID of the document object.
     * @param displayRuleId The ID of the display rule.
     * @return The [AreaBuilder] instance for method chaining.
     */
    fun documentObjectRef(documentObjectId: String, displayRuleId: String) = apply {
        content.add(DocumentObjectRef(documentObjectId, DisplayRuleRef(displayRuleId)))
    }

    /**
     * Adds a paragraph to the flow area using a builder function.
     * @param builder A builder function to build the paragraph.
     * @return The [AreaBuilder] instance for method chaining.
     */
    fun paragraph(builder: ParagraphBuilder.() -> Unit) = apply {
        content.add(ParagraphBuilder().apply(builder).build())
    }

    /**
     * Adds a reference to an image by its ID to the content.
     * @param imageRefId The ID of the image reference.
     * @return The [AreaBuilder] instance for method chaining.
     */
    fun imageRef(imageRefId: String) = apply {
        content.add(ImageRef(imageRefId))
    }

    /**
     * Builds the [Area] instance.
     * @throws RuntimeException if position is not set.
     * @return The constructed [Area] instance.
     */
    fun build(): Area {
        return Area(
            content = content,
            position = position ?: throw RuntimeException("Position is required in flow area")
        )
    }
}