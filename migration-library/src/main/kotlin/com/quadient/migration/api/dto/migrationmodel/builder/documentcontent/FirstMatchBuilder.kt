package com.quadient.migration.api.dto.migrationmodel.builder

import com.quadient.migration.api.dto.migrationmodel.DisplayRuleRef
import com.quadient.migration.api.dto.migrationmodel.DocumentContent
import com.quadient.migration.api.dto.migrationmodel.DocumentObjectRef
import com.quadient.migration.api.dto.migrationmodel.FirstMatch
import com.quadient.migration.api.dto.migrationmodel.builder.documentcontent.SelectByLanguageBuilder

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
     * Adds default content as a paragraph with the given string.
     * @param text The string to be wrapped in a paragraph.
     * @return The FirstMatchBuilder instance for method chaining.
     */
    fun defaultString(text: String) = apply {
        default.add(ParagraphBuilder().string(text).build())
    }

    class CaseBuilder {
        var content: MutableList<DocumentContent> = mutableListOf()
        var displayRuleRef: DisplayRuleRef? = null
        var name: String? = null

        /**
         * Replaces the content for the case.
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

        /**
         * Adds a paragraph to the case using a builder function.
         * @param builder A builder function to build the paragraph.
         * @return A CaseBuilder instance for method chaining.
         */
        fun paragraph(builder: ParagraphBuilder.() -> Unit) = apply {
            content.add(ParagraphBuilder().apply(builder).build())
        }

        /**
         * Adds a table to the case using a builder function.
         * @param builder A builder function to build the table.
         * @return A CaseBuilder instance for method chaining.
         */
        fun table(builder: TableBuilder.() -> Unit) = apply {
            content.add(TableBuilder().apply(builder).build())
        }

        /**
         * Adds an image reference to the case.
         * @param imageId The ID of the image to reference.
         * @return A CaseBuilder instance for method chaining.
         */
        fun imageRef(imageId: String) = apply {
            content.add(com.quadient.migration.api.dto.migrationmodel.ImageRef(imageId))
        }

        /**
         * Adds a document object reference to the case.
         * @param documentObjectId The ID of the document object to reference.
         * @return A CaseBuilder instance for method chaining.
         */
        fun documentObjectRef(documentObjectId: String) = apply {
            content.add(DocumentObjectRef(documentObjectId, null))
        }

        /**
         * Adds a conditional document object reference to the case.
         * @param documentObjectId The ID of the document object to reference.
         * @param displayRuleId The ID of the display rule.
         * @return A CaseBuilder instance for method chaining.
         */
        fun documentObjectRef(documentObjectId: String, displayRuleId: String) = apply {
            content.add(DocumentObjectRef(documentObjectId, DisplayRuleRef(displayRuleId)))
        }

        /**
         * Adds a nested first match block to the case.
         * @param builder A builder function to build the first match block.
         * @return A CaseBuilder instance for method chaining.
         */
        fun firstMatch(builder: FirstMatchBuilder.() -> Unit) = apply {
            content.add(FirstMatchBuilder().apply(builder).build())
        }

        /**
         * Adds a select by language block to the case.
         * @param builder A builder function to build the select by language block.
         * @return A CaseBuilder instance for method chaining.
         */
        fun selectByLanguage(builder: SelectByLanguageBuilder.() -> Unit) = apply {
            content.add(SelectByLanguageBuilder().apply(builder).build())
        }
    }
}
