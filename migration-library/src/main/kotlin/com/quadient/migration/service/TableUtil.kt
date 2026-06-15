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

data class DocumentTable(val contentPath: String, val table: Table)

fun collectDocumentTables(content: List<DocumentContent>): List<DocumentTable> {
    val result = mutableListOf<DocumentTable>()
    val typeCounters = mutableMapOf<String, Int>()

    content.forEach { item ->
        when (item) {
            is Table -> {
                val idx = typeCounters.getOrDefault("table", 0).also { typeCounters["table"] = it + 1 }
                result.add(DocumentTable("table:$idx", item))
            }
            is Area -> {
                val idx = typeCounters.getOrDefault("area", 0).also { typeCounters["area"] = it + 1 }
                collectContainerTables(item.content, "area:$idx", result)
            }
            is RepeatedContent -> {
                val idx = typeCounters.getOrDefault("repeatedContent", 0).also { typeCounters["repeatedContent"] = it + 1 }
                collectContainerTables(item.content, "repeatedContent:$idx", result)
            }
            is FirstMatch -> {
                val idx = typeCounters.getOrDefault("firstMatch", 0).also { typeCounters["firstMatch"] = it + 1 }
                collectContainerTables(item.cases.flatMap { it.content } + item.default, "firstMatch:$idx", result)
            }
            is SelectByLanguage -> {
                val idx = typeCounters.getOrDefault("selectByLanguage", 0).also { typeCounters["selectByLanguage"] = it + 1 }
                collectContainerTables(item.cases.flatMap { it.content }, "selectByLanguage:$idx", result)
            }
            else -> {}
        }
    }

    return result
}

private fun collectContainerTables(content: List<DocumentContent>, containerPath: String, result: MutableList<DocumentTable>) {
    var tableIdx = 0
    content.forEach { item ->
        if (item is Table) result.add(DocumentTable("$containerPath/table:${tableIdx++}", item))
    }
}

fun computeFingerprint(table: Table): String {
    val colCount = table.columnWidths.size
    val altText = table.pdfAlternateText?.take(50)
    val repeatedVariable = repeatedVariable(table)
    val firstRowPreview = extractFirstRowPreview(table)

    return buildString {
        append("${colCount}cols")
        if (table.name != null) append("|name:${table.name}")
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
        append("$colCount cols")
        if (repeatedVariable != null) append(" | repeatedBy: $repeatedVariable")
        else append(" | $bodyRowCount body rows")
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
