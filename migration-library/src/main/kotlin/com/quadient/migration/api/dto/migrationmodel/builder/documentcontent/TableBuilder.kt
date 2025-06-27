package com.quadient.migration.api.dto.migrationmodel.builder

import com.quadient.migration.api.dto.migrationmodel.DisplayRuleRef
import com.quadient.migration.api.dto.migrationmodel.DocumentContent
import com.quadient.migration.api.dto.migrationmodel.Table
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
         * Append content to the cell.
         * @param content The content to append to the cell. Must be either
         * a [String], [com.quadient.migration.api.dto.migrationmodel.Text]
         * or [com.quadient.migration.api.dto.migrationmodel.Ref].
         */
        fun appendContent(content: DocumentContent) = apply { this.content.add(content) }

        /**
         * Replace content of the cell with single object.
         * @param content The content to append to the cell. Must be either
         * a [String], [com.quadient.migration.api.dto.migrationmodel.Text]
         * or [com.quadient.migration.api.dto.migrationmodel.Ref].
         */
        fun content(content: DocumentContent) = apply { this.content.apply { clear() }.add(content) }

        /**
         * Replace content of the cell with the provided list of objects.
         * @param content The content to append to the cell. Must be either
         * a [String], [com.quadient.migration.api.dto.migrationmodel.Text]
         * or [com.quadient.migration.api.dto.migrationmodel.Ref].
         */
        fun content(content: List<DocumentContent>) = apply { this@Cell.content.apply { clear() }.addAll(content) }
    }

    data class ColumnWidth(val minWidth: Size, val percentWidth: Double)
}
