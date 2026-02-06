package com.quadient.migration.api.dto.migrationmodel

import com.quadient.migration.shared.DocumentObjectOptions
import com.quadient.migration.shared.DocumentObjectType
import com.quadient.migration.shared.MetadataPrimitive
import com.quadient.migration.shared.SkipOptions
import kotlinx.datetime.Instant

data class DocumentObject(
    override val id: String,
    override var name: String? = null,
    override var originLocations: List<String> = emptyList(),
    override var customFields: CustomFieldMap,
    var type: DocumentObjectType,
    var content: List<DocumentContent> = mutableListOf(),
    var internal: Boolean? = false,
    var targetFolder: String? = null,
    var displayRuleRef: DisplayRuleRef? = null,
    var variableStructureRef: VariableStructureRef? = null,
    var baseTemplate: String? = null,
    var options: DocumentObjectOptions? = null,
    override val created: Instant? = null,
    override val lastUpdated: Instant? = null,
    val metadata: Map<String, List<MetadataPrimitive>>,
    val skip: SkipOptions,
    val subject: String?,
) : MigrationObject, RefValidatable {
    override fun collectRefs(): List<Ref> {
        val contentRefs = content.flatMap {
            when (it) {
                is RefValidatable -> it.collectRefs()
                else -> emptyList()
            }
        }
        return contentRefs + listOfNotNull(displayRuleRef, variableStructureRef)
    }
}