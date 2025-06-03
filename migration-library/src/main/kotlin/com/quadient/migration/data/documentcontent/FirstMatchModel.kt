package com.quadient.migration.data

import com.quadient.migration.persistence.migrationmodel.FirstMatchEntity
import com.quadient.migration.persistence.migrationmodel.FirstMatchEntity.CaseEntity
import com.quadient.migration.service.RefValidatable

data class FirstMatchModel(val cases: List<CaseModel>, val default: List<DocumentContentModel>) : RefValidatable {
    companion object {
        fun fromDb(entity: FirstMatchEntity) = FirstMatchModel(
            cases = entity.cases.map { CaseModel.fromDb(it) },
            default = entity.default.map { DocumentContentModel.fromDbContent(it) })
    }

    override fun collectRefs(): List<RefModel> {
        return cases.flatMap { listOf(it.displayRuleRef) + it.content.flatMap { it.collectRefs() } + default.flatMap { it.collectRefs() } }
    }

    data class CaseModel(val displayRuleRef: DisplayRuleModelRef, val content: List<DocumentContentModel>) {
        companion object {
            fun fromDb(entity: CaseEntity) = CaseModel(
                displayRuleRef = DisplayRuleModelRef.fromDb(entity.displayRuleRef),
                content = entity.content.map { DocumentContentModel.fromDbContent(it) })
        }
    }
}
