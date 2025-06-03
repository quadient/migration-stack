package com.quadient.migration.api.dto.migrationmodel.builder

import com.quadient.migration.api.dto.migrationmodel.Variable
import com.quadient.migration.shared.DataType

class VariableBuilder(id: String) : DtoBuilderBase<Variable, VariableBuilder>(id) {
    var dataType: DataType? = null
    var defaultValue: String? = null

    fun dataType(dataType: DataType) = apply { this.dataType = dataType }
    fun defaultValue(defaultValue: String?) = apply { this.defaultValue = defaultValue }

    override fun build(): Variable {
        return Variable(
            id = id,
            name = name,
            originLocations = originLocations,
            customFields = customFields,
            dataType = dataType ?: throw IllegalArgumentException("dataType is required"),
            defaultValue = defaultValue
        )
    }
}