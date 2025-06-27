package com.quadient.migration.api.dto.migrationmodel.builder

import com.quadient.migration.api.dto.migrationmodel.DisplayRuleRef
import com.quadient.migration.api.dto.migrationmodel.DocumentContent
import com.quadient.migration.api.dto.migrationmodel.FirstMatch

class FirstMatchBuilder {
    private var default: MutableList<DocumentContent> = mutableListOf()
    private var cases: MutableList<CaseBuilder> = mutableListOf()

    /**
     * Builds a FirstMatch instance with the provided cases and default content.
     * The displayRuleRef for each case must be provided.
     * @return A FirstMatch instance containing the cases and default content.
     */
    fun build(): FirstMatch {
        return FirstMatch(
            cases.map {
                FirstMatch.Case(
                    it.displayRuleRef ?: throw IllegalArgumentException("displayRuleRef must be provided"),
                    it.content,
                    it.name
                )
            }, default
        )
    }

    /**
     * Adds a new case to the FirstMatch instance.
     * @return A CaseBuilder instance to configure the new case.
     */
    fun addCase() = CaseBuilder().apply { cases.add(this) }

    /**
     * Sets the default content for the FirstMatch instance.
     * @param default The default DocumentContent to be used.
     * @return The FirstMatchBuilder instance for method chaining.
     */
    fun default(default: DocumentContent) = apply { this.default = mutableListOf(default) }

    /**
     * Appends additional default content to the FirstMatch instance.
     * @param default The DocumentContent to be added to the default list.
     * @return The FirstMatchBuilder instance for method chaining.
     */
    fun appendDefault(default: DocumentContent) = apply { this.default.add(default) }

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
        var displayRuleRef: DisplayRuleRef? = null
        var name: String? = null

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
         * Sets the display rule reference for the case.
         * @param ref The [DisplayRuleRef] to be used in the case.
         * @return A CaseBuilder instance for method chaining.
         */
        fun displayRule(ref: DisplayRuleRef) = apply { this.displayRuleRef = ref }

        /**
         * Sets the display rule reference for the case using an ID.
         * @param id The ID of the display rule to be used in the case.
         * @return A CaseBuilder instance for method chaining.
         */
        fun displayRule(id: String) = apply { this.displayRuleRef = DisplayRuleRef(id) }

        /**
         * Sets the name for the case.
         * @param name The name to be used for the case.
         * @return A CaseBuilder instance for method chaining.
         */
        fun name(name: String) = apply { this.name = name }
    }
}
