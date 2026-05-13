package com.quadient.migration.api.dto.migrationmodel.builder.components

import com.quadient.migration.api.dto.migrationmodel.Variable
import com.quadient.migration.api.dto.migrationmodel.VariableRef

@Suppress("UNCHECKED_CAST")
interface HasVariableRef<T> {
    var variableRef: VariableRef?

    /**
     * Sets the variable reference for dynamic data binding.
     * @param variableRef The variable reference to set, or null to remove.
     * @return This builder instance for method chaining.
     */
    fun variableRef(variableRef: VariableRef?) = apply { this.variableRef = variableRef } as T

    /**
     * Sets the variable reference for dynamic data binding using a string ID.
     * @param variableRefId The ID of the variable to reference, or null to remove.
     * @return This builder instance for method chaining.
     */
    fun variableRef(variableRefId: String?) = apply { this.variableRef = variableRefId?.let { VariableRef(it) } } as T

    /**
     * Sets the variable reference for dynamic data binding using a [Variable] model object.
     * @param variable The variable whose ID will be used as the reference, or null to remove.
     * @return This builder instance for method chaining.
     */
    fun variableRef(variable: Variable?) = apply { this.variableRef = variable?.let { VariableRef(it.id) } } as T
}

