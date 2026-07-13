package com.quadient.migration.service

import com.quadient.migration.api.dto.migrationmodel.Area
import com.quadient.migration.api.dto.migrationmodel.DocumentContent
import com.quadient.migration.api.dto.migrationmodel.FirstMatch
import com.quadient.migration.api.dto.migrationmodel.RepeatedContent
import com.quadient.migration.api.dto.migrationmodel.SelectByLanguage
import com.quadient.migration.api.dto.migrationmodel.Table

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
