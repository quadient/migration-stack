package com.quadient.migration.api.dto.migrationmodel

import com.quadient.migration.data.FirstMatchModel
import com.quadient.migration.persistence.migrationmodel.FirstMatchEntity

data class FirstMatch(val cases: List<Case>, val default: List<DocumentContent>) : DocumentContent, TextContent {
    companion object {
        fun fromModel(model: FirstMatchModel): FirstMatch = FirstMatch(
            cases = model.cases.map { Case.fromModel(it) },
            default = model.default.map { DocumentContent.fromModelContent(it) })
    }

    fun toDb() = FirstMatchEntity(
        cases = cases.map { it.toDb() }, default = default.toDb()
    )

    data class Case(val displayRuleRef: DisplayRuleRef, val content: List<DocumentContent>, val name: String?) {
        companion object {
            fun fromModel(model: FirstMatchModel.CaseModel) = Case(
                displayRuleRef = DisplayRuleRef.fromModel(model.displayRuleRef),
                content = model.content.map { DocumentContent.fromModelContent(it) },
                name = model.name
            )
        }

        fun toDb() = FirstMatchEntity.CaseEntity(
            displayRuleRef = displayRuleRef.toDb(), content = content.toDb(), name = name
        )
    }
}
