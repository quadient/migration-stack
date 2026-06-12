package com.quadient.migration.service

import com.quadient.migration.api.dto.migrationmodel.Area
import com.quadient.migration.api.dto.migrationmodel.DocumentContent
import com.quadient.migration.api.dto.migrationmodel.FirstMatch
import com.quadient.migration.api.dto.migrationmodel.Paragraph
import com.quadient.migration.api.dto.migrationmodel.RepeatedContent
import com.quadient.migration.api.dto.migrationmodel.SelectByLanguage
import com.quadient.migration.api.dto.migrationmodel.StringValue
import com.quadient.migration.api.dto.migrationmodel.Table
import com.quadient.migration.api.dto.migrationmodel.VariableRef
import com.quadient.migration.shared.VariableRefPath

data class TableLocation(val containerIndex: Int?, val containerType: String?, val tableIndex: Int)

data class DocumentTable(val location: TableLocation, val table: Table)

fun collectDocumentTables(content: List<DocumentContent>): List<DocumentTable> {
    val result = mutableListOf<DocumentTable>()

    var topLevelTableIdx = 0
    content.forEachIndexed { contentIdx, item ->
        when (item) {
            is Table -> result.add(
                DocumentTable(TableLocation(null, null, topLevelTableIdx++), item)
            )

            is Area -> {
                var tableIdx = 0
                item.content.forEach { areaItem ->
                    if (areaItem is Table) {
                        result.add(
                            DocumentTable(
                                TableLocation(contentIdx, item::class.simpleName, tableIdx++), areaItem
                            )
                        )
                    }
                }
            }

            is RepeatedContent -> {
                var tableIdx = 0
                item.content.forEach { rcItem ->
                    if (rcItem is Table) {
                        result.add(
                            DocumentTable(
                                TableLocation(contentIdx, item::class.simpleName, tableIdx++), rcItem
                            )
                        )
                    }
                }
            }

            is FirstMatch -> {
                var tableIdx = 0
                val allContent = item.cases.flatMap { it.content } + item.default
                allContent.forEach { fmItem ->
                    if (fmItem is Table) {
                        result.add(
                            DocumentTable(
                                TableLocation(contentIdx, item::class.simpleName, tableIdx++), fmItem
                            )
                        )
                    }
                }
            }

            is SelectByLanguage -> {
                var tableIdx = 0
                item.cases.flatMap { it.content }.forEach { sblItem ->
                    if (sblItem is Table) {
                        result.add(
                            DocumentTable(
                                TableLocation(contentIdx, item::class.simpleName, tableIdx++), sblItem
                            )
                        )
                    }
                }
            }

            else -> {}
        }
    }

    return result
}

fun computeFingerprint(table: Table): String {
    val colCount = table.columnWidths.size
    val altText = table.pdfAlternateText?.take(50)
    val repeatedVariable = repeatedVariable(table)
    val firstRowPreview = extractFirstRowPreview(table)

    return buildString {
        append("${colCount}cols")
        if (altText != null) append("|altText:$altText")
        if (repeatedVariable != null) append("|repeatedBy:$repeatedVariable")
        if (firstRowPreview != null) append("|$firstRowPreview")
    }
}

fun buildContentPreview(table: Table): String {
    val colCount = table.columnWidths.size
    val bodyRowCount = table.rows.count { it is Table.Row }
    val repeatedVariable = repeatedVariable(table)
    val firstRowPreview = extractFirstRowPreview(table, " | ")

    return buildString {
        append("${colCount}cols")
        if (repeatedVariable != null) append(" | repeatedBy: $repeatedVariable")
        else append(" | $bodyRowCount rows")
        if (table.firstHeader.isNotEmpty()) append(" | firstHeader: $firstRowPreview")
        else if (firstRowPreview != null) append(" | row0: $firstRowPreview")
    }
}

private fun repeatedVariable(table: Table): String? = table.rows.filterIsInstance<Table.RepeatedRow>()
    .firstOrNull()?.variable?.let { if (it is VariableRefPath) it.variableId else it.toString() }

private fun extractFirstRowPreview(table: Table, separator: String = "|"): String? {
    val firstRow =
        (table.firstHeader.firstOrNull() ?: table.header.firstOrNull() ?: table.rows.filterIsInstance<Table.Row>()
            .firstOrNull()) as? Table.Row ?: return null

    return firstRow.cells.joinToString(separator) { cell -> extractCellText(cell).take(30) }.take(100)
}

private fun extractCellText(cell: Table.Cell): String =
    cell.content.asSequence().filterIsInstance<Paragraph>().flatMap { it.content }.flatMap { it.content }
        .joinToString("") { textContent ->
            when (textContent) {
                is StringValue -> textContent.value
                is VariableRef -> "var:${textContent.id}"
                else -> ""
            }
        }
