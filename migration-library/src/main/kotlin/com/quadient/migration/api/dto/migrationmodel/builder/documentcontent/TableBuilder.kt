package com.quadient.migration.api.dto.migrationmodel.builder

import com.quadient.migration.api.dto.migrationmodel.DisplayRuleRef
import com.quadient.migration.api.dto.migrationmodel.DocumentContent
import com.quadient.migration.api.dto.migrationmodel.Table
import com.quadient.migration.shared.Size
import com.quadient.migration.shared.PdfTaggingRule

class TableBuilder {
    private val rows = mutableListOf<Row>()
    private val columnWidths = mutableListOf<ColumnWidth>()
    private var pdfTaggingRule: PdfTaggingRule = PdfTaggingRule.Default
    private var pdfAlternateText: String? = null

    fun pdfTaggingRule(rule: PdfTaggingRule) = apply { this.pdfTaggingRule = rule }
    fun pdfAlternateText(text: String?) = apply { this.pdfAlternateText = text }

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
        }, pdfTaggingRule, pdfAlternateText)
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
    }

    data class ColumnWidth(val minWidth: Size, val percentWidth: Double)
}
