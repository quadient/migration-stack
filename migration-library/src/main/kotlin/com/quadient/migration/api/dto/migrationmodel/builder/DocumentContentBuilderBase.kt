package com.quadient.migration.api.dto.migrationmodel.builder

import com.quadient.migration.api.dto.migrationmodel.DisplayRuleRef
import com.quadient.migration.api.dto.migrationmodel.DocumentContent
import com.quadient.migration.api.dto.migrationmodel.DocumentObjectRef
import com.quadient.migration.api.dto.migrationmodel.ImageRef
import com.quadient.migration.api.dto.migrationmodel.builder.documentcontent.SelectByLanguageBuilder

/**
 * Base interface for builders that contain a list of DocumentContent.
 * Provides standard methods for adding various types of document content.
 */
@Suppress("UNCHECKED_CAST")
interface DocumentContentBuilderBase<T> {
    /**
     * The mutable list of document content.
     * Implementing classes must provide this property.
     */
    val content: MutableList<DocumentContent>

    /**
     * Replaces all content with a single DocumentContent item.
     * @param content The content to set.
     * @return This builder instance for method chaining.
     */
    fun content(content: DocumentContent): T = apply {
        this.content.clear()
        this.content.add(content)
    } as T

    /**
     * Replaces all content with multiple DocumentContent items.
     * @param content The list of content to set.
     * @return This builder instance for method chaining.
     */
    fun content(content: List<DocumentContent>): T = apply {
        this.content.clear()
        this.content.addAll(content)
    } as T

    /**
     * Appends a DocumentContent item to the existing content.
     * @param content The content to append.
     * @return This builder instance for method chaining.
     */
    fun appendContent(content: DocumentContent): T = apply {
        this.content.add(content)
    } as T

    /**
     * Adds a paragraph to the content using a builder function.
     * @param builder A builder function to build the paragraph.
     * @return This builder instance for method chaining.
     */
    fun paragraph(builder: ParagraphBuilder.() -> Unit): T = apply {
        this.content.add(ParagraphBuilder().apply(builder).build())
    } as T

    /**
     * Adds a table to the content using a builder function.
     * @param builder A builder function to build the table.
     * @return This builder instance for method chaining.
     */
    fun table(builder: TableBuilder.() -> Unit): T = apply {
        this.content.add(TableBuilder().apply(builder).build())
    } as T

    /**
     * Adds an image reference to the content.
     * @param imageId The ID of the image to reference.
     * @return This builder instance for method chaining.
     */
    fun imageRef(imageId: String): T = apply {
        this.content.add(ImageRef(imageId))
    } as T

    /**
     * Adds a document object reference to the content.
     * @param documentObjectId The ID of the document object to reference.
     * @return This builder instance for method chaining.
     */
    fun documentObjectRef(documentObjectId: String): T = apply {
        this.content.add(DocumentObjectRef(documentObjectId, null))
    } as T

    /**
     * Adds a conditional document object reference to the content.
     * @param documentObjectId The ID of the document object to reference.
     * @param displayRuleId The ID of the display rule.
     * @return This builder instance for method chaining.
     */
    fun documentObjectRef(documentObjectId: String, displayRuleId: String): T = apply {
        this.content.add(DocumentObjectRef(documentObjectId, DisplayRuleRef(displayRuleId)))
    } as T

    /**
     * Adds a first match block to the content using a builder function.
     * @param builder A builder function to build the first match block.
     * @return This builder instance for method chaining.
     */
    fun firstMatch(builder: FirstMatchBuilder.() -> Unit): T = apply {
        this.content.add(FirstMatchBuilder().apply(builder).build())
    } as T

    /**
     * Adds a select by language block to the content using a builder function.
     * @param builder A builder function to build the select by language block.
     * @return This builder instance for method chaining.
     */
    fun selectByLanguage(builder: SelectByLanguageBuilder.() -> Unit): T = apply {
        this.content.add(SelectByLanguageBuilder().apply(builder).build())
    } as T
}