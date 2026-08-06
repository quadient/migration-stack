package com.quadient.migration.api.dto.migrationmodel.builder.documentcontent

import com.quadient.migration.api.dto.migrationmodel.DocumentContent
import com.quadient.migration.api.dto.migrationmodel.SelectByLanguage
import com.quadient.migration.api.dto.migrationmodel.builder.DocumentContentBuilderBase

class SelectByLanguageBuilder {
    private var cases: MutableList<CaseOrBuilder> = mutableListOf()

    /**
     * Builds a SelectByLanguage instance with the provided cases.
     * @return A SelectByLanguage instance containing the cases.
     */
    fun build(): SelectByLanguage {
        return SelectByLanguage(cases.map {
            when (it) {
                is PrebuiltCase -> it.case
                is CaseBuilder -> it.build()
            }
        })
    }

    /**
     * Adds a new case to the SelectByLanguageBuilder instance.
     * @return A CaseBuilder instance to configure the new case.
     */
    fun addCase() = CaseBuilder().apply { cases.add(this) }

    /**
     * Adds a case to the SelectByLanguageBuilder instance using a builder function.
     * @param builder A builder function to configure the CaseBuilder.
     * @return The SelectByLanguageBuilder instance for method chaining.
     */
    fun case(builder: CaseBuilder.() -> Unit) = apply {
        val caseBuilder = CaseBuilder().apply(builder)
        cases.add(caseBuilder)
    }

    /**
     * Adds an existing case to the SelectByLanguageBuilder instance.
     * @param case The [SelectByLanguage.Case] instance to add.
     * @return The SelectByLanguageBuilder instance for method chaining.
     */
    fun case(case: SelectByLanguage.Case) = apply {
        cases.add(PrebuiltCase(case))
    }

    private sealed interface CaseOrBuilder

    @JvmInline
    value class PrebuiltCase(val case: SelectByLanguage.Case): CaseOrBuilder

    class CaseBuilder : DocumentContentBuilderBase<CaseBuilder>, CaseOrBuilder {
        override val content: MutableList<DocumentContent> = mutableListOf()
        var language: String? = null

        /**
         * Sets the language for the case.
         * @param language The language to be used for the case.
         * @return A CaseBuilder instance for method chaining.
         */
        fun language(language: String) = apply { this.language = language }

        fun build(): SelectByLanguage.Case {
            return SelectByLanguage.Case(content, requireNotNull(language) { "language must be provided" })
        }
    }
}
