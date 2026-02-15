package com.quadient.migration.api.dto.migrationmodel.builder

import com.quadient.migration.api.dto.migrationmodel.DisplayRuleRef
import com.quadient.migration.api.dto.migrationmodel.DocumentContent
import com.quadient.migration.api.dto.migrationmodel.Table
import com.quadient.migration.api.dto.migrationmodel.builder.documentcontent.BorderOptionsBuilder
import com.quadient.migration.shared.BorderOptions
import com.quadient.migration.shared.CellAlignment
import com.quadient.migration.shared.CellHeight
import com.quadient.migration.shared.TablePdfTaggingRule
import com.quadient.migration.shared.Size
import com.quadient.migration.shared.TableAlignment

object Dsl {
    @JvmStatic
    fun table(init: TableDsl.() -> Unit) = TableDsl().apply(init).run {
        Table(
            rows = rows.map(TableDsl.Row::build),
            header = header.map(TableDsl.Row::build),
            firstHeader = firstHeader.map(TableDsl.Row::build),
            footer = footer.map(TableDsl.Row::build),
            lastFooter = lastFooter.map(TableDsl.Row::build),
            columnWidths = columnWidths.map { Table.ColumnWidth(it.minWidth, it.percentWidth) },
            pdfTaggingRule = pdfTaggingRule,
            pdfAlternateText = pdfAlternateText,
            minWidth = minWidth,
            maxWidth = maxWidth,
            percentWidth = percentWidth,
            border = border,
            alignment = alignment
        )
    }
}

@TableDocumentContentDsl
class TableDsl {
    val rows = mutableListOf<Row>()
    var header = mutableListOf<Row>()
    var firstHeader = mutableListOf<Row>()
    var footer = mutableListOf<Row>()
    var lastFooter = mutableListOf<Row>()
    var columnWidths = mutableListOf<ColumnWidth>()
    var pdfTaggingRule: TablePdfTaggingRule = TablePdfTaggingRule.Default
    var pdfAlternateText: String? = null
    var minWidth: Size? = null
    var maxWidth: Size? = null
    var percentWidth: Double? = null
    var border: BorderOptions? = null
    var alignment: TableAlignment = TableAlignment.Left

    fun pdfTaggingRule(rule: TablePdfTaggingRule) = apply { this.pdfTaggingRule = rule }
    fun pdfAlternateText(text: String?) = apply { this.pdfAlternateText = text }
    fun minWidth(size: Size) = apply { this.minWidth = size }
    fun maxWidth(size: Size) = apply { this.maxWidth = size }
    fun percentWidth(percent: Double) = apply { this.percentWidth = percent }
    fun alignment(alignment: TableAlignment) = apply { this.alignment = alignment }

    /**
     * Add a row to the table. Rows are added in the order they are defined.
     * And all rows must have the same number of cells.
     */
    fun row(init: Row.() -> Unit) = Row().apply(init).apply(rows::add)
    fun addColumnWidth(minWidth: Size, percentWidth: Double) = apply {
        columnWidths.add(ColumnWidth(minWidth, percentWidth))
    }
    fun columnWidths(widths: List<ColumnWidth>) = columnWidths.apply { clear() }.addAll(widths)

    fun headerRow(init: Row.() -> Unit) = Row().apply(init).apply(header::add)
    fun firstHeaderRow(init: Row.() -> Unit) = Row().apply(init).apply(firstHeader::add)
    fun footerRow(init: Row.() -> Unit) = Row().apply(init).apply(footer::add)
    fun lastFooterRow(init: Row.() -> Unit) = Row().apply(init).apply(lastFooter::add)

    fun border(init: BorderOptionsBuilder.() -> Unit) = apply { this.border = BorderOptionsBuilder().apply(init).build() }

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

    @TableDocumentContentDsl
    class Cell {
        val content = mutableListOf<DocumentContent>()
        var mergeLeft = false
        var mergeUp = false
        var height: CellHeight? = null
        var border: BorderOptions? = null
        var alignment: CellAlignment? = null

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

        fun heightFixed(size: Size) = apply { height = CellHeight.Fixed(size) }
        fun heightCustom(minHeight: Size, maxHeight: Size) = apply { height = CellHeight.Custom(minHeight, maxHeight) }
        fun alignment(alignment: CellAlignment) = apply { this.alignment = alignment }
        fun border(init: BorderOptionsBuilder.() -> Unit) = apply { this.border = BorderOptionsBuilder().apply(init).build() }
    }

    data class ColumnWidth(val minWidth: Size, val percentWidth: Double)
}

@DslMarker
annotation class TableDocumentContentDsl
