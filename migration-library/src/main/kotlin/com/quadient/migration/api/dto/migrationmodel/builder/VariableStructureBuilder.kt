package com.quadient.migration.api.dto.migrationmodel.builder

import com.quadient.migration.api.dto.migrationmodel.Variable
import com.quadient.migration.api.dto.migrationmodel.VariableRef
import com.quadient.migration.api.dto.migrationmodel.VariableStructure
import com.quadient.migration.shared.LiteralPath
import com.quadient.migration.shared.VariablePathData
import com.quadient.migration.shared.VariableRefPath

class VariableStructureBuilder(id: String) : DtoBuilderBase<VariableStructure, VariableStructureBuilder>(id) {
    var structure = mutableMapOf<String, VariablePathData>()
    var languageVariable: VariableRef? = null

    /**
     * Adds a variable to the structure with a literal path.
     *
     * @param id The unique identifier for the variable (used as the map key).
     * @param path The literal path of the variable (e.g. "Data.Clients.Value").
     * @return This builder instance for method chaining.
     */
    fun addVariable(id: String, path: String): VariableStructureBuilder = apply {
        structure[id] = VariablePathData(LiteralPath(path), null)
    }

    /**
     * Adds a variable to the structure with a literal path and a display name override.
     *
     * @param id The unique identifier for the variable (used as the map key).
     * @param path The literal path of the variable (e.g. "Data.Clients.Value").
     * @param name A name to override the variable's default name.
     * @return This builder instance for method chaining.
     */
    fun addVariable(id: String, path: String, name: String): VariableStructureBuilder = apply {
        structure[id] = VariablePathData(LiteralPath(path), name)
    }

    /**
     * Adds a variable to the structure referencing an Array or SubTree variable by its ID.
     * The referenced variable must be registered with [DataType.Array] or [DataType.SubTree]
     * and must carry a [Variable.path].
     *
     * @param id The unique identifier for the variable (used as the map key).
     * @param variableRef A [VariableRef] pointing to an Array or SubTree variable.
     * @return This builder instance for method chaining.
     */
    fun addVariable(id: String, variableRef: VariableRef): VariableStructureBuilder = apply {
        structure[id] = VariablePathData(VariableRefPath(variableRef.id), null)
    }

    /**
     * Adds a variable to the structure referencing an Array or SubTree variable by its ID,
     * with a display name override.
     * The referenced variable must be registered with [DataType.Array] or [DataType.SubTree]
     * and must carry a [Variable.path].
     *
     * @param id The unique identifier for the variable (used as the map key).
     * @param variableRef A [VariableRef] pointing to an Array or SubTree variable.
     * @param name A name to override the variable's default name.
     * @return This builder instance for method chaining.
     */
    fun addVariable(id: String, variableRef: VariableRef, name: String): VariableStructureBuilder = apply {
        structure[id] = VariablePathData(VariableRefPath(variableRef.id), name)
    }

    /**
     * Adds a variable to the structure referencing an Array or SubTree variable directly.
     * The referenced variable must be registered with [DataType.Array] or [DataType.SubTree]
     * and must carry a [Variable.path].
     *
     * @param id The unique identifier for the variable (used as the map key).
     * @param variable A [Variable] whose ID is used as the variable reference path.
     * @return This builder instance for method chaining.
     */
    fun addVariable(id: String, variable: Variable): VariableStructureBuilder = apply {
        structure[id] = VariablePathData(VariableRefPath(variable.id), null)
    }

    /**
     * Adds a variable to the structure referencing an Array or SubTree variable directly,
     * with a display name override.
     * The referenced variable must be registered with [DataType.Array] or [DataType.SubTree]
     * and must carry a [Variable.path].
     *
     * @param id The unique identifier for the variable (used as the map key).
     * @param variable A [Variable] whose ID is used as the variable reference path.
     * @param name A name to override the variable's default name.
     * @return This builder instance for method chaining.
     */
    fun addVariable(id: String, variable: Variable, name: String): VariableStructureBuilder = apply {
        structure[id] = VariablePathData(VariableRefPath(variable.id), name)
    }

    /**
     * Adds a variable to the structure using a [VariablePathData] instance.
     *
     * @param id The unique identifier for the variable (used as the map key).
     * @param variablePathData The [VariablePathData] instance representing the variable path data.
     * @return This builder instance for method chaining.
     */
    fun addVariable(id: String, variablePathData: VariablePathData): VariableStructureBuilder = apply {
        structure[id] = variablePathData
    }

    /**
     * Replaces the entire variable structure with the provided map.
     *
     * @param structure A map where keys are variable IDs and values are [VariablePathData] instances.
     * @return This builder instance for method chaining.
     */
    fun structure(structure: Map<String, VariablePathData>): VariableStructureBuilder = apply {
        this.structure = structure.toMutableMap()
    }

    /**
     * Sets the language variable reference.
     *
     * @param languageVariable A [VariableRef] representing the language variable.
     * @return This builder instance for method chaining.
     */
    fun languageVariable(languageVariable: VariableRef?): VariableStructureBuilder = apply {
        this.languageVariable = languageVariable
    }

    /**
     * Sets the language variable reference using its ID.
     *
     * @param languageVariableId The unique identifier for the language variable.
     * @return This builder instance for method chaining.
     */
    fun languageVariable(languageVariableId: String?): VariableStructureBuilder = apply {
        this.languageVariable = languageVariableId?.let { VariableRef(it) }
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
            languageVariable = languageVariable
        )
    }
}