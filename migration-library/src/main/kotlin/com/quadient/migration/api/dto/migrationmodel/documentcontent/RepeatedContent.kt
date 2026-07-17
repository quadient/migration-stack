package com.quadient.migration.api.dto.migrationmodel

import com.quadient.migration.persistence.migrationmodel.RepeatedContentEntity
import com.quadient.migration.shared.LiteralPath
import com.quadient.migration.shared.VariablePath
import com.quadient.migration.shared.VariableRefPath

data class RepeatedContent(
    val variablePath: VariablePath,
    val content: List<DocumentContent>,
) : DocumentContent, RefValidatable {
    override val pathName = "repeatedContent"

    override fun toPreview(nameResolver: (DocumentContent) -> String?): String {
        val variable = when (variablePath) {
            is VariableRefPath -> VariableRef(variablePath.variableId).toPreview(nameResolver)
            is LiteralPath -> variablePath.path
        }
        return "$pathName: $variable | ${content.size} items"
    }

    override fun collectRefs(): Set<Ref> {
        val contentRefs = content.flatMap {
            when (it) {
                is RefValidatable -> it.collectRefs()
                else -> emptySet()
            }
        }.toSet()

        val variableRefs = when (val path = variablePath) {
            is VariableRefPath -> setOf(VariableRef(path.variableId))
            else -> emptySet()
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
