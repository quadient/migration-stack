package com.quadient.migration.api.dto.migrationmodel

import com.quadient.migration.data.TableModel
import com.quadient.migration.persistence.migrationmodel.TableEntity
import com.quadient.migration.persistence.migrationmodel.TableEntity.ColumnWidthEntity
import com.quadient.migration.shared.Size
import com.quadient.migration.shared.TablePdfTaggingRule

data class Table(
    val rows: List<Row>,
    val columnWidths: List<ColumnWidth>,
    val pdfTaggingRule: TablePdfTaggingRule = TablePdfTaggingRule.Default,
    val pdfAlternateText: String? = null,
) : DocumentContent, TextContent {
    companion object {
        fun fromModel(model: TableModel): Table = Table(
            rows = model.rows.map { row ->
                Row(row.cells.map { cell ->
                    Cell(
                        cell.content.map { DocumentContent.fromModelContent(it) },
                        cell.mergeLeft,
                        cell.mergeUp,
                    )
                }, displayRuleRef = row.displayRuleRef?.let { DisplayRuleRef.fromModel(it) })
            },
            columnWidths = model.columnWidths.map { ColumnWidth(it.minWidth, it.percentWidth) },
            pdfTaggingRule = model.pdfTaggingRule,
            pdfAlternateText = model.pdfAlternateText
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

    data class Row(val cells: List<Cell>, val displayRuleRef: DisplayRuleRef? = null)

    data class Cell(
        val content: List<DocumentContent>,
        val mergeLeft: Boolean,
        val mergeUp: Boolean,
    )

    data class ColumnWidth(val minWidth: Size, val percentWidth: Double)
}
