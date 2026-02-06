package com.quadient.migration.api.dto.migrationmodel.builder

import com.quadient.migration.api.dto.migrationmodel.DisplayRuleRef
import com.quadient.migration.api.dto.migrationmodel.DocumentContent
import com.quadient.migration.api.dto.migrationmodel.DocumentObject
import com.quadient.migration.api.dto.migrationmodel.VariableStructureRef
import com.quadient.migration.api.dto.migrationmodel.builder.documentcontent.AreaBuilder
import com.quadient.migration.shared.DocumentObjectOptions
import com.quadient.migration.shared.DocumentObjectType
import com.quadient.migration.shared.MetadataPrimitive
import com.quadient.migration.shared.SkipOptions

class DocumentObjectBuilder(id: String, private val type: DocumentObjectType) :
    DtoBuilderBase<DocumentObject, DocumentObjectBuilder>(id), DocumentContentBuilderBase<DocumentObjectBuilder> {
    override val content: MutableList<DocumentContent> = mutableListOf()
    private var internal: Boolean = false
    private var targetFolder: String? = null
    private var displayRuleRef: DisplayRuleRef? = null
    private var variableStructureRef: VariableStructureRef? = null
    private var baseTemplate: String? = null
    private var options: DocumentObjectOptions? = null
    private var metadata: MutableMap<String, List<MetadataPrimitive>> = mutableMapOf()
    private var skip = false
    private var placeholder: String? = null
    private var reason: String? = null
    private var subject: String? = null

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
     * Add an area to the document object (only for Page-type documents).
     * @param builder Builder function where receiver is a [AreaBuilder].
     * @return This builder instance for method chaining.
     */
    fun area(builder: AreaBuilder.() -> Unit) = apply {
        content.add(AreaBuilder().apply(builder).build())
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

    fun skip(placeholder: String? = null, reason: String? = null) = apply {
        this.skip = true
        this.placeholder = placeholder
        this.reason = reason
    }

    /**
     * Sets the subject of the document object. This is visible as description in Interactive
     * @param subject the subject of the document object
     * @return the builder instance for chaining
     */
    fun subject(subject: String) = apply {
        this.subject = subject
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
            created = null,
            lastUpdated = null,
            displayRuleRef = displayRuleRef,
            variableStructureRef = variableStructureRef,
            baseTemplate = baseTemplate,
            options = options,
            metadata = metadata,
            skip = SkipOptions(skipped = skip, reason = reason, placeholder = placeholder),
            subject = subject,
        )
    }
}