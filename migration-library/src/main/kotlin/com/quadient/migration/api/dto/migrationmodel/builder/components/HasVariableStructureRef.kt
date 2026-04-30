package com.quadient.migration.api.dto.migrationmodel.builder.components

import com.quadient.migration.api.dto.migrationmodel.VariableStructure
import com.quadient.migration.api.dto.migrationmodel.VariableStructureRef

@Suppress("UNCHECKED_CAST")
interface HasVariableStructureRef<T> {
    var variableStructureRef: VariableStructureRef?

    /**
     * Add a reference to a variable structure to this document object.
     * @param id ID of the variable structure to reference.
     * @return This builder instance for method chaining.
     */
    fun variableStructureRef(id: String?) =
        apply { this.variableStructureRef = id?.let { VariableStructureRef(it) } } as T

    /**
     * Add a reference to a variable structure to this document object.
     * @param variableStructure The [VariableStructure] object to reference.
     * @return This builder instance for method chaining.
     */
    fun variableStructureRef(variableStructure: VariableStructure?) =
        apply { this.variableStructureRef = variableStructure?.let { VariableStructureRef(it.id) } } as T
}
