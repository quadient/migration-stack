package com.quadient.migration.api.dto.migrationmodel

import com.quadient.migration.shared.DisplayRuleDefinition
import com.quadient.migration.shared.MetadataPrimitive
import kotlin.time.Instant

data class DisplayRule @JvmOverloads constructor(
    override val id: String,
    override var name: String?,
    override var originLocations: List<String> = emptyList(),
    override var customFields: CustomFieldMap,
    var definition: DisplayRuleDefinition?,
    override var created: Instant? = null,
    override var lastUpdated: Instant? = null,
    var targetId: DisplayRuleRef? = null,
    var internal: Boolean = true,
    val metadata: Map<String, List<MetadataPrimitive>> = emptyMap(),
    val subject: String? = null,
    val targetFolder: String? = null,
    val baseTemplate: String? = null,
    val variableStructureRef: VariableStructureRef? = null,
) : MigrationObject, RefValidatable {
    override fun collectRefs(): List<Ref> {
        return (definition?.collectRefs() ?: emptyList()) + listOfNotNull(variableStructureRef, targetId)
    }
}