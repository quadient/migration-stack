package com.quadient.migration.api.dto.migrationmodel

import com.quadient.migration.shared.DocumentObjectType
import com.quadient.migration.shared.MetadataEntry
import com.quadient.migration.shared.SkipOptions
import kotlin.time.Instant

data class DocumentObject(
    override val id: String,
    override var name: String? = null,
    override var originLocations: List<String> = emptyList(),
    override var customFields: CustomFieldMap,
    var type: DocumentObjectType,
    var content: List<DocumentContent> = emptyList(),
    var internal: Boolean? = false,
    var targetFolder: String? = null,
    var displayRuleRef: DisplayRuleRef? = null,
    var variableStructureRef: VariableStructureRef? = null,
    var baseTemplate: BaseTemplateLocation? = null,
    var options: DocumentObjectOptions? = null,
    var pdfMetadata: PdfMetadata? = null,
    override var created: Instant? = null,
    override var lastUpdated: Instant? = null,
    val metadata: List<MetadataEntry> = emptyList(),
    val skip: SkipOptions,
    val subject: String?,
) : MigrationObject, RefValidatable {
    override fun collectRefs(): Set<Ref> {
        val contentRefs = content.flatMap {
            when (it) {
                is RefValidatable -> it.collectRefs()
                else -> emptySet()
            }
        }.toSet()

        val pdfMetadataRefs = pdfMetadata?.collectRefs().orEmpty()

        val baseTemplateRef = baseTemplate as? BaseTemplateRef

        return contentRefs + pdfMetadataRefs + setOfNotNull(displayRuleRef, variableStructureRef, baseTemplateRef)
    }
}
