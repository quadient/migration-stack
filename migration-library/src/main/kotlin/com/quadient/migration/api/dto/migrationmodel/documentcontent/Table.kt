package com.quadient.migration.api.dto.migrationmodel

import com.quadient.migration.data.TableModel
import com.quadient.migration.persistence.migrationmodel.TableEntity
import com.quadient.migration.persistence.migrationmodel.TableEntity.ColumnWidthEntity
import com.quadient.migration.shared.Size

data class Table(
    val rows: List<Row>,
    val columnWidths: List<ColumnWidth>,
) : DocumentContent, TextContent {
    companion object {
        fun fromModel(model: TableModel): Table = Table(rows = model.rows.map { row ->
            Row(row.cells.map { cell ->
                Cell(
                    cell.content.map { DocumentContent.fromModelContent(it) },
                    cell.mergeLeft,
                    cell.mergeUp,
                )
            }, displayRuleRef = row.displayRuleRef?.let { DisplayRuleRef.fromModel(it) })
        }, columnWidths = model.columnWidths.map { ColumnWidth(it.minWidth, it.percentWidth) })
    }

    fun toDb(): TableEntity {
        return TableEntity(rows = rows.map { row ->
            TableEntity.Row(row.cells.map { cell ->
                TableEntity.Cell(
                    cell.content.toDb(),
                    cell.mergeLeft,
                    cell.mergeUp,
                )
            }, displayRuleRef = row.displayRuleRef?.toDb())
        }, columnWidths = columnWidths.map { ColumnWidthEntity(it.minWidth, it.percentWidth) })
    }

    data class Row(val cells: List<Cell>, val displayRuleRef: DisplayRuleRef? = null)

    data class Cell(
        val content: List<DocumentContent>,
        val mergeLeft: Boolean,
        val mergeUp: Boolean,
    )

    data class ColumnWidth(val minWidth: Size, val percentWidth: Double)
}
