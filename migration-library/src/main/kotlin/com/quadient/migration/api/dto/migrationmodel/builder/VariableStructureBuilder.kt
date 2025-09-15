package com.quadient.migration.api.dto.migrationmodel.builder

import com.quadient.migration.api.dto.migrationmodel.VariableStructure
import com.quadient.migration.shared.VariablePathAndName

class VariableStructureBuilder(id: String) : DtoBuilderBase<VariableStructure, VariableStructureBuilder>(id) {
    var structure = mutableMapOf<String, VariablePathAndName>()

    /**
     * Adds a variable to the structure with the given name and path.
     * @param name The name of the variable.
     * @param path The path of the variable.
     * @return The builder instance for chaining.
     */
    fun addVariable(name: String, path: String) = apply {
        structure[name] = path
        return this
    }

    /**
     * Replaces the whole variable structure with the provided map.
     * @param structure A map where keys are variable names and values are their paths.
     * @return The builder instance for chaining.
     */
    fun structure(structure: Map<String, String>) = apply {
        this.structure = structure.toMutableMap()
        return this
    }

    /**
     * Sets the name of the variable structure.
     * @param name The name to use.
     * @return The builder instance for chaining.
     */
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