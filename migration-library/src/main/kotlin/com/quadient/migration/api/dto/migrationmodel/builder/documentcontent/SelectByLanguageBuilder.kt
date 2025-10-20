package com.quadient.migration.api.dto.migrationmodel.builder.documentcontent

import com.quadient.migration.api.dto.migrationmodel.DocumentContent
import com.quadient.migration.api.dto.migrationmodel.SelectByLanguage

class SelectByLanguageBuilder  {
    private var cases: MutableList<CaseBuilder> = mutableListOf()

    /**
     * Builds a FirstMatch instance with the provided cases and default content.
     * The displayRuleRef for each case must be provided.
     * @return A FirstMatch instance containing the cases and default content.
     */
    fun build(): SelectByLanguage {
        return SelectByLanguage (
            cases.map {
                SelectByLanguage.Case(
                    it.content,
                    requireNotNull(it.language) { "language must be provided" }
                )
            }
        )
    }

    /**
     * Adds a new case to the FirstMatch instance.
     * @return A CaseBuilder instance to configure the new case.
     */
    fun addCase() = CaseBuilder().apply { cases.add(this) }

    /**
     * Adds a case to the FirstMatch instance using a builder function.
     * @param builder A builder function to configure the CaseBuilder.
     * @return The FirstMatchBuilder instance for method chaining.
     */
    fun case(builder: CaseBuilder.() -> Unit) = apply {
        val caseBuilder = CaseBuilder().apply(builder)
        cases.add(caseBuilder)
    }

    class CaseBuilder {
        var content: MutableList<DocumentContent> = mutableListOf()
        var language: String? = null

        /**
         * Sets the content for the case.
         * @param content The [DocumentContent] to be used in the case.
         * @return A CaseBuilder instance for method chaining.
         */
        fun content(content: DocumentContent) = apply { this.content = mutableListOf(content) }

        /**
         * Appends additional content to the case.
         * @param content The [DocumentContent] to be added to the case.
         * @return A CaseBuilder instance for method chaining.
         */
        fun appendContent(content: DocumentContent) = apply { this.content.add(content) }

        /**
         * Sets the language for the case.
         * @param language The language to be used for the case.
         * @return A CaseBuilder instance for method chaining.
         */
        fun language(language: String) = apply { this.language = language }
    }
}
