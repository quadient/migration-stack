package com.quadient.migration.api.dto.migrationmodel.builder

import com.quadient.migration.api.dto.migrationmodel.VariableStringContent

class VariableStringContentBuilder : HasGenericContent<VariableStringContent, VariableStringContentBuilder>,
    HasStringContent<VariableStringContent, VariableStringContentBuilder>,
    HasVariableRefContent<VariableStringContent, VariableStringContentBuilder> {
    override val content: MutableList<VariableStringContent> = mutableListOf()

    fun build(): List<VariableStringContent> = content
}
