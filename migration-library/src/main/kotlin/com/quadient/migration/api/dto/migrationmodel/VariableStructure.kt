package com.quadient.migration.api.dto.migrationmodel

import com.quadient.migration.data.VariableStructureModel
import com.quadient.migration.shared.VariablePathData

data class VariableStructure(
    override val id: String,
    override var name: String? = null,
    override var originLocations: List<String> = emptyList(),
    override var customFields: CustomFieldMap,
    val structure: Map<String, VariablePathData>,
    val languageVariable: VariableRef?,
) : MigrationObject {

    companion object {
        fun fromModel(model: VariableStructureModel) = VariableStructure(
            id = model.id,
            name = model.name,
            originLocations = model.originLocations,
            customFields = CustomFieldMap(model.customFields.toMutableMap()),
            structure = model.structure.map { (key, value) -> key.id to value }.toMap(),
            languageVariable = model.languageVariable?.let { VariableRef.fromModel(it) }
        )
    }
}