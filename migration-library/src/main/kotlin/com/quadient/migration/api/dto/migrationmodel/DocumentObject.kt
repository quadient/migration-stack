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
    override val created: Instant,
    override val lastUpdated: Instant,
    var type: DocumentObjectType,
    var content: List<DocumentContent> = mutableListOf(),
    var internal: Boolean? = false,
    var targetFolder: String? = null,
    var displayRuleRef: DisplayRuleRef? = null,
    var variableStructureRef: VariableStructureRef? = null,
    var baseTemplate: String? = null,
    var options: DocumentObjectOptions? = null,
    val metadata: Map<String, List<MetadataPrimitive>>,
    val skip: SkipOptions,
    val subject: String?,
) : MigrationObject, RefValidatable {
    override fun collectRefs(): List<Ref> {
        // Refs are collected from content via proper traversal
        return listOfNotNull(displayRuleRef, variableStructureRef)
    }
}