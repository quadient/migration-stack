package com.quadient.migration.api.dto.migrationmodel

import com.quadient.migration.data.VariableStructureModel

data class VariableStructure(
    override val id: String,
    override var name: String? = null,
    override var originLocations: List<String> = emptyList(),
    override var customFields: CustomFieldMap,
    val structure: Map<String, String>
) : MigrationObject {

    companion object {
        fun fromModel(model: VariableStructureModel) = VariableStructure(
            id = model.id,
            name = model.name,
            originLocations = model.originLocations,
            customFields = CustomFieldMap(model.customFields.toMutableMap()),
            structure = model.structure.map { (key, value) -> key.id to value.value }.toMap(),
        )
    }
}