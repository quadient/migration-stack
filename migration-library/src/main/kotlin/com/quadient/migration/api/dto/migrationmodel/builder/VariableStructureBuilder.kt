package com.quadient.migration.api.dto.migrationmodel.builder

import com.quadient.migration.api.dto.migrationmodel.VariableRef
import com.quadient.migration.api.dto.migrationmodel.VariableStructure
import com.quadient.migration.shared.VariablePathData

class VariableStructureBuilder(id: String) : DtoBuilderBase<VariableStructure, VariableStructureBuilder>(id) {
    var structure = mutableMapOf<String, VariablePathData>()
    var languageVariable: VariableRef? = null

    /**
     * Adds a variable to the structure.
     *
     * @param id The unique identifier for the variable (used as the map key).
     * @param path The path of the variable.
     * @return This builder instance for method chaining.
     */
    fun addVariable(id: String, path: String) = apply {
        structure[id] = VariablePathData(path, null)
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
        structure[id] = VariablePathData(path, name)
        return this
    }

    /**
     * Adds a variable to the structure using a [VariablePathData] instance.
     *
     * @param id The unique identifier for the variable (used as the map key).
     * @param variablePathData The [VariablePathData] instance representing the variable path data.
     * @return This builder instance for method chaining.
     */
    fun addVariable(id: String, variablePathData: VariablePathData) = apply {
        structure[id] = variablePathData
        return this
    }

    /**
     * Replaces the entire variable structure with the provided map.
     *
     * @param structure A map where keys are variable IDs and values are [VariablePathData] instances.
     * @return This builder instance for method chaining.
     */
    fun structure(structure: Map<String, VariablePathData>) = apply {
        this.structure = structure.toMutableMap()
        return this
    }

    /**
     * Sets the language variable reference.
     *
     * @param languageVariable A [VariableRef] representing the language variable.
     * @return This builder instance for method chaining.
     */
    fun languageVariable(languageVariable: VariableRef?) = apply {
        this.languageVariable = languageVariable
        return this
    }

    /**
     * Sets the language variable reference using its ID.
     *
     * @param languageVariableId The unique identifier for the language variable.
     * @return This builder instance for method chaining.
     */
    fun languageVariable(languageVariableId: String?) = apply {
        this.languageVariable = languageVariableId?.let { VariableRef(it) }
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
            created = created,
            lastUpdated = lastUpdated,
            structure = structure,
            languageVariable = languageVariable
        )
    }
}