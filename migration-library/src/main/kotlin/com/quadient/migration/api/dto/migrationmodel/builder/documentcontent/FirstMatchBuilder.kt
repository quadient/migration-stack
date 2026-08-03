package com.quadient.migration.api.dto.migrationmodel.builder

import com.quadient.migration.api.dto.migrationmodel.DisplayRuleRef
import com.quadient.migration.api.dto.migrationmodel.DocumentContent
import com.quadient.migration.api.dto.migrationmodel.FirstMatch
import com.quadient.migration.api.dto.migrationmodel.StringValue
import com.quadient.migration.api.dto.migrationmodel.VariableStringContent
import com.quadient.migration.api.dto.migrationmodel.builder.components.HasDisplayRuleRef
import com.quadient.migration.api.dto.migrationmodel.builder.components.HasName

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
     * Replaces the default content for the FirstMatch instance.
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

    /**
     * Sets the default content as a paragraph using a builder function.
     * @param builder A builder function to configure the paragraph.
     * @return The FirstMatchBuilder instance for method chaining.
     */
    fun defaultParagraph(builder: ParagraphBuilder.() -> Unit) = apply {
        default.add(ParagraphBuilder().apply(builder).build())
    }

    /**
     * Sets the default content as a table using a builder function.
     * @param builder A builder function to configure the table.
     * @return The FirstMatchBuilder instance for method chaining.
     */
    fun defaultTable(builder: TableBuilder.() -> Unit) = apply {
        default.add(TableBuilder().apply(builder).build())
    }

    /**
     * Adds default content as a string value.
     * @param text The string to be used as default.
     * @return The FirstMatchBuilder instance for method chaining.
     */
    fun defaultString(text: String) = apply {
        default.add(StringValue(text))
    }

    class CaseBuilder : DocumentContentBuilderBase<CaseBuilder>, HasDisplayRuleRef<CaseBuilder>, HasName<CaseBuilder> {
        override val content: MutableList<DocumentContent> = mutableListOf()
        override var displayRuleRef: DisplayRuleRef? = null
        override var name: String? = null
    }
}

class SimpleFirstMatchBuilder {
    private var default: MutableList<VariableStringContent> = mutableListOf()
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
     * Replaces the default content for the FirstMatch instance.
     * @param default The default DocumentContent to be used.
     * @return The FirstMatchBuilder instance for method chaining.
     */
    fun default(default: VariableStringContent) = apply { this.default = mutableListOf(default) }

    /**
     * Appends additional default content to the FirstMatch instance.
     * @param default The DocumentContent to be added to the default list.
     * @return The FirstMatchBuilder instance for method chaining.
     */
    fun appendDefault(default: VariableStringContent) = apply { this.default.add(default) }

    /**
     * Adds a case to the FirstMatch instance using a builder function.
     * @param builder A builder function to configure the CaseBuilder.
     * @return The FirstMatchBuilder instance for method chaining.
     */
    fun case(builder: CaseBuilder.() -> Unit) = apply {
        val caseBuilder = CaseBuilder().apply(builder)
        cases.add(caseBuilder)
    }

    /**
     * Adds default content as a string value.
     * @param text The string to be used as default.
     * @return The FirstMatchBuilder instance for method chaining.
     */
    fun defaultString(text: String) = apply {
        default.add(StringValue(text))
    }

    class CaseBuilder : HasDisplayRuleRef<CaseBuilder>,
        HasName<CaseBuilder>,
        HasStringContent<VariableStringContent, CaseBuilder>,
        HasVariableRefContent<VariableStringContent, CaseBuilder>
    {
        override val content: MutableList<VariableStringContent> = mutableListOf()
        override var displayRuleRef: DisplayRuleRef? = null
        override var name: String? = null
    }
}
