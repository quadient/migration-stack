package com.quadient.migration.api.dto.migrationmodel.builder

import com.quadient.migration.api.dto.migrationmodel.DisplayRuleRef
import com.quadient.migration.api.dto.migrationmodel.DocumentContent
import com.quadient.migration.api.dto.migrationmodel.DocumentObject
import com.quadient.migration.api.dto.migrationmodel.DocumentObjectRef
import com.quadient.migration.api.dto.migrationmodel.ImageRef
import com.quadient.migration.api.dto.migrationmodel.VariableStructureRef
import com.quadient.migration.api.dto.migrationmodel.builder.documentcontent.AreaBuilder
import com.quadient.migration.api.dto.migrationmodel.builder.documentcontent.SelectByLanguageBuilder
import com.quadient.migration.shared.DocumentObjectOptions
import com.quadient.migration.shared.DocumentObjectType
import com.quadient.migration.shared.MetadataPrimitive

class DocumentObjectBuilder(id: String, private val type: DocumentObjectType) :
    DtoBuilderBase<DocumentObject, DocumentObjectBuilder>(id) {
    private var content: List<DocumentContent> = mutableListOf()
    private var internal: Boolean = false
    private var targetFolder: String? = null
    private var displayRuleRef: DisplayRuleRef? = null
    private var variableStructureRef: VariableStructureRef? = null
    private var baseTemplate: String? = null
    private var options: DocumentObjectOptions? = null
    private var metadata: MutableMap<String, List<MetadataPrimitive>> = mutableMapOf()

    /**
     * Replace content of the document object.
     * @param content List of [DocumentContent] to set as the content of the document object.
     * @return This builder instance for method chaining.
     */
    fun content(content: List<DocumentContent>) = apply { this.content = content }

    /**
     * Set whether the document object is internal. Internal objects do not create a separate
     * file in the target system.
     * @param internal Boolean indicating if the document object is internal.
     * @return This builder instance for method chaining.
     */
    fun internal(internal: Boolean) = apply { this.internal = internal }

    /**
     * Set the target folder for the document object.
     * @param targetFolder String representing the target folder path.
     * @return This builder instance for method chaining.
     */
    fun targetFolder(targetFolder: String) = apply { this.targetFolder = targetFolder }
    /**
     * Add display rule to this document object.
     * @param id ID of the display rule to reference.
     * @return This builder instance for method chaining.
     */
    fun displayRuleRef(id: String) = apply { this.displayRuleRef = DisplayRuleRef(id) }
    /**
     * Add display rule to this document object.
     * @param ref Reference to the display rule.
     * @return This builder instance for method chaining.
     */
    fun displayRuleRef(ref: DisplayRuleRef) = apply { this.displayRuleRef = ref }
    /**
     * Add a reference to a variable structure to this document object.
     * @param id ID of the variable structure to reference.
     * @return This builder instance for method chaining.
     */
    fun variableStructureRef(id: String) = apply { this.variableStructureRef = VariableStructureRef(id) }
    /**
     * Override the default base template for this document object.
     * @param baseTemplate Path to the base template to use for this document object.
     * @return This builder instance for method chaining.
     */
    fun baseTemplate(baseTemplate: String) = apply { this.baseTemplate = baseTemplate }

    /**
     * Set options for the document object.
     * @param options [DocumentObjectOptions] to set for the document object.
     * @return This builder instance for method chaining.
     */
    fun options(options: DocumentObjectOptions) = apply { this.options = options }

    /**
     * Add an area to the document object.
     * @param builder Builder function where receiver is a [AreaBuilder].
     * @return This builder instance for method chaining.
     */
    fun area(builder: AreaBuilder.() -> Unit) = apply {
        val areaBuilder = AreaBuilder().apply(builder)
        content = content + areaBuilder.build()
    }

    /**
     * Add a paragraph to the document object.
     * @param builder Builder function where receiver is a [ParagraphBuilder].
     * @return This builder instance for method chaining.
     */
    fun paragraph(builder: ParagraphBuilder.() -> Unit) = apply {
        val paragraphBuilder = ParagraphBuilder().apply(builder)
        content = content + paragraphBuilder.build()
    }

    /**
     * Add a table to the document object.
     * @param builder Builder function where receiver is a [TableBuilder].
     * @return This builder instance for method chaining.
     */
    fun table(builder: TableBuilder.() -> Unit) = apply {
        val tableBuilder = TableBuilder().apply(builder)
        content = content + tableBuilder.build()
    }

    /**
     * Add a first match block to the document object
     * @param builder Builder function where receiver is a [FirstMatchBuilder].
     * @return This builder instance for method chaining.
     */
    fun firstMatch(builder: FirstMatchBuilder.() -> Unit) = apply {
        val firstMatchBuilder = FirstMatchBuilder().apply (builder)
        content = content + firstMatchBuilder.build()
    }

    /**
     * Add a select by language block to the document object
     * @param builder Builder function where receiver is a [SelectByLanguageBuilder].
     * @return This builder instance for method chaining.
     */
    fun selectByLanguage(builder: SelectByLanguageBuilder.() -> Unit) = apply {
        val selectByLanguageBuilder = SelectByLanguageBuilder().apply (builder)
        content = content + selectByLanguageBuilder.build()
    }

    /**
     * Add a reference to an image to the document object
     * @param imageRef ID of the image to reference.
     * @return This builder instance for method chaining.
     */
    fun imageRef(imageRef: String) = apply {
        imageRef(ImageRef(imageRef))
    }

    /**
     * Add a reference to an image to the document object
     * @param ref Reference to the image.
     * @return This builder instance for method chaining.
     */
    fun imageRef(ref: ImageRef) = apply {
        content = content + ref
    }

    /**
     * Add a reference to another document object to the document object.
     * @param documentObjectId ID of the document object to reference.
     * @return This builder instance for method chaining.
     */
    fun documentObjectRef(documentObjectId: String) = apply {
        documentObjectRef(DocumentObjectRef(documentObjectId))
    }

    /**
     * Add a reference to another document object to the document object.
     * @param documentObjectId ID of the document object to reference.
     * @param displayRuleId ID of the display rule to reference.
     * @return This builder instance for method chaining.
     */
    fun documentObjectRef(documentObjectId: String, displayRuleId: String) = apply {
        documentObjectRef(DocumentObjectRef(documentObjectId, DisplayRuleRef(displayRuleId)))
    }

    /**
     * Add a reference to another document object to the document object.
     * @param ref Reference to the document object.
     * @return This builder instance for method chaining.
     */
    fun documentObjectRef(ref: DocumentObjectRef) = apply {
        content = content + ref
    }

    /**
     * Add metadata to the document object.
     * Metadata are not stored if empty.
     * @param key Key of the metadata entry.
     * @param block Builder function where receiver is a [MetadataBuilder].
     * @return This builder instance for method chaining.
     */
    fun metadata(key: String, block: MetadataBuilder.() -> Unit) = apply {
        val result = MetadataBuilder().apply(block).build()
        if (result.isNotEmpty()) {
            metadata[key] = result
        }
    }

    override fun build(): DocumentObject {
        return DocumentObject(
            id = id,
            type = type,
            name = name,
            content = content,
            internal = internal,
            targetFolder = targetFolder,
            originLocations = originLocations,
            customFields = customFields,
            displayRuleRef = displayRuleRef,
            baseTemplate = baseTemplate,
            options = options,
            metadata = metadata,
        )
    }
}