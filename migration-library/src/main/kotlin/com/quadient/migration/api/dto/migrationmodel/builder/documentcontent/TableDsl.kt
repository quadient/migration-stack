package com.quadient.migration.api.dto.migrationmodel.builder

import com.quadient.migration.api.dto.migrationmodel.DisplayRuleRef
import com.quadient.migration.api.dto.migrationmodel.DocumentContent
import com.quadient.migration.api.dto.migrationmodel.Table
import com.quadient.migration.shared.PdfTaggingRule
import com.quadient.migration.shared.Size

object Dsl {
    @JvmStatic
    fun table(init: TableDsl.() -> Unit) = TableDsl().apply(init).run {
        Table(rows = rows.map {
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
        }, pdfTaggingRule, pdfAlternateText)
    }
}

@TableDocumentContentDsl
class TableDsl {
    val rows = mutableListOf<Row>()
    var columnWidths = mutableListOf<ColumnWidth>()
    var pdfTaggingRule: PdfTaggingRule = PdfTaggingRule.Default
    var pdfAlternateText: String? = null

    fun pdfTaggingRule(rule: PdfTaggingRule) = apply { this.pdfTaggingRule = rule }
    fun pdfAlternateText(text: String?) = apply { this.pdfAlternateText = text }

    /**
     * Add a row to the table. Rows are added in the order they are defined.
     * And all rows must have the same number of cells.
     */
    fun row(init: Row.() -> Unit) = Row().apply(init).apply(rows::add)
    fun addColumnWidth(minWidth: Size, percentWidth: Double) = apply {
        columnWidths.add(ColumnWidth(minWidth, percentWidth))
    }
    fun columnWidths(widths: List<ColumnWidth>) = columnWidths.apply { clear() }.addAll(widths)

    @TableDocumentContentDsl
    class Row {
        val cells = mutableListOf<Cell>()
        var displayRuleRef: DisplayRuleRef? = null

        /**
         * Add a cell to the row. Cells are added in the order they are defined.
         * And all rows must have the same number of cells.
         */
        fun cell(init: Cell.() -> Unit) = Cell().apply(init).apply(cells::add)
        fun displayRuleRef(id: String) = this.apply { this.displayRuleRef = DisplayRuleRef(id) }
        fun displayRuleRef(ref: DisplayRuleRef) = this.apply { this.displayRuleRef = ref }
    }

    @TableDocumentContentDsl
    class Cell {
        val content = mutableListOf<DocumentContent>()
        var mergeLeft = false
        var mergeUp = false

        /**
         * Append content to the cell.
         * @param content The content to append to the cell. Must be either
         * a [String], [com.quadient.migration.api.dto.migrationmodel.Text]
         * or [com.quadient.migration.api.dto.migrationmodel.Ref].
         */
        fun appendContent(content: DocumentContent) = this@Cell.content.add(content)

        /**
         * Replace content of the cell with single object.
         * @param content The content to append to the cell. Must be either
         * a [String], [com.quadient.migration.api.dto.migrationmodel.Text]
         * or [com.quadient.migration.api.dto.migrationmodel.Ref].
         */
        fun content(content: DocumentContent) = this@Cell.content.apply { clear() }.add(content)

        /**
         * Replace content of the cell with the provided list of objects.
         * @param content The content to append to the cell. Must be either
         * a [String], [com.quadient.migration.api.dto.migrationmodel.Text]
         * or [com.quadient.migration.api.dto.migrationmodel.Ref].
         */
        fun content(content: List<DocumentContent>) = this@Cell.content.apply { clear() }.addAll(content)

        /**
         * Add a paragraph to the cell.
         * @param builder A lambda that builds the paragraph.
         * @return The current [Cell] instance for chaining.
         */
        fun paragraph(builder: ParagraphBuilder.() -> Unit) = apply {
            content.add(ParagraphBuilder().apply(builder).build())
        }
    }

    data class ColumnWidth(val minWidth: Size, val percentWidth: Double)
}

@DslMarker
annotation class TableDocumentContentDsl
