package com.quadient.migration.api.dto.migrationmodel.builder

import com.quadient.migration.api.dto.migrationmodel.DisplayRuleRef
import com.quadient.migration.api.dto.migrationmodel.DocumentContent
import com.quadient.migration.api.dto.migrationmodel.Table
import com.quadient.migration.api.dto.migrationmodel.Variable
import com.quadient.migration.api.dto.migrationmodel.VariableRef
import com.quadient.migration.api.dto.migrationmodel.builder.documentcontent.BorderOptionsBuilder
import com.quadient.migration.api.dto.migrationmodel.TableRow as TableRowModel
import com.quadient.migration.shared.BorderOptions
import com.quadient.migration.shared.CellAlignment
import com.quadient.migration.shared.CellHeight
import com.quadient.migration.shared.VariablePath
import com.quadient.migration.shared.LiteralPath
import com.quadient.migration.shared.Size
import com.quadient.migration.shared.TableAlignment
import com.quadient.migration.shared.TablePdfTaggingRule
import com.quadient.migration.shared.VariableRefPath

@DslMarker
annotation class TableBuilderDsl

@TableBuilderDsl
class TableBuilder : RowBuilderBase<TableBuilder> {
    override val rows = mutableListOf<TableRow>()
    private var header = mutableListOf<TableRow>()
    private var firstHeader = mutableListOf<TableRow>()
    private var footer = mutableListOf<TableRow>()
    private var lastFooter = mutableListOf<TableRow>()
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
    fun border(init: BorderOptionsBuilder.() -> Unit) =
        apply { this.border = BorderOptionsBuilder().apply(init).build() }

    /**
     * Add a repeated row group to the table. The rows added to the builder will be repeated
     * for each element of the given variable.
     * @param variable The literal path or variable reference driving repetition.
     */
    fun addRepeatedRow(variable: VariablePath) = RepeatedRowBuilder(variable).apply { rows.add(this) }

    /**
     * Add a repeated row group to the table and configure it via [init].
     * @param variable The literal path or variable reference driving repetition.
     * @return This builder instance for method chaining.
     */
    fun addRepeatedRow(variable: VariablePath, init: RepeatedRowBuilder.() -> Unit): TableBuilder =
        apply { rows.add(RepeatedRowBuilder(variable).apply(init)) }

    /**
     * Add a repeated row group driven by a literal path (e.g. "Data.Clients").
     */
    fun addRepeatedRow(literalPath: String) = addRepeatedRow(LiteralPath(literalPath))

    /**
     * Add a repeated row group driven by a literal path and configure it via [init].
     * @return This builder instance for method chaining.
     */
    fun addRepeatedRow(literalPath: String, init: RepeatedRowBuilder.() -> Unit) =
        addRepeatedRow(LiteralPath(literalPath), init)

    /**
     * Add a repeated row group driven by a registered variable reference.
     */
    fun addRepeatedRow(variableRef: VariableRef) = addRepeatedRow(VariableRefPath(variableRef.id))

    /**
     * Add a repeated row group driven by a registered variable reference and configure it via [init].
     * @return This builder instance for method chaining.
     */
    fun addRepeatedRow(variableRef: VariableRef, init: RepeatedRowBuilder.() -> Unit) =
        addRepeatedRow(VariableRefPath(variableRef.id), init)

    /**
     * Add a repeated row group driven by a [Variable] object.
     */
    fun addRepeatedRow(variable: Variable) = addRepeatedRow(VariableRefPath(variable.id))

    /**
     * Add a repeated row group driven by a [Variable] object and configure it via [init].
     * @return This builder instance for method chaining.
     */
    fun addRepeatedRow(variable: Variable, init: RepeatedRowBuilder.() -> Unit) =
        addRepeatedRow(VariableRefPath(variable.id), init)

    /**
     * Add a column width to the table. Column widths are added in the order they are defined.
     */
    fun addColumnWidth(minWidth: Size, percentWidth: Double) = apply {
        columnWidths.add(ColumnWidth(minWidth, percentWidth))
    }

    /**
     * Set the column widths for the table. This will replace any existing column widths.
     * @param width The list of column widths to set.
     * @return The builder instance for method chaining.
     */
    fun columnWidths(width: List<ColumnWidth>) = apply {
        columnWidths.clear()
        columnWidths.addAll(width)
    }

    fun addHeaderRow() = Row().also { header.add(it) }
    fun addHeaderRow(init: Row.() -> Unit): TableBuilder = apply { header.add(Row().apply(init)) }
    fun addHeaderRow(row: Row) = apply { header.add(row) }
    fun addFirstHeaderRow() = Row().also { firstHeader.add(it) }
    fun addFirstHeaderRow(init: Row.() -> Unit): TableBuilder = apply { firstHeader.add(Row().apply(init)) }
    fun addFirstHeaderRow(row: Row) = apply { firstHeader.add(row) }
    fun addFooterRow() = Row().also { footer.add(it) }
    fun addFooterRow(init: Row.() -> Unit): TableBuilder = apply { footer.add(Row().apply(init)) }
    fun addFooterRow(row: Row) = apply { footer.add(row) }
    fun addLastFooterRow() = Row().also { lastFooter.add(it) }
    fun addLastFooterRow(init: Row.() -> Unit): TableBuilder = apply { lastFooter.add(Row().apply(init)) }
    fun addLastFooterRow(row: Row) = apply { lastFooter.add(row) }

    fun addRepeatedHeaderRow(variable: VariablePath) = RepeatedRowBuilder(variable).also { header.add(it) }
    fun addRepeatedHeaderRow(variable: VariablePath, init: RepeatedRowBuilder.() -> Unit): TableBuilder =
        apply { header.add(RepeatedRowBuilder(variable).apply(init)) }
    fun addRepeatedFirstHeaderRow(variable: VariablePath) = RepeatedRowBuilder(variable).also { firstHeader.add(it) }
    fun addRepeatedFirstHeaderRow(variable: VariablePath, init: RepeatedRowBuilder.() -> Unit): TableBuilder =
        apply { firstHeader.add(RepeatedRowBuilder(variable).apply(init)) }
    fun addRepeatedFooterRow(variable: VariablePath) = RepeatedRowBuilder(variable).also { footer.add(it) }
    fun addRepeatedFooterRow(variable: VariablePath, init: RepeatedRowBuilder.() -> Unit): TableBuilder =
        apply { footer.add(RepeatedRowBuilder(variable).apply(init)) }
    fun addRepeatedLastFooterRow(variable: VariablePath) = RepeatedRowBuilder(variable).also { lastFooter.add(it) }
    fun addRepeatedLastFooterRow(variable: VariablePath, init: RepeatedRowBuilder.() -> Unit): TableBuilder =
        apply { lastFooter.add(RepeatedRowBuilder(variable).apply(init)) }

    fun build(): Table {
        return Table(
            rows = rows.map(TableRow::build),
            header = header.map(TableRow::build),
            firstHeader = firstHeader.map(TableRow::build),
            footer = footer.map(TableRow::build),
            lastFooter = lastFooter.map(TableRow::build),
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

    /** Builder row type — sealed to allow both [Row] and [RepeatedRowBuilder] in the same list. */
    sealed interface TableRow {
        fun build(): TableRowModel
    }

    @TableBuilderDsl
    class Row : TableRow {
        val cells = mutableListOf<Cell>()
        var displayRuleRef: DisplayRuleRef? = null

        /**
         * Add a cell to the row. Cells are added in the order they are defined.
         * All rows must have the same number of cells.
         */
        fun addCell() = Cell().also { cells.add(it) }

        /**
         * Creates a new [Cell], configures it via [init], appends it, and returns this row.
         * @return The row instance for method chaining.
         */
        fun addCell(init: Cell.() -> Unit): Row = apply { cells.add(Cell().apply(init)) }

        /**
         * Appends a pre-configured [cell] to the row.
         * @return The row instance for method chaining.
         */
        fun addCell(cell: Cell) = apply { cells.add(cell) }

        /**
         * Adds multiple cells to the row. This will append the cells to any existing cells.
         * @param cells The list of cells to add.
         * @return The builder instance for method chaining.
         */
        fun addCells(cells: List<Cell>) = apply {
            this.cells.addAll(cells)
        }

        fun displayRuleRef(id: String) = this.apply { this.displayRuleRef = DisplayRuleRef(id) }
        fun displayRuleRef(ref: DisplayRuleRef) = this.apply { this.displayRuleRef = ref }

        override fun build(): Table.Row {
            return Table.Row(cells = cells.map { it.build() }, displayRuleRef = displayRuleRef)
        }
    }

    @TableBuilderDsl
    class RepeatedRowBuilder(private val variable: VariablePath) : TableRow, RowBuilderBase<RepeatedRowBuilder> {
        override val rows = mutableListOf<TableRow>()

        override fun build(): Table.RepeatedRow {
            return Table.RepeatedRow(rows = rows.map { (it as Row).build() }, variable)
        }
    }

    @TableBuilderDsl
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
        fun border(init: BorderOptionsBuilder.() -> Unit) =
            apply { this.border = BorderOptionsBuilder().apply(init).build() }

        fun build(): Table.Cell {
            return Table.Cell(
                content = content,
                mergeUp = mergeUp,
                mergeLeft = mergeLeft,
                height = height,
                border = border,
                alignment = alignment,
            )
        }
    }

    data class ColumnWidth(val minWidth: Size, val percentWidth: Double)
}

/**
 * Common interface for builders that manage a collection of [TableBuilder.Row] entries.
 * Implemented by both [TableBuilder] and [TableBuilder.RepeatedRowBuilder].
 */
@Suppress("UNCHECKED_CAST")
@TableBuilderDsl
interface RowBuilderBase<T> {
    val rows: MutableList<TableBuilder.TableRow>

    /** Creates a new [TableBuilder.Row], appends it, and returns it for further configuration. */
    fun addRow(): TableBuilder.Row = TableBuilder.Row().also { rows.add(it) }

    /**
     * Creates a new [TableBuilder.Row], configures it via [init], appends it, and returns this builder.
     * @return This builder instance for method chaining.
     */
    fun addRow(init: TableBuilder.Row.() -> Unit): T = apply { rows.add(TableBuilder.Row().apply(init)) } as T

    /**
     * Appends a pre-configured [row] to this container.
     * @return This builder instance for method chaining.
     */
    fun addRow(row: TableBuilder.Row): T = apply { rows.add(row) } as T

    /**
     * Appends multiple pre-configured [rows] to this container.
     * @return This builder instance for method chaining.
     */
    fun addRows(rows: List<TableBuilder.Row>): T = apply { this.rows.addAll(rows) } as T
}