package com.quadient.migration.api.dto.migrationmodel.builder

import com.quadient.migration.api.dto.migrationmodel.DisplayRuleRef
import com.quadient.migration.api.dto.migrationmodel.DocumentContent
import com.quadient.migration.api.dto.migrationmodel.DocumentObject
import com.quadient.migration.api.dto.migrationmodel.PdfMetadata
import com.quadient.migration.api.dto.migrationmodel.VariableStringContent
import com.quadient.migration.api.dto.migrationmodel.VariableStructureRef
import com.quadient.migration.api.dto.migrationmodel.builder.components.HasBaseTemplate
import com.quadient.migration.api.dto.migrationmodel.builder.components.HasDisplayRuleRef
import com.quadient.migration.api.dto.migrationmodel.builder.components.HasDocumentObjectOptions
import com.quadient.migration.api.dto.migrationmodel.builder.components.HasInternal
import com.quadient.migration.api.dto.migrationmodel.builder.components.HasMetadata
import com.quadient.migration.api.dto.migrationmodel.builder.components.HasPdfMetadata
import com.quadient.migration.api.dto.migrationmodel.builder.components.HasSkip
import com.quadient.migration.api.dto.migrationmodel.builder.components.HasSubject
import com.quadient.migration.api.dto.migrationmodel.builder.components.HasTargetFolder
import com.quadient.migration.api.dto.migrationmodel.builder.components.HasVariableStructureRef
import com.quadient.migration.shared.DocumentObjectOptions
import com.quadient.migration.shared.DocumentObjectType
import com.quadient.migration.shared.MetadataPrimitive
import com.quadient.migration.shared.SkipOptions

class SnippetBuilder(private val id: String) {
    /**
     * Adds a first match block to the content using a builder function.
     * @param builder A builder function to build the first match block.
     * @return A [FirstMatchSnippetBuilder] instance with the first match block applied.
     */
    fun firstMatch(builder: SimpleFirstMatchBuilder.() -> Unit) =
        FirstMatchSnippetBuilder(id).apply { firstMatch(builder) }

    /**
     * Creates a [SimpleSnippetBuilder] for building a snippet with simple string content.
     * @return A new [SimpleSnippetBuilder] instance.
     */
    fun simple() = SimpleSnippetBuilder(id)
}

class FirstMatchSnippetBuilder(id: String) : DtoBuilderBase<DocumentObject, FirstMatchSnippetBuilder>(id),
    HasVariableStructureRef<FirstMatchSnippetBuilder>,
    HasDisplayRuleRef<FirstMatchSnippetBuilder>,
    HasInternal<FirstMatchSnippetBuilder>,
    HasTargetFolder<FirstMatchSnippetBuilder>,
    HasBaseTemplate<FirstMatchSnippetBuilder>,
    HasSubject<FirstMatchSnippetBuilder>,
    HasDocumentObjectOptions<FirstMatchSnippetBuilder>,
    HasMetadata<FirstMatchSnippetBuilder>,
    HasSkip<FirstMatchSnippetBuilder>,
    HasPdfMetadata<FirstMatchSnippetBuilder>
{
    val content: MutableList<DocumentContent> = mutableListOf()
    override var variableStructureRef: VariableStructureRef? = null
    override var displayRuleRef: DisplayRuleRef? = null
    override var internal: Boolean = false
    override var targetFolder: String? = null
    override var baseTemplate: String? = null
    override var subject: String? = null
    override var options: DocumentObjectOptions? = null
    override var metadata: MutableMap<String, List<MetadataPrimitive>> = mutableMapOf()
    override var skip: Boolean = false
    override var placeholder: String? = null
    override var reason: String? = null
    override var pdfMetadata: PdfMetadata? = null

    /**
     * Adds a first match block to the content using a builder function.
     * @param builder A builder function to build the first match block.
     * @return This builder instance for method chaining.
     */
    fun firstMatch(builder: SimpleFirstMatchBuilder.() -> Unit): FirstMatchSnippetBuilder = apply {
        this.content.clear()
        this.content.add(SimpleFirstMatchBuilder().apply(builder).build())
    }

    /**
     * Builds the [DocumentObject] representing this snippet with all configured first-match content blocks.
     * @return The constructed [DocumentObject] of type [DocumentObjectType.Snippet].
     */
    override fun build(): DocumentObject {
        return DocumentObject(
            id = id,
            type = DocumentObjectType.Snippet,
            name = name,
            content = content,
            internal = internal,
            targetFolder = targetFolder,
            originLocations = originLocations,
            customFields = customFields,
            displayRuleRef = displayRuleRef,
            variableStructureRef = variableStructureRef,
            baseTemplate = baseTemplate,
            options = options,
            pdfMetadata = pdfMetadata,
            metadata = metadata,
            skip = SkipOptions(skipped = skip, reason = reason, placeholder = placeholder),
            subject = subject,
        )
    }
}

class SimpleSnippetBuilder(id: String) : DtoBuilderBase<DocumentObject, SimpleSnippetBuilder>(id),
    HasStringContent<VariableStringContent, SimpleSnippetBuilder>,
    HasVariableRefContent<VariableStringContent, SimpleSnippetBuilder>,
    HasVariableStructureRef<SimpleSnippetBuilder>,
    HasDisplayRuleRef<SimpleSnippetBuilder>,
    HasInternal<SimpleSnippetBuilder>,
    HasTargetFolder<SimpleSnippetBuilder>,
    HasBaseTemplate<SimpleSnippetBuilder>,
    HasSubject<SimpleSnippetBuilder>,
    HasDocumentObjectOptions<SimpleSnippetBuilder>,
    HasMetadata<SimpleSnippetBuilder>,
    HasSkip<SimpleSnippetBuilder>,
    HasPdfMetadata<SimpleSnippetBuilder>
{
    override val content: MutableList<VariableStringContent> = mutableListOf()
    override var variableStructureRef: VariableStructureRef? = null
    override var displayRuleRef: DisplayRuleRef? = null
    override var internal: Boolean = false
    override var targetFolder: String? = null
    override var baseTemplate: String? = null
    override var subject: String? = null
    override var options: DocumentObjectOptions? = null
    override var metadata: MutableMap<String, List<MetadataPrimitive>> = mutableMapOf()
    override var skip: Boolean = false
    override var placeholder: String? = null
    override var reason: String? = null
    override var pdfMetadata: PdfMetadata? = null

    /**
     * Builds the [DocumentObject] representing this snippet with all configured string content.
     * @return The constructed [DocumentObject] of type [DocumentObjectType.Snippet].
     */
    override fun build(): DocumentObject {
        return DocumentObject(
            id = id,
            type = DocumentObjectType.Snippet,
            name = name,
            content = content,
            internal = internal,
            targetFolder = targetFolder,
            originLocations = originLocations,
            customFields = customFields,
            displayRuleRef = displayRuleRef,
            variableStructureRef = variableStructureRef,
            baseTemplate = baseTemplate,
            options = options,
            pdfMetadata = pdfMetadata,
            metadata = metadata,
            skip = SkipOptions(skipped = skip, reason = reason, placeholder = placeholder),
            subject = subject,
        )
    }

}
