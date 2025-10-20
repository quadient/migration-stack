package com.quadient.migration.api.dto.migrationmodel

import com.quadient.migration.data.SelectByLanguageModel
import com.quadient.migration.persistence.migrationmodel.SelectByLanguageEntity

data class SelectByLanguage(val cases: List<Case>) : DocumentContent {
    companion object {
        fun fromModel(model: SelectByLanguageModel): SelectByLanguage = SelectByLanguage(
            cases = model.cases.map { Case.fromModel(it) },
        )
    }

    fun toDb() = SelectByLanguageEntity(
        cases = cases.map { it.toDb() })

    data class Case(val content: List<DocumentContent>, val language: String) {
        companion object {
            fun fromModel(model: SelectByLanguageModel.CaseModel) = Case(
                content = model.content.map { DocumentContent.fromModelContent(it) }, language = model.language
            )
        }

        fun toDb() = SelectByLanguageEntity.CaseEntity(content = content.toDb(), language = language)
    }
}
