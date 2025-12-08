package com.quadient.migration.api.dto.migrationmodel.builder

import com.quadient.migration.api.dto.migrationmodel.DisplayRuleRef
import com.quadient.migration.api.dto.migrationmodel.DocumentContent
import com.quadient.migration.api.dto.migrationmodel.DocumentObjectRef
import com.quadient.migration.api.dto.migrationmodel.Table
import com.quadient.migration.api.dto.migrationmodel.builder.documentcontent.SelectByLanguageBuilder
import com.quadient.migration.shared.Size

class TableBuilder {
    private val rows = mutableListOf<Row>()
    private val columnWidths = mutableListOf<ColumnWidth>()

    /**
     * Add a row to the table. Rows are added in the order they are defined.
     * And all rows must have the same number of cells.
     */
    fun addRow() = Row().apply { rows.add(this) }

    /**
     * Add a column width to the table. Column widths are added in the order they are defined.
     */
    fun addColumnWidth(minWidth: Size, percentWidth: Double) = apply {
        columnWidths.add(ColumnWidth(minWidth, percentWidth))
    }

    /**
     * Set the column widths for the table. This will replace any existing column widths.
     * @param width The list of column widths to set.
     */
    fun columnWidths(width: List<ColumnWidth>) = columnWidths.apply { clear() }.addAll(width)

    fun build(): Table {
        return Table(rows = rows.map {
            Table.Row(cells = it.cells.map {
                Table.Cell(
                    content = it.content,
                    mergeUp = it.mergeUp,
                    mergeLeft = it.mergeLeft,
                )
            }, displayRuleRef = it.displayRuleRef)
        }, columnWidths = columnWidths.map {
            Table.ColumnWidth(
                minWidth = it.minWidth,
                percentWidth = it.percentWidth,
            )
        })
    }

    class Row {
        val cells = mutableListOf<Cell>()
        var displayRuleRef: DisplayRuleRef? = null

        /**
         * Add a cell to the row. Cells are added in the order they are defined.
         * And all rows must have the same number of cells.
         */
        fun addCell() = Cell().apply { cells.add(this) }
        fun displayRuleRef(id: String) = this.apply { this.displayRuleRef = DisplayRuleRef(id) }
        fun displayRuleRef(ref: DisplayRuleRef) = this.apply { this.displayRuleRef = ref }
    }

    class Cell {
        val content = mutableListOf<DocumentContent>()
        var mergeLeft = false
        var mergeUp = false

        fun mergeLeft(value: Boolean) = apply { mergeLeft = value }
        fun mergeUp(value: Boolean) = apply { mergeUp = value }

        /**
         * Appends content to the cell.
         * @param content The content to append to the cell.
         * @return The [Cell] instance for method chaining.
         */
        fun appendContent(content: DocumentContent) = apply { this.content.add(content) }

        /**
         * Replaces all content in the cell with a single item.
         * @param content The content to set as the cell content.
         * @return The [Cell] instance for method chaining.
         */
        fun content(content: DocumentContent) = apply { this.content.apply { clear() }.add(content) }

        /**
         * Replaces all content in the cell with multiple items.
         * @param content The list of content to set as the cell content.
         * @return The [Cell] instance for method chaining.
         */
        fun content(content: List<DocumentContent>) = apply { this@Cell.content.apply { clear() }.addAll(content) }

        /**
         * Adds a paragraph with the given string to the cell.
         * @param text The string to add in a paragraph.
         * @return The [Cell] instance for method chaining.
         */
        fun string(text: String) = apply {
            this.content.add(ParagraphBuilder().string(text).build())
        }

        /**
         * Adds a paragraph to the cell using a builder function.
         * @param builder A builder function to build the paragraph.
         * @return The [Cell] instance for method chaining.
         */
        fun paragraph(builder: ParagraphBuilder.() -> Unit) = apply {
            content.add(ParagraphBuilder().apply(builder).build())
        }

        /**
         * Adds a table to the cell using a builder function.
         * @param builder A builder function to build the table.
         * @return The [Cell] instance for method chaining.
         */
        fun table(builder: TableBuilder.() -> Unit) = apply {
            content.add(TableBuilder().apply(builder).build())
        }

        /**
         * Adds an image reference to the cell.
         * @param imageId The ID of the image to reference.
         * @return The [Cell] instance for method chaining.
         */
        fun imageRef(imageId: String) = apply {
            content.add(com.quadient.migration.api.dto.migrationmodel.ImageRef(imageId))
        }

        /**
         * Adds a document object reference to the cell.
         * @param documentObjectId The ID of the document object to reference.
         * @return The [Cell] instance for method chaining.
         */
        fun documentObjectRef(documentObjectId: String) = apply {
            content.add(DocumentObjectRef(documentObjectId, null))
        }

        /**
         * Adds a conditional document object reference to the cell.
         * @param documentObjectId The ID of the document object to reference.
         * @param displayRuleId The ID of the display rule.
         * @return The [Cell] instance for method chaining.
         */
        fun documentObjectRef(documentObjectId: String, displayRuleId: String) = apply {
            content.add(DocumentObjectRef(documentObjectId, DisplayRuleRef(displayRuleId)))
        }

        /**
         * Adds a first match block to the cell using a builder function.
         * @param builder A builder function to build the first match block.
         * @return The [Cell] instance for method chaining.
         */
        fun firstMatch(builder: FirstMatchBuilder.() -> Unit) = apply {
            content.add(FirstMatchBuilder().apply(builder).build())
        }

        /**
         * Adds a select by language block to the cell using a builder function.
         * @param builder A builder function to build the select by language block.
         * @return The [Cell] instance for method chaining.
         */
        fun selectByLanguage(builder: SelectByLanguageBuilder.() -> Unit) = apply {
            content.add(SelectByLanguageBuilder().apply(builder).build())
        }
    }

    data class ColumnWidth(val minWidth: Size, val percentWidth: Double)
}
