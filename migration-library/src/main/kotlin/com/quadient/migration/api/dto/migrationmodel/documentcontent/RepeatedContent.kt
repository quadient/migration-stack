package com.quadient.migration.api.dto.migrationmodel

import com.quadient.migration.persistence.migrationmodel.RepeatedContentEntity
import com.quadient.migration.shared.VariablePath
import com.quadient.migration.shared.VariableRefPath

data class RepeatedContent(
    val variablePath: VariablePath,
    val content: List<DocumentContent>,
) : DocumentContent, RefValidatable {

    override fun collectRefs(): List<Ref> {
        val contentRefs = content.flatMap {
            when (it) {
                is RefValidatable -> it.collectRefs()
            }
        }

        val variableRefs = when (val path = variablePath) {
            is VariableRefPath -> listOf(VariableRef(path.variableId))
            else -> emptyList()
        }

        return contentRefs + variableRefs
    }

    companion object {
        fun fromDb(entity: RepeatedContentEntity): RepeatedContent = RepeatedContent(
            variablePath = entity.variablePath,
            content = entity.content.map { DocumentContent.fromDbContent(it) },
        )
    }

    fun toDb() = RepeatedContentEntity(
        variablePath = variablePath,
        content = content.toDb(),
    )
}
