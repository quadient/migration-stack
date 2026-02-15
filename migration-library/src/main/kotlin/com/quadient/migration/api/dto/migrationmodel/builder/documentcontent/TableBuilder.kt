package com.quadient.migration.api.dto.migrationmodel.builder

import com.quadient.migration.api.dto.migrationmodel.DisplayRuleRef
import com.quadient.migration.api.dto.migrationmodel.DocumentContent
import com.quadient.migration.api.dto.migrationmodel.Table
import com.quadient.migration.api.dto.migrationmodel.builder.documentcontent.BorderOptionsBuilder
import com.quadient.migration.shared.BorderOptions
import com.quadient.migration.shared.CellAlignment
import com.quadient.migration.shared.CellHeight
import com.quadient.migration.shared.Size
import com.quadient.migration.shared.TableAlignment
import com.quadient.migration.shared.TablePdfTaggingRule

class TableBuilder {
    private val rows = mutableListOf<Row>()
    private var header = mutableListOf<Row>()
    private var firstHeader = mutableListOf<Row>()
    private var footer = mutableListOf<Row>()
    private var lastFooter = mutableListOf<Row>()
    private val columnWidths = mutableListOf<ColumnWidth>()
    private var pdfTaggingRule: TablePdfTaggingRule = TablePdfTaggingRule.Default
    private var pdfAlternateText: String? = null
    private var minWidth: Size? = null
    private var maxWidth: Size? = null
    private var percentWidth: Double? = null
    private var border: BorderOptions? = null
    private var alignment: TableAlignment = TableAlignment.Left

    fun pdfTaggingRule(rule: TablePdfTaggingRule) = apply { this.pdfTaggingRule = rule }
    fun pdfAlternateText(text: String?) = apply { this.pdfAlternateText = text }
    fun minWidth(size: Size) = apply { this.minWidth = size }
    fun maxWidth(size: Size) = apply { this.maxWidth = size }
    fun percentWidth(percent: Double) = apply { this.percentWidth = percent }
    fun alignment(alignment: TableAlignment) = apply { this.alignment = alignment }
    fun border(init: BorderOptionsBuilder.() -> Unit) = apply { this.border = BorderOptionsBuilder().apply(init).build() }

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

    fun addHeaderRow() = Row().apply { header.add(this) }
    fun addFirstHeaderRow() = Row().apply { firstHeader.add(this) }
    fun addFooterRow() = Row().apply { footer.add(this) }
    fun addLastFooterRow() = Row().apply { lastFooter.add(this) }

    fun build(): Table {
        return Table(
            rows = rows.map(Row::build),
            header = header.map(Row::build),
            firstHeader = firstHeader.map(Row::build),
            footer = footer.map(Row::build),
            lastFooter = lastFooter.map(Row::build),
            columnWidths = columnWidths.map { Table.ColumnWidth(it.minWidth, it.percentWidth) },
            pdfTaggingRule = pdfTaggingRule,
            pdfAlternateText = pdfAlternateText,
            minWidth = minWidth,
            maxWidth = maxWidth,
            percentWidth = percentWidth,
            border = border,
            alignment = alignment,
        )
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

        fun build(): Table.Row {
            return Table.Row(cells = cells.map { cell ->
                Table.Cell(
                    content = cell.content,
                    mergeUp = cell.mergeUp,
                    mergeLeft = cell.mergeLeft,
                    height = cell.height,
                    border = cell.border,
                    alignment = cell.alignment,
                )
            }, displayRuleRef = displayRuleRef)
        }
    }

    class Cell : DocumentContentBuilderBase<Cell> {
        override val content = mutableListOf<DocumentContent>()
        var mergeLeft = false
        var mergeUp = false
        var height: CellHeight? = null
        var border: BorderOptions? = null
        var alignment: CellAlignment? = null

        fun mergeLeft(value: Boolean) = apply { mergeLeft = value }
        fun mergeUp(value: Boolean) = apply { mergeUp = value }
        fun heightFixed(size: Size) = apply { height = CellHeight.Fixed(size) }
        fun heightCustom(minHeight: Size, maxHeight: Size) = apply { height = CellHeight.Custom(minHeight, maxHeight) }
        fun alignment(alignment: CellAlignment) = apply { this.alignment = alignment }
        fun border(init: BorderOptionsBuilder.() -> Unit) = apply { this.border = BorderOptionsBuilder().apply(init).build() }
    }

    data class ColumnWidth(val minWidth: Size, val percentWidth: Double)
}
