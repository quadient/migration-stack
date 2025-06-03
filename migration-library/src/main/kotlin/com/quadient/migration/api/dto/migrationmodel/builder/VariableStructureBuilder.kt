package com.quadient.migration.api.dto.migrationmodel.builder

import com.quadient.migration.api.dto.migrationmodel.VariableStructure

class VariableStructureBuilder(id: String) : DtoBuilderBase<VariableStructure, VariableStructureBuilder>(id) {
    var structure = mutableMapOf<String, String>()

    fun addVariable(name: String, path: String) = apply {
        structure[name] = path
        return this
    }

    fun structure(structure: Map<String, String>) = apply {
        this.structure = structure.toMutableMap()
        return this
    }

    override fun build(): VariableStructure {
        return VariableStructure(
            id = id,
            name = name,
            originLocations = originLocations,
            customFields = customFields,
            structure = structure,
        )
    }
}