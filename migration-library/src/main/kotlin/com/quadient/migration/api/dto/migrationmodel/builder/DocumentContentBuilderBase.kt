package com.quadient.migration.api.dto.migrationmodel.builder

import com.quadient.migration.api.dto.migrationmodel.Attachment
import com.quadient.migration.api.dto.migrationmodel.DisplayRule
import com.quadient.migration.api.dto.migrationmodel.DisplayRuleRef
import com.quadient.migration.api.dto.migrationmodel.DocumentContent
import com.quadient.migration.api.dto.migrationmodel.DocumentObject
import com.quadient.migration.api.dto.migrationmodel.DocumentObjectRef
import com.quadient.migration.api.dto.migrationmodel.Image
import com.quadient.migration.api.dto.migrationmodel.ImageRef
import com.quadient.migration.api.dto.migrationmodel.AttachmentRef
import com.quadient.migration.api.dto.migrationmodel.Shape
import com.quadient.migration.api.dto.migrationmodel.StringValue
import com.quadient.migration.api.dto.migrationmodel.Variable
import com.quadient.migration.api.dto.migrationmodel.VariableRef
import com.quadient.migration.api.dto.migrationmodel.builder.documentcontent.ColumnLayoutBuilder
import com.quadient.migration.api.dto.migrationmodel.builder.documentcontent.ShapeBuilder
import com.quadient.migration.api.dto.migrationmodel.builder.documentcontent.RepeatedContentBuilder
import com.quadient.migration.api.dto.migrationmodel.builder.documentcontent.SelectByLanguageBuilder
import com.quadient.migration.shared.LiteralPath
import com.quadient.migration.shared.VariablePath
import com.quadient.migration.shared.VariableRefPath

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
     * Adds an image reference to the content.
     * @param image The [Image] object to reference.
     * @return This builder instance for method chaining.
     */
    fun imageRef(image: Image): T = apply {
        this.content.add(ImageRef(image.id))
    } as T

    /**
     * Adds an attachment reference to the content.
     * @param attachmentId The ID of the attachment to reference.
     * @return This builder instance for method chaining.
     */
    fun attachmentRef(attachmentId: String): T = apply {
        this.content.add(AttachmentRef(attachmentId))
    } as T

    /**
     * Adds an attachment reference to the content.
     * @param attachment The [Attachment] object to reference.
     * @return This builder instance for method chaining.
     */
    fun attachmentRef(attachment: Attachment): T = apply {
        this.content.add(AttachmentRef(attachment.id))
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
     * Adds a document object reference to the content.
     * @param documentObject The [DocumentObject] to reference.
     * @return This builder instance for method chaining.
     */
    fun documentObjectRef(documentObject: DocumentObject): T = apply {
        this.content.add(DocumentObjectRef(documentObject.id, null))
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
     * Adds a conditional document object reference to the content.
     * @param documentObject The [DocumentObject] to reference.
     * @param displayRule The [DisplayRule] to apply.
     * @return This builder instance for method chaining.
     */
    fun documentObjectRef(documentObject: DocumentObject, displayRule: DisplayRule): T = apply {
        this.content.add(DocumentObjectRef(documentObject.id, DisplayRuleRef(displayRule.id)))
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

    /**
     * Adds a paragraph with the given string to the content.
     * @param text The string to add in a paragraph.
     * @return This builder instance for method chaining.
     */
    fun string(text: String): T = apply {
        this.content.add(StringValue(text))
    } as T

    /**
     * Adds a variable to the content.
     * @param ref The [VariableRef] referencing the variable to add.
     * @return This builder instance for method chaining.
     */
    fun variableRef(ref: VariableRef): T = apply {
        this.content.add(ref)
    } as T

    /**
     * Adds a variable reference to the content by ID.
     * @param id The ID of the variable to reference.
     * @return This builder instance for method chaining.
     */
    fun variableRef(id: String): T = apply {
        this.content.add(VariableRef(id))
    } as T

    /**
     * Adds a variable reference to the content.
     * @param variable The [Variable] to reference.
     * @return This builder instance for method chaining.
     */
    fun variable(variable: Variable): T = apply {
        this.content.add(VariableRef(variable.id))
    } as T

    /**
     * Adds repeated content to the content using a builder function.
     * The content repeats once per element of the given array variable.
     * @param variablePath The [VariablePath] referencing the array variable to repeat over.
     * @param builder A builder function to build the repeated content.
     * @return This builder instance for method chaining.
     */
    fun repeatedContent(variablePath: VariablePath, builder: RepeatedContentBuilder.() -> Unit): T = apply {
        this.content.add(RepeatedContentBuilder(variablePath).apply(builder).build())
    } as T

    /**
     * Adds repeated content to the content using a literal path string.
     * @param literalPath The literal data path of the array variable to repeat over.
     * @param builder A builder function to build the repeated content.
     * @return This builder instance for method chaining.
     */
    fun repeatedContent(literalPath: String, builder: RepeatedContentBuilder.() -> Unit): T =
        repeatedContent(LiteralPath(literalPath), builder)

    /**
     * Adds repeated content to the content using a variable reference.
     * @param variableRef The [VariableRef] referencing the array variable to repeat over.
     * @param builder A builder function to build the repeated content.
     * @return This builder instance for method chaining.
     */
    fun repeatedContent(variableRef: VariableRef, builder: RepeatedContentBuilder.() -> Unit): T =
        repeatedContent(VariableRefPath(variableRef.id), builder)

    /**
     * Adds repeated content to the content using a [Variable] object.
     * @param variable The [Variable] referencing the array variable to repeat over.
     * @param builder A builder function to build the repeated content.
     * @return This builder instance for method chaining.
     */
    fun repeatedContent(variable: Variable, builder: RepeatedContentBuilder.() -> Unit): T =
        repeatedContent(VariableRefPath(variable.id), builder)

    /**
     * Defines a column layout modifier that affects sibling content within the current block.
     * @param builder A builder function to configure the [ColumnLayoutBuilder].
     * @return This builder instance for method chaining.
     */
    fun columnLayout(builder: ColumnLayoutBuilder.() -> Unit = {}): T = apply {
        this.content.add(ColumnLayoutBuilder().apply(builder).build())
    } as T

    /**
     * Adds an existing [Shape] to the content.
     * @param shape The [Shape] instance to append.
     * @return This builder instance for method chaining.
     */
    fun shape(shape: Shape): T = apply {
        this.content.add(shape)
    } as T

    /**
     * Adds a path object to the content using a builder function.
     * @param builder A builder function to construct the [Shape].
     * @return This builder instance for method chaining.
     */
    fun shape(builder: ShapeBuilder.() -> Unit): T = apply {
        this.content.add(ShapeBuilder().apply(builder).build())
    } as T
}