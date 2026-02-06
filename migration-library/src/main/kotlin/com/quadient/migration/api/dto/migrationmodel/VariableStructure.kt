package com.quadient.migration.api.dto.migrationmodel

import com.quadient.migration.shared.VariablePathData
import kotlinx.datetime.Instant

data class VariableStructure @JvmOverloads constructor(
    override val id: String,
    override var name: String? = null,
    override var originLocations: List<String> = emptyList(),
    override var customFields: CustomFieldMap,
    val structure: Map<String, VariablePathData>,
    val languageVariable: VariableRef?,
    override var created: Instant? = null,
    override var lastUpdated: Instant? = null,
) : MigrationObject, RefValidatable {
    override fun collectRefs(): List<Ref> {
        return structure.keys.map { VariableRef(it) }
    }
}