package com.quadient.migration.api.dto.migrationmodel.builder

import com.quadient.migration.api.dto.migrationmodel.DisplayRule
import com.quadient.migration.api.dto.migrationmodel.DisplayRuleRef
import com.quadient.migration.api.dto.migrationmodel.DocumentContent
import com.quadient.migration.api.dto.migrationmodel.Table
import com.quadient.migration.api.dto.migrationmodel.Variable
import com.quadient.migration.api.dto.migrationmodel.VariableRef
import com.quadient.migration.api.dto.migrationmodel.builder.documentcontent.HasBorder
import com.quadient.migration.api.dto.migrationmodel.TableRow as TableRowModel
import com.quadient.migration.shared.BorderOptions
import com.quadient.migration.shared.CellAlignment
import com.quadient.migration.shared.CellHeight
import com.quadient.migration.shared.CellOverflow
import com.quadient.migration.shared.VariablePath
import com.quadient.migration.shared.LiteralPath
import com.quadient.migration.shared.Size
import com.quadient.migration.shared.TableAction
import com.quadient.migration.shared.TableAlignment
import com.quadient.migration.shared.TablePdfTaggingRule
import com.quadient.migration.shared.VariableRefPath

@DslMarker
annotation class TableBuilderDsl

@TableBuilderDsl
class TableBuilder : RowBuilderBase<TableBuilder>, HasBorder<TableBuilder> {
    override val rows = mutableListOf<TableRowOrBuilder>()
    private var header = mutableListOf<TableRowOrBuilder>()
    private var firstHeader = mutableListOf<TableRowOrBuilder>()
    private var footer = mutableListOf<TableRowOrBuilder>()
    private var lastFooter = mutableListOf<TableRowOrBuilder>()
    private val columnWidths = mutableListOf<ColumnWidth>()
    private var pdfTaggingRule: TablePdfTaggingRule = TablePdfTaggingRule.Default
    private var pdfAlternateText: String? = null
    private var minWidth: Size? = null
    private var maxWidth: Size? = null
    private var percentWidth: Double? = null
    override var border: BorderOptions? = null
    private var alignment: TableAlignment = TableAlignment.Left
    private var tableStyleName: String? = null
    private var action: TableAction = TableAction.Keep
    private var name: String? = null

    fun pdfTaggingRule(rule: TablePdfTaggingRule) = apply { this.pdfTaggingRule = rule }
    fun pdfAlternateText(text: String?) = apply { this.pdfAlternateText = text }
    fun minWidth(size: Size) = apply { this.minWidth = size }
    fun maxWidth(size: Size) = apply { this.maxWidth = size }
    fun percentWidth(percent: Double) = apply { this.percentWidth = percent }
    fun alignment(alignment: TableAlignment) = apply { this.alignment = alignment }
    fun action(action: TableAction) = apply { this.action = action }
    fun name(name: String?) = apply { this.name = name }

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
    fun addRepeatedHeaderRow(repeatedRow: Table.RepeatedRow): TableBuilder = apply {
        header.add(PrebuiltRow(repeatedRow))
    }
    fun addRepeatedFirstHeaderRow(variable: VariablePath) = RepeatedRowBuilder(variable).also { firstHeader.add(it) }
    fun addRepeatedFirstHeaderRow(variable: VariablePath, init: RepeatedRowBuilder.() -> Unit): TableBuilder =
        apply { firstHeader.add(RepeatedRowBuilder(variable).apply(init)) }
    fun addRepeatedFirstHeaderRow(repeatedRow: Table.RepeatedRow): TableBuilder = apply {
        firstHeader.add(PrebuiltRow(repeatedRow))
    }
    fun addRepeatedFooterRow(variable: VariablePath) = RepeatedRowBuilder(variable).also { footer.add(it) }
    fun addRepeatedFooterRow(variable: VariablePath, init: RepeatedRowBuilder.() -> Unit): TableBuilder =
        apply { footer.add(RepeatedRowBuilder(variable).apply(init)) }
    fun addRepeatedFooterRow(repeatedRow: Table.RepeatedRow): TableBuilder = apply {
        footer.add(PrebuiltRow(repeatedRow))
    }
    fun addRepeatedLastFooterRow(variable: VariablePath) = RepeatedRowBuilder(variable).also { lastFooter.add(it) }
    fun addRepeatedLastFooterRow(variable: VariablePath, init: RepeatedRowBuilder.() -> Unit): TableBuilder =
        apply { lastFooter.add(RepeatedRowBuilder(variable).apply(init)) }
    fun addRepeatedLastFooterRow(repeatedRow: Table.RepeatedRow): TableBuilder = apply {
        lastFooter.add(PrebuiltRow(repeatedRow))
    }

    /**
     * Adds a table style to this table. The table style must exist in
     * the style definition for this to work.
     * @param name The name of the style definition.
     * @return The builder instance for method chaining.
     */
    fun tableStyleName(name: String?) = apply {
        this.tableStyleName = name
    }

    fun build(): Table {
        return Table(
            rows = rows.map(TableRowOrBuilder::unwrap),
            header = header.map(TableRowOrBuilder::unwrap),
            firstHeader = firstHeader.map(TableRowOrBuilder::unwrap),
            footer = footer.map(TableRowOrBuilder::unwrap),
            lastFooter = lastFooter.map(TableRowOrBuilder::unwrap),
            columnWidths = columnWidths.map { Table.ColumnWidth(it.minWidth, it.percentWidth) },
            pdfTaggingRule = pdfTaggingRule,
            pdfAlternateText = pdfAlternateText,
            minWidth = minWidth,
            maxWidth = maxWidth,
            percentWidth = percentWidth,
            border = border,
            alignment = alignment,
            tableStyleName = tableStyleName,
            action = action,
            name = name,
        )
    }


    @JvmInline
    value class PrebuiltRow(val row: TableRowModel) : TableRowOrBuilder

    sealed interface TableRow : TableRowOrBuilder {
        fun build(): TableRowModel
    }

    @TableBuilderDsl
    class Row : TableRow {
        val cells = mutableListOf<CellOrBuilder>()
        var displayRuleRef: DisplayRuleRef? = null

        /**
         * Sets cells of the rows. This will replace the existing cells.
         * All rows must have the same number of cells.
         * @return The row instance for method chaining.
         */
        fun cells(cells: List<Table.Cell>) = apply {
            this.cells.clear()
            this.cells.addAll(cells.map { PrebuiltCell(it) })
        }

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
        @Deprecated("Overloads which take other builders are not supported anymore and will be removed in the future")
        fun addCell(cell: Cell) = apply { cells.add(cell) }

        /**
         * Appends a pre-configured [cell] to the row.
         * @return The row instance for method chaining.
         */
        fun addCell(cell: Table.Cell) = apply { cells.add(PrebuiltCell(cell)) }

        /**
         * Adds multiple cells to the row. This will append the cells to any existing cells.
         * @param cells The list of cells to add.
         * @return The builder instance for method chaining.
         */
        fun addCells(cells: List<Table.Cell>) = apply {
            this.cells.addAll(cells.map { PrebuiltCell(it) })
        }


        fun displayRuleRef(id: String) = this.apply { this.displayRuleRef = DisplayRuleRef(id) }
        fun displayRuleRef(ref: DisplayRuleRef) = this.apply { this.displayRuleRef = ref }
        fun displayRuleRef(rule: DisplayRule) = this.apply { this.displayRuleRef = DisplayRuleRef(rule.id) }

        override fun build(): Table.Row {
            return Table.Row(cells = cells.map {
                when (it) {
                    is Cell -> it.build()
                    is PrebuiltCell -> it.cell
                }
            }, displayRuleRef = displayRuleRef)
        }
    }

    @TableBuilderDsl
    class RepeatedRowBuilder(private val variable: VariablePath) : TableRow, RowBuilderBase<RepeatedRowBuilder> {
        override val rows = mutableListOf<TableRowOrBuilder>()
        var displayRuleRef: DisplayRuleRef? = null

        fun displayRuleRef(id: String) = this.apply { this.displayRuleRef = DisplayRuleRef(id) }
        fun displayRuleRef(ref: DisplayRuleRef) = this.apply { this.displayRuleRef = ref }
        fun displayRuleRef(rule: DisplayRule) = this.apply { this.displayRuleRef = DisplayRuleRef(rule.id) }

        override fun build(): Table.RepeatedRow {
            return Table.RepeatedRow(rows = rows.map { it.unwrap() }, variable = variable, displayRuleRef = displayRuleRef)
        }
    }

    sealed interface CellOrBuilder

    @JvmInline
    value class PrebuiltCell(val cell: Table.Cell) : CellOrBuilder

    @TableBuilderDsl
    class Cell : CellOrBuilder, DocumentContentBuilderBase<Cell>, HasBorder<Cell> {
        override val content = mutableListOf<DocumentContent>()
        var mergeLeft = false
        var mergeUp = false
        var height: CellHeight? = null
        override var border: BorderOptions? = null
        var alignment: CellAlignment? = null
        var overflow: CellOverflow? = null

        fun mergeLeft(value: Boolean) = apply { mergeLeft = value }
        fun mergeUp(value: Boolean) = apply { mergeUp = value }
        fun heightFixed(size: Size) = apply { height = CellHeight.Fixed(size) }
        fun heightCustom(minHeight: Size, maxHeight: Size) = apply { height = CellHeight.Custom(minHeight, maxHeight) }
        fun alignment(alignment: CellAlignment) = apply { this.alignment = alignment }
        fun overflow(overflow: CellOverflow) = apply { this.overflow = overflow }

        fun build(): Table.Cell {
            return Table.Cell(
                content = content,
                mergeUp = mergeUp,
                mergeLeft = mergeLeft,
                height = height,
                border = border,
                alignment = alignment,
                overflow = overflow,
            )
        }
    }

    data class ColumnWidth(val minWidth: Size, val percentWidth: Double)
}

/**
 * Common interface for builders that manage a collection of [TableBuilder.TableRow] entries.
 * Implemented by both [TableBuilder] and [TableBuilder.RepeatedRowBuilder].
 */
@Suppress("UNCHECKED_CAST")
@TableBuilderDsl
interface RowBuilderBase<T> {
    val rows: MutableList<TableRowOrBuilder>

    /**
     * Sets rows to the provided list of [newRows]. This will replace any existing rows.
     * @return This builder instance for method chaining.
     */
    fun rows(newRows: List<TableBuilder.TableRow>): T = apply {
        rows.clear()
        rows.addAll(newRows)
    } as T

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

    /**
     * Add a repeated row group. The rows added to the builder will be repeated
     * for each element of the given variable.
     * @param variable The literal path or variable reference driving repetition.
     */
    fun addRepeatedRow(variable: VariablePath): TableBuilder.RepeatedRowBuilder =
        TableBuilder.RepeatedRowBuilder(variable).also { rows.add(it) }

    /**
     * Add a repeated row group and configure it via [init].
     * @param variable The literal path or variable reference driving repetition.
     * @return This builder instance for method chaining.
     */
    fun addRepeatedRow(variable: VariablePath, init: TableBuilder.RepeatedRowBuilder.() -> Unit): T =
        apply { rows.add(TableBuilder.RepeatedRowBuilder(variable).apply(init)) } as T

    /**
     * Add an existing repeated row group.
     * @param repeatedRow The [Table.RepeatedRow] instance to add.
     * @return This builder instance for method chaining.
     */
    fun addRepeatedRow(repeatedRow: Table.RepeatedRow): T = apply { rows.add(TableBuilder.PrebuiltRow(repeatedRow)) } as T

    /**
     * Add a repeated row group driven by a literal path (e.g. "Data.Clients").
     */
    fun addRepeatedRow(literalPath: String): TableBuilder.RepeatedRowBuilder =
        addRepeatedRow(LiteralPath(literalPath))

    /**
     * Add a repeated row group driven by a literal path and configure it via [init].
     * @return This builder instance for method chaining.
     */
    fun addRepeatedRow(literalPath: String, init: TableBuilder.RepeatedRowBuilder.() -> Unit): T =
        addRepeatedRow(LiteralPath(literalPath), init)

    /**
     * Add a repeated row group driven by a registered variable reference.
     */
    fun addRepeatedRow(variableRef: VariableRef): TableBuilder.RepeatedRowBuilder =
        addRepeatedRow(VariableRefPath(variableRef.id))

    /**
     * Add a repeated row group driven by a registered variable reference and configure it via [init].
     * @return This builder instance for method chaining.
     */
    fun addRepeatedRow(variableRef: VariableRef, init: TableBuilder.RepeatedRowBuilder.() -> Unit): T =
        addRepeatedRow(VariableRefPath(variableRef.id), init)

    /**
     * Add a repeated row group driven by a [Variable] object.
     */
    fun addRepeatedRow(variable: Variable): TableBuilder.RepeatedRowBuilder =
        addRepeatedRow(VariableRefPath(variable.id))

    /**
     * Add a repeated row group driven by a [Variable] object and configure it via [init].
     * @return This builder instance for method chaining.
     */
    fun addRepeatedRow(variable: Variable, init: TableBuilder.RepeatedRowBuilder.() -> Unit): T =
        addRepeatedRow(VariableRefPath(variable.id), init)
}

sealed interface TableRowOrBuilder {
    fun unwrap(): TableRowModel {
        return when (this) {
            is TableBuilder.PrebuiltRow -> this.row
            is TableBuilder.RepeatedRowBuilder -> this.build()
            is TableBuilder.Row -> this.build()
        }
    }
}