package com.quadient.migration.api.dto.migrationmodel.builder

import com.quadient.migration.api.dto.migrationmodel.Variable
import com.quadient.migration.shared.DataType

class VariableBuilder(id: String) : DtoBuilderBase<Variable, VariableBuilder>(id) {
    var dataType: DataType? = null
    var defaultValue: String? = null

    /**
     * Sets the [DataType] of the variable.
     * @param dataType The data type of the variable.
     * @return The builder instance for method chaining.
     */
    fun dataType(dataType: DataType) = apply { this.dataType = dataType }

    /**
     * Sets the default value of the variable.
     * @param defaultValue The default value of the variable.
     * @return The builder instance for method chaining.
     */
    fun defaultValue(defaultValue: String?) = apply { this.defaultValue = defaultValue }

    /**
     * Builds the [Variable] instance.
     * @return The constructed [Variable].
     * @throws IllegalArgumentException if dataType is not set.
     */
    override fun build(): Variable {
        return Variable(
            id = id,
            name = name,
            originLocations = originLocations,
            customFields = customFields,
            created = created,
            lastUpdated = lastUpdated,
            dataType = dataType ?: throw IllegalArgumentException("dataType is required"),
            defaultValue = defaultValue
        )
    }
}