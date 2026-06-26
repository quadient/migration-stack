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
import com.quadient.migration.api.dto.migrationmodel.VariableStringContent
import com.quadient.migration.shared.VariableRefPath

data class DocumentTable(val contentPath: String, val table: Table)

fun collectDocumentTables(content: List<DocumentContent>): List<DocumentTable> {
    val result = mutableListOf<DocumentTable>()
    val locations = mutableMapOf<String, Int>()

    content.forEach { item ->
        val (pathPart, children) = when (item) {
            is Table -> locations.next(item.pathName) to null
            is Area -> locations.next(item.pathName) to item.content
            is RepeatedContent -> locations.next(item.pathName) to item.content
            is FirstMatch -> locations.next(item.pathName) to (item.cases.flatMap { it.content } + item.default)
            is SelectByLanguage -> locations.next(item.pathName) to item.cases.flatMap { it.content }
            else -> return@forEach
        }
        if (children == null) {
            result.add(DocumentTable(pathPart, item as Table))
        } else {
            val containerLocations = mutableMapOf<String, Int>()
            children.filterIsInstance<Table>().forEach { table ->
                val tablePath = containerLocations.next(table.pathName)
                result.add(DocumentTable(listOf(pathPart, tablePath).joinToString("/"), table))
            }
        }
    }

    return result
}

private fun MutableMap<String, Int>.next(name: String): String {
    val idx = getOrDefault(name, 0).also { this[name] = it + 1 }
    return "$name:$idx"
}

fun computeFingerprint(table: Table): String = buildString {
    append("${table.columnWidths.size}cols")
    tableRepeatedVariable(table)?.let { append("|repeatedBy:$it") }
    tableFirstRowPreview(table)?.let { append("|$it") }
}

fun buildContentPreview(table: Table): String {
    val colCount = table.columnWidths.size
    val bodyRowCount = table.rows.count { it is Table.Row }
    val repeatedVariable = tableRepeatedVariable(table)
    val firstRowPreview = tableFirstRowPreview(table, " | ")

    return buildString {
        append("$colCount cols")
        if (repeatedVariable != null) append(" | repeatedBy: $repeatedVariable")
        else append(" | $bodyRowCount body rows")
        if (table.firstHeader.isNotEmpty()) append(" | firstHeader: $firstRowPreview")
        else if (firstRowPreview != null) append(" | row0: $firstRowPreview")
    }
}

fun tableRepeatedVariable(table: Table): String? = table.rows.filterIsInstance<Table.RepeatedRow>()
    .firstOrNull()?.variable?.let { if (it is VariableRefPath) it.variableId else it.toString() }

fun tableFirstRowPreview(table: Table, separator: String = "|"): String? {
    val firstRow =
        (table.firstHeader.firstOrNull() ?: table.header.firstOrNull() ?: table.rows.filterIsInstance<Table.Row>()
            .firstOrNull()) as? Table.Row ?: return null

    return firstRow.cells.joinToString(separator) { cell -> extractCellText(cell).replace('\r', ' ').replace('\n', ' ').take(30) }.take(100)
}

private fun extractCellText(cell: Table.Cell): String =
    cell.content.asSequence()
        .flatMap { item ->
            when (item) {
                is Paragraph -> item.content.asSequence().flatMap { it.content.asSequence() }
                is VariableStringContent -> sequenceOf(item)
                else -> emptySequence()
            }
        }
        .joinToString("") { textContent ->
            when (textContent) {
                is StringValue -> textContent.value
                is VariableRef -> "var:${textContent.id}"
                else -> ""
            }
        }
