package com.quadient.migration.data

import com.quadient.migration.persistence.migrationmodel.TableEntity
import com.quadient.migration.service.RefValidatable
import com.quadient.migration.shared.Size

data class TableModel(
    val rows: List<RowModel>,
    val columnWidths: List<ColumnWidthModel>,
) : DocumentContentModel, TextContentModel, RefValidatable {

    companion object {
        fun fromDb(table: TableEntity): TableModel = TableModel(rows = table.rows.map { row ->
            RowModel(row.cells.map { cell ->
                CellModel(
                    cell.content.map { DocumentContentModel.fromDbContent(it) },
                    cell.mergeLeft,
                    cell.mergeUp,
                )
            }, displayRuleRef = row.displayRuleRef?.let { DisplayRuleModelRef.fromDb(it) })
        }, columnWidths = table.columnWidths.map { ColumnWidthModel(it.minWidth, it.percentWidth) })
    }

    override fun collectRefs(): List<RefModel> {
        return rows.flatMap { row ->
            row.cells.flatMap { cell ->
                cell
                    .content
                    .map { it.collectRefs() }
                    .flatten() + (row.displayRuleRef?.let { listOf(it) } ?: emptyList())
            }
        }
    }

    data class RowModel(val cells: List<CellModel>, val displayRuleRef: DisplayRuleModelRef?)

    data class CellModel(
        val content: List<DocumentContentModel>,
        var mergeLeft: Boolean = false,
        var mergeUp: Boolean = false,
    )

    data class ColumnWidthModel(val minWidth: Size, val percentWidth: Double)
}
