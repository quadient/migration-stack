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
        return Table(rows = rows.map { row ->
            Table.Row(cells = row.cells.map { cell ->
                Table.Cell(
                    content = cell.content,
                    mergeUp = cell.mergeUp,
                    mergeLeft = cell.mergeLeft,
                )
            }, displayRuleRef = row.displayRuleRef)
        }, columnWidths = columnWidths.map { colWidth ->
            Table.ColumnWidth(
                minWidth = colWidth.minWidth,
                percentWidth = colWidth.percentWidth,
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

    class Cell : DocumentContentBuilderBase<Cell> {
        override val content = mutableListOf<DocumentContent>()
        var mergeLeft = false
        var mergeUp = false

        fun mergeLeft(value: Boolean) = apply { mergeLeft = value }
        fun mergeUp(value: Boolean) = apply { mergeUp = value }

        /**
         * Adds a paragraph with the given string to the cell.
         * @param text The string to add in a paragraph.
         * @return The [Cell] instance for method chaining.
         */
        fun string(text: String) = apply {
            this.content.add(ParagraphBuilder().string(text).build())
        }
    }

    data class ColumnWidth(val minWidth: Size, val percentWidth: Double)
}
