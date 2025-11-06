package com.quadient.migration.data

import com.quadient.migration.persistence.migrationmodel.SelectByLanguageEntity
import com.quadient.migration.service.RefValidatable

data class SelectByLanguageModel(val cases: List<CaseModel>) : DocumentContentModel, RefValidatable {
    companion object {
        fun fromDb(entity: SelectByLanguageEntity): SelectByLanguageModel = SelectByLanguageModel(
            cases = entity.cases.map { CaseModel.fromDb(it) },
        )
    }

    override fun collectRefs(): List<RefModel> {
        return cases.flatMap { it.content.flatMap { it.collectRefs() } }
    }

    data class CaseModel(val language: String, val content: List<DocumentContentModel>) {
        companion object {
            fun fromDb(entity: SelectByLanguageEntity.CaseEntity) = CaseModel(
                content = entity.content.map { DocumentContentModel.fromDbContent(it) }, language = entity.language
            )
        }
    }
}
