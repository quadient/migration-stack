package com.quadient.migration.api.dto.migrationmodel.builder.documentcontent

import com.quadient.migration.api.dto.migrationmodel.DisplayRuleRef
import com.quadient.migration.api.dto.migrationmodel.DocumentContent
import com.quadient.migration.api.dto.migrationmodel.DocumentObjectRef
import com.quadient.migration.api.dto.migrationmodel.ImageRef
import com.quadient.migration.api.dto.migrationmodel.SelectByLanguage
import com.quadient.migration.api.dto.migrationmodel.builder.FirstMatchBuilder
import com.quadient.migration.api.dto.migrationmodel.builder.ParagraphBuilder
import com.quadient.migration.api.dto.migrationmodel.builder.TableBuilder

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

    class CaseBuilder {
        var content: MutableList<DocumentContent> = mutableListOf()
        var language: String? = null

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
         * Sets the language for the case.
         * @param language The language to be used for the case.
         * @return A CaseBuilder instance for method chaining.
         */
        fun language(language: String) = apply { this.language = language }

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
            content.add(ImageRef(imageId))
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
         * Adds a first match block to the case.
         * @param builder A builder function to build the first match block.
         * @return A CaseBuilder instance for method chaining.
         */
        fun firstMatch(builder: FirstMatchBuilder.() -> Unit) = apply {
            content.add(FirstMatchBuilder().apply(builder).build())
        }

        /**
         * Adds a nested select by language block to the case.
         * @param builder A builder function to build the select by language block.
         * @return A CaseBuilder instance for method chaining.
         */
        fun selectByLanguage(builder: SelectByLanguageBuilder.() -> Unit) = apply {
            content.add(SelectByLanguageBuilder().apply(builder).build())
        }
    }
}
