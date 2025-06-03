package com.quadient.migration.data

import com.quadient.migration.service.RefValidatable
import kotlinx.datetime.Instant

data class VariableStructureModel(
    override val id: String,
    override val name: String? = null,
    override val originLocations: List<String> = emptyList(),
    override val customFields: Map<String, String>,
    override val created: Instant,
    val lastUpdated: Instant,
    val structure: Map<VariableModelRef, VariablePath>
) : RefValidatable, MigrationObjectModel {
    override fun collectRefs(): List<RefModel> {
        return structure.keys.map { it }
    }
}

@JvmInline
value class VariablePath(val value: String)