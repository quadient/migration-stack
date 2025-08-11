package com.quadient.migration.api.dto.migrationmodel

import com.quadient.migration.data.DocumentObjectModel
import com.quadient.migration.shared.DocumentObjectOptions
import com.quadient.migration.shared.DocumentObjectType
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
    var created: Instant? = null,
    var lastUpdated: Instant? = null,
) : MigrationObject {
    companion object {
        fun fromModel(model: DocumentObjectModel): DocumentObject {
            return DocumentObject(
                id = model.id,
                name = model.name,
                originLocations = model.originLocations,
                customFields = CustomFieldMap(model.customFields.toMutableMap()),
                type = model.type,
                content = model.content.map { DocumentContent.fromModelContent(it) },
                internal = model.internal,
                targetFolder = model.targetFolder?.toString(),
                displayRuleRef = model.displayRuleRef?.let(DisplayRuleRef::fromModel),
                variableStructureRef = model.variableStructureRef?.let(VariableStructureRef::fromModel),
                baseTemplate = model.baseTemplate,
                options = model.options,
                created = model.created,
                lastUpdated = model.lastUpdated,
            )
        }
    }
}