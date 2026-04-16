package com.quadient.migration.api.dto.migrationmodel

import com.quadient.migration.persistence.migrationmodel.TableEntity
import com.quadient.migration.shared.BorderOptions
import com.quadient.migration.shared.CellAlignment
import com.quadient.migration.shared.CellHeight
import com.quadient.migration.shared.VariablePath
import com.quadient.migration.shared.Size
import com.quadient.migration.shared.TableAlignment
import com.quadient.migration.shared.TablePdfTaggingRule
import com.quadient.migration.shared.VariableRefPath

/** Sealed type representing either a single table row or a group of rows repeated by an array variable. */
sealed interface TableRow : RefValidatable

data class Table(
    val rows: List<TableRow>,
    val header: List<TableRow> = emptyList(),
    val firstHeader: List<TableRow> = emptyList(),
    val footer: List<TableRow> = emptyList(),
    val lastFooter: List<TableRow> = emptyList(),
    val columnWidths: List<ColumnWidth>,
    val pdfTaggingRule: TablePdfTaggingRule = TablePdfTaggingRule.Default,
    val pdfAlternateText: String? = null,
    val minWidth: Size? = null,
    val maxWidth: Size? = null,
    val percentWidth: Double? = null,
    val border: BorderOptions? = null,
    val alignment: TableAlignment = TableAlignment.Left,
    val tableStyleName: String? = null,
) : DocumentContent, TextContent, RefValidatable {
    override fun collectRefs(): List<Ref> {
        return (rows + header + firstHeader + footer + lastFooter).flatMap { it.collectRefs() }
    }

    companion object {
        fun fromDb(table: TableEntity): Table = Table(
            rows = table.rows.map(::tableRowFromDb),
            header = table.header.map(::tableRowFromDb),
            firstHeader = table.firstHeader.map(::tableRowFromDb),
            footer = table.footer.map(::tableRowFromDb),
            lastFooter = table.lastFooter.map(::tableRowFromDb),
            columnWidths = table.columnWidths.map { ColumnWidth(it.minWidth, it.percentWidth) },
            pdfTaggingRule = table.pdfTaggingRule,
            pdfAlternateText = table.pdfAlternateText,
            minWidth = table.minWidth,
            maxWidth = table.maxWidth,
            percentWidth = table.percentWidth,
            border = table.border,
            alignment = table.alignment,
            tableStyleName = table.tableStyleName,
        )
    }

    fun toDb(): TableEntity {
        return TableEntity(
            rows = rows.map { it.toDb() },
            header = header.map { it.toDb() },
            firstHeader = firstHeader.map { it.toDb() },
            footer = footer.map { it.toDb() },
            lastFooter = lastFooter.map { it.toDb() },
            columnWidths = columnWidths.map { TableEntity.ColumnWidthEntity(it.minWidth, it.percentWidth) },
            pdfTaggingRule = pdfTaggingRule,
            pdfAlternateText = pdfAlternateText,
            minWidth = minWidth,
            maxWidth = maxWidth,
            percentWidth = percentWidth,
            border = border,
            alignment = alignment,
            tableStyleName = tableStyleName,
        )
    }

    data class Row(val cells: List<Cell>, val displayRuleRef: DisplayRuleRef? = null) : TableRow {
        override fun collectRefs(): List<Ref> {
            return cells.flatMap { it.collectRefs() } + listOfNotNull(displayRuleRef)
        }

        fun toDb(): TableEntity.Row {
            return TableEntity.Row(cells.map(Cell::toDb), displayRuleRef = displayRuleRef?.toDb())
        }

        companion object {
            fun fromDb(row: TableEntity.Row): Row {
                return Row(
                    cells = row.cells.map(Cell::fromDb),
                    displayRuleRef = row.displayRuleRef?.let { DisplayRuleRef.fromDb(it) })
            }
        }
    }

    data class RepeatedRow(
        val rows: List<Row>,
        val variable: VariablePath,
    ) : TableRow {
        override fun collectRefs(): List<Ref> {
            val rowRefs = rows.flatMap { it.collectRefs() }
            val varRef = (variable as? VariableRefPath)?.let { VariableRef(it.variableId) }
            return rowRefs + listOfNotNull(varRef)
        }

        fun toDb(): TableEntity.RepeatedRow {
            return TableEntity.RepeatedRow(
                rows = rows.map(Row::toDb),
                variable = variable,
            )
        }

        companion object {
            fun fromDb(row: TableEntity.RepeatedRow): RepeatedRow {
                return RepeatedRow(
                    rows = row.rows.map(Row::fromDb),
                    variable = row.variable,
                )
            }
        }
    }

    data class Cell(
        val content: List<DocumentContent>,
        val mergeLeft: Boolean,
        val mergeUp: Boolean,
        val height: CellHeight?,
        val border: BorderOptions? = null,
        val alignment: CellAlignment? = null,
    ) : RefValidatable {
        override fun collectRefs(): List<Ref> {
            return content.flatMap {
                when (it) {
                    is RefValidatable -> it.collectRefs()
                    else -> emptyList()
                }
            }
        }

        companion object {
            fun fromDb(cell: TableEntity.Cell): Cell {
                return Cell(
                    content = cell.content.map(DocumentContent::fromDbContent),
                    mergeLeft = cell.mergeLeft,
                    mergeUp = cell.mergeUp,
                    height = cell.height,
                    border = cell.border,
                    alignment = cell.alignment,
                )
            }
        }

        fun toDb(): TableEntity.Cell {
            return TableEntity.Cell(
                content = content.toDb(),
                mergeLeft = mergeLeft,
                mergeUp = mergeUp,
                height = height,
                border = border,
                alignment = alignment,
            )
        }
    }

    data class ColumnWidth(val minWidth: Size, val percentWidth: Double)
}

private fun tableRowFromDb(row: TableEntity.TableRow): TableRow = when (row) {
    is TableEntity.Row -> Table.Row.fromDb(row)
    is TableEntity.RepeatedRow -> Table.RepeatedRow.fromDb(row)
}

private fun TableRow.toDb(): TableEntity.TableRow = when (this) {
    is Table.Row -> this.toDb()
    is Table.RepeatedRow -> this.toDb()
}
