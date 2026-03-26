package com.quadient.migration.api.dto.migrationmodel

import com.quadient.migration.persistence.migrationmodel.RepeatedContentEntity
import com.quadient.migration.shared.VariablePath

data class RepeatedContent(
    val variablePath: VariablePath,
    val content: List<DocumentContent>,
) : DocumentContent, RefValidatable {

    override fun collectRefs(): List<Ref> {
        return content.flatMap {
            when (it) {
                is RefValidatable -> it.collectRefs()
                else -> emptyList()
            }
        }
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
