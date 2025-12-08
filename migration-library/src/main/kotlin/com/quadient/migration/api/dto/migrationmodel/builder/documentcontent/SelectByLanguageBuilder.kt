package com.quadient.migration.api.dto.migrationmodel.builder.documentcontent

import com.quadient.migration.api.dto.migrationmodel.DocumentContent
import com.quadient.migration.api.dto.migrationmodel.SelectByLanguage
import com.quadient.migration.api.dto.migrationmodel.builder.DocumentContentBuilderBase

class SelectByLanguageBuilder {
    private var cases: MutableList<CaseBuilder> = mutableListOf()

    /**
     * Builds a SelectByLanguage instance with the provided cases and default content.
     * @return A SelectByLanguage instance containing the cases and default content.
     */
    fun build(): SelectByLanguage {
        return SelectByLanguage(
            cases.map {
                SelectByLanguage.Case(
                    it.content, requireNotNull(it.language) { "language must be provided" })
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

    class CaseBuilder : DocumentContentBuilderBase<CaseBuilder> {
        override val content: MutableList<DocumentContent> = mutableListOf()
        var language: String? = null

        /**
         * Sets the language for the case.
         * @param language The language to be used for the case.
         * @return A CaseBuilder instance for method chaining.
         */
        fun language(language: String) = apply { this.language = language }
    }
}
