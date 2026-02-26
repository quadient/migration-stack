package com.quadient.migration.api.dto.migrationmodel.builder

import com.quadient.migration.api.dto.migrationmodel.StringValue
import com.quadient.migration.api.dto.migrationmodel.VariableRef
import com.quadient.migration.api.dto.migrationmodel.VariableStringContent

class VariableStringContentBuilder {
    private val content: MutableList<VariableStringContent> = mutableListOf()

    fun string(value: String) = apply { content.add(StringValue(value)) }

    fun variableRef(variableId: String) = apply { content.add(VariableRef(variableId)) }

    fun variableRef(ref: VariableRef) = apply { content.add(ref) }

    fun build(): List<VariableStringContent> = content
}
