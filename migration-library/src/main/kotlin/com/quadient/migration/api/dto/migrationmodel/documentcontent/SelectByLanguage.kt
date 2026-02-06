package com.quadient.migration.api.dto.migrationmodel

import com.quadient.migration.persistence.migrationmodel.SelectByLanguageEntity

data class SelectByLanguage(val cases: List<Case>) : DocumentContent, RefValidatable {
    override fun collectRefs(): List<Ref> {
        return cases.flatMap { it.collectRefs() }
    }

    companion object {
        fun fromDb(entity: SelectByLanguageEntity): SelectByLanguage = SelectByLanguage(
            cases = entity.cases.map { Case.fromDb(it) },
        )
    }

    fun toDb() = SelectByLanguageEntity(
        cases = cases.map { it.toDb() })

    data class Case(val content: List<DocumentContent>, val language: String) : RefValidatable {
        override fun collectRefs(): List<Ref> {
            return content.flatMap {
                when (it) {
                    is RefValidatable -> it.collectRefs()
                    else -> emptyList()
                }
            }
        }

        companion object {
            fun fromDb(entity: SelectByLanguageEntity.CaseEntity) = Case(
                content = entity.content.map { DocumentContent.fromDbContent(it) }, language = entity.language
            )
        }

        fun toDb() = SelectByLanguageEntity.CaseEntity(content = content.toDb(), language = language)
    }
}
