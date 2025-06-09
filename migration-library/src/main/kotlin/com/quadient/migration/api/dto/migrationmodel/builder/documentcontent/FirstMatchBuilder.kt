package com.quadient.migration.api.dto.migrationmodel.builder

import com.quadient.migration.api.dto.migrationmodel.DisplayRuleRef
import com.quadient.migration.api.dto.migrationmodel.DocumentContent
import com.quadient.migration.api.dto.migrationmodel.FirstMatch

class FirstMatchBuilder {
    var default: MutableList<DocumentContent> = mutableListOf()
    var cases: MutableList<CaseBuilder> = mutableListOf()

    fun build(): FirstMatch {
        return FirstMatch(
            cases.map {
                FirstMatch.Case(
                    it.displayRuleRef ?: throw IllegalArgumentException("displayRuleRef must be provided"),
                    it.content
                )
            },
            default
        )
    }

    fun addCase() = CaseBuilder().apply { cases.add(this) }
    fun default(default: DocumentContent) = apply { this.default = mutableListOf(default) }
    fun appendDefault(default: DocumentContent) = apply { this.default.add(default) }

    fun case(builder: CaseBuilder.() -> Unit) = apply {
        val caseBuilder = CaseBuilder().apply(builder)
        cases.add(caseBuilder)
    }

    class CaseBuilder {
        var content: MutableList<DocumentContent> = mutableListOf()
        var displayRuleRef: DisplayRuleRef? = null

        fun content(content: DocumentContent) = apply { this.content = mutableListOf(content) }
        fun appendContent(content: DocumentContent) = apply { this.content.add(content) }
        fun displayRule(ref: DisplayRuleRef) = apply { this.displayRuleRef = ref }
        fun displayRule(id: String) = apply { this.displayRuleRef = DisplayRuleRef(id) }
    }
}
