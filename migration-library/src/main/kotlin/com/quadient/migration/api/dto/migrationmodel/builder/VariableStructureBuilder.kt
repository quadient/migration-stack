package com.quadient.migration.api.dto.migrationmodel.builder

import com.quadient.migration.api.dto.migrationmodel.VariableStructure
import com.quadient.migration.shared.VariablePathAndName

class VariableStructureBuilder(id: String) : DtoBuilderBase<VariableStructure, VariableStructureBuilder>(id) {
    var structure = mutableMapOf<String, VariablePathAndName>()

    /**
     * Adds a variable to the structure.
     *
     * @param id The unique identifier for the variable (used as the map key).
     * @param path The path of the variable.
     * @return This builder instance for method chaining.
     */
    fun addVariable(id: String, path: String) = apply {
        structure[id] = VariablePathAndName(null, path)
        return this
    }

    /**
     * Adds a variable to the structure.
     *
     * @param id The unique identifier for the variable (used as the map key).
     * @param path The path of the variable.
     * @param name A name to override the variable's default name.
     * @return This builder instance for method chaining.
     */
    fun addVariable(id: String, path: String, name: String) = apply {
        structure[id] = VariablePathAndName(name, path)
        return this
    }

    /**
     * Adds a variable to the structure using a [VariablePathAndName] instance.
     *
     * @param id The unique identifier for the variable (used as the map key).
     * @param variablePathAndName The [VariablePathAndName] instance representing the variable data.
     * @return This builder instance for method chaining.
     */
    fun addVariable(id: String, variablePathAndName: VariablePathAndName) = apply {
        structure[id] = variablePathAndName
        return this
    }

    /**
     * Replaces the entire variable structure with the provided map.
     *
     * @param structure A map where keys are variable IDs and values are [VariablePathAndName] instances.
     * @return This builder instance for method chaining.
     */
    fun structure(structure: Map<String, VariablePathAndName>) = apply {
        this.structure = structure.toMutableMap()
        return this
    }

    /**
     * Builds and returns a [VariableStructure] instance based on the current state of the builder.
     *
     * @return The constructed [VariableStructure] object.
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