package com.quadient.migration.api.dto.migrationmodel

import com.quadient.migration.persistence.migrationmodel.TableEntity
import com.quadient.migration.persistence.migrationmodel.TableEntity.ColumnWidthEntity
import com.quadient.migration.shared.Size
import com.quadient.migration.shared.TablePdfTaggingRule

data class Table(
    val rows: List<Row>,
    val columnWidths: List<ColumnWidth>,
    val pdfTaggingRule: TablePdfTaggingRule = TablePdfTaggingRule.Default,
    val pdfAlternateText: String? = null,
) : DocumentContent, TextContent, RefValidatable {
    override fun collectRefs(): List<Ref> {
        return rows.flatMap { it.collectRefs() }
    }

    companion object {
        fun fromDb(table: TableEntity): Table = Table(
            rows = table.rows.map { row ->
                Row(row.cells.map { cell ->
                    Cell(
                        cell.content.map { DocumentContent.fromDbContent(it) },
                        cell.mergeLeft,
                        cell.mergeUp,
                    )
                }, displayRuleRef = row.displayRuleRef?.let { DisplayRuleRef.fromDb(it) })
            },
            columnWidths = table.columnWidths.map { ColumnWidth(it.minWidth, it.percentWidth) },
            pdfTaggingRule = table.pdfTaggingRule,
            pdfAlternateText = table.pdfAlternateText
        )
    }

    fun toDb(): TableEntity {
        return TableEntity(
            rows = rows.map { row ->
                TableEntity.Row(row.cells.map { cell ->
                    TableEntity.Cell(
                        cell.content.toDb(),
                        cell.mergeLeft,
                        cell.mergeUp,
                    )
                }, displayRuleRef = row.displayRuleRef?.toDb())
            },
            columnWidths = columnWidths.map { ColumnWidthEntity(it.minWidth, it.percentWidth) },
            pdfTaggingRule = pdfTaggingRule,
            pdfAlternateText = pdfAlternateText
        )
    }

    data class Row(val cells: List<Cell>, val displayRuleRef: DisplayRuleRef? = null) : RefValidatable {
        override fun collectRefs(): List<Ref> {
            return cells.flatMap { it.collectRefs() } + listOfNotNull(displayRuleRef)
        }
    }

    data class Cell(
        val content: List<DocumentContent>,
        val mergeLeft: Boolean,
        val mergeUp: Boolean,
    ) : RefValidatable {
        override fun collectRefs(): List<Ref> {
            return content.flatMap {
                when (it) {
                    is RefValidatable -> it.collectRefs()
                    else -> emptyList()
                }
            }
        }
    }

    data class ColumnWidth(val minWidth: Size, val percentWidth: Double)
}
