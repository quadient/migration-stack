package com.quadient.migration.api.dto.migrationmodel

import com.quadient.migration.persistence.migrationmodel.FirstMatchEntity

data class FirstMatch(val cases: List<Case>, val default: List<DocumentContent>) : DocumentContent, TextContent, RefValidatable {
    override fun collectRefs(): List<Ref> {
        return cases.flatMap { it.collectRefs() } + default.flatMap {
            when (it) {
                is RefValidatable -> it.collectRefs()
                else -> emptyList()
            }
        }
    }

    companion object {
        fun fromDb(entity: FirstMatchEntity): FirstMatch = FirstMatch(
            cases = entity.cases.map { Case.fromDb(it) },
            default = entity.default.map { DocumentContent.fromDbContent(it) })
    }

    fun toDb() = FirstMatchEntity(
        cases = cases.map { it.toDb() }, default = default.toDb()
    )

    data class Case(val displayRuleRef: DisplayRuleRef, val content: List<DocumentContent>, val name: String?) : RefValidatable {
        override fun collectRefs(): List<Ref> {
            return listOf(displayRuleRef) + content.flatMap {
                when (it) {
                    is RefValidatable -> it.collectRefs()
                    else -> emptyList()
                }
            }
        }

        companion object {
            fun fromDb(entity: FirstMatchEntity.CaseEntity) = Case(
                displayRuleRef = DisplayRuleRef.fromDb(entity.displayRuleRef),
                content = entity.content.map { DocumentContent.fromDbContent(it) },
                name = entity.name
            )
        }

        fun toDb() = FirstMatchEntity.CaseEntity(
            displayRuleRef = displayRuleRef.toDb(), content = content.toDb(), name = name
        )
    }
}
