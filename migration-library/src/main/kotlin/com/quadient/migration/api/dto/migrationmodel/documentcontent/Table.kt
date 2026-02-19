package com.quadient.migration.api.dto.migrationmodel

import com.quadient.migration.persistence.migrationmodel.TableEntity
import com.quadient.migration.shared.BorderOptions
import com.quadient.migration.shared.CellAlignment
import com.quadient.migration.shared.CellHeight
import com.quadient.migration.shared.Size
import com.quadient.migration.shared.TableAlignment
import com.quadient.migration.shared.TablePdfTaggingRule

data class Table(
    val rows: List<Row>,
    val header: List<Row> = emptyList(),
    val firstHeader: List<Row> = emptyList(),
    val footer: List<Row> = emptyList(),
    val lastFooter: List<Row> = emptyList(),
    val columnWidths: List<ColumnWidth>,
    val pdfTaggingRule: TablePdfTaggingRule = TablePdfTaggingRule.Default,
    val pdfAlternateText: String? = null,
    val minWidth: Size? = null,
    val maxWidth: Size? = null,
    val percentWidth: Double? = null,
    val border: BorderOptions? = null,
    val alignment: TableAlignment = TableAlignment.Left
) : DocumentContent, TextContent, RefValidatable {
    override fun collectRefs(): List<Ref> {
        return (rows + header + firstHeader + footer + lastFooter).flatMap { it.collectRefs() }
    }

    companion object {
        fun fromDb(table: TableEntity): Table = Table(
            rows = table.rows.map(Row::fromDb),
            header = table.header.map(Row::fromDb),
            firstHeader = table.firstHeader.map(Row::fromDb),
            footer = table.footer.map(Row::fromDb),
            lastFooter = table.lastFooter.map(Row::fromDb),
            columnWidths = table.columnWidths.map { ColumnWidth(it.minWidth, it.percentWidth) },
            pdfTaggingRule = table.pdfTaggingRule,
            pdfAlternateText = table.pdfAlternateText,
            minWidth = table.minWidth,
            maxWidth = table.maxWidth,
            percentWidth = table.percentWidth,
            border = table.border,
            alignment = table.alignment,
        )
    }

    fun toDb(): TableEntity {
        return TableEntity(
            rows = rows.map(Row::toDb),
            header = header.map(Row::toDb),
            firstHeader = firstHeader.map(Row::toDb),
            footer = footer.map(Row::toDb),
            lastFooter = lastFooter.map(Row::toDb),
            columnWidths = columnWidths.map { TableEntity.ColumnWidthEntity(it.minWidth, it.percentWidth) },
            pdfTaggingRule = pdfTaggingRule,
            pdfAlternateText = pdfAlternateText,
            minWidth = minWidth,
            maxWidth = maxWidth,
            percentWidth = percentWidth,
            border = border,
            alignment = alignment,
        )
    }

    data class Row(val cells: List<Cell>, val displayRuleRef: DisplayRuleRef? = null) : RefValidatable {
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
