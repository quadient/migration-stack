//! ---
//! displayName: Export Tables
//! category: Mapping
//! description: Export tables from document objects. Outputs one CSV row per table, in document order. Edit the action and PDF properties, then re-import.
//! target: gradle
//! ---
package com.quadient.migration.example.common.mapping

import com.quadient.migration.api.Migration
import com.quadient.migration.api.repository.DocumentObjectRepository
import com.quadient.migration.example.common.util.Csv
import com.quadient.migration.example.common.util.Mapping
import com.quadient.migration.service.DocumentTable
import groovy.transform.Field

import java.nio.file.Path

import static com.quadient.migration.example.common.util.InitMigration.initMigration
import static com.quadient.migration.service.TableUtilKt.*

@Field Migration migration = initMigration(this.binding)

def tablesFile = Mapping.csvPath(binding, migration.projectConfig.name, "tables")

run(migration, tablesFile)

static void run(Migration migration, Path path) {
    def tablesFile = path.toFile()
    tablesFile.createParentDirectories()

    def documentObjects = (migration.documentObjectRepository as DocumentObjectRepository).listAll()
            .sort { it.name ?: it.id }

    tablesFile.withWriter { writer ->
        def headers = [
            Mapping.displayHeader("documentObjectId", false),
            Mapping.displayHeader("documentObjectName", true),
            Mapping.displayHeader("contentPath", false),
            Mapping.displayHeader("contentPreview", true),
            Mapping.displayHeader("tableName", false),
            Mapping.displayHeader("pdfTaggingRule", false),
            Mapping.displayHeader("pdfAlternateText", false),
            Mapping.displayHeader("action", false),
        ]
        writer.writeLine(headers.join(","))

        documentObjects.each { docObj ->
            def documentTables = collectDocumentTables(docObj.content)
            documentTables.each { docTable ->
                writer.writeLine(buildRow(docObj.id, docObj.name, docTable))
            }
        }
    }
}

static String buildRow(String docObjId, String docObjName, DocumentTable docTable) {
    def contentPath = docTable.contentPath
    def table = docTable.table

    def builder = new StringBuilder()
    builder.append(Csv.serialize(docObjId) + ",")
    builder.append(Csv.serialize(docObjName) + ",")
    builder.append(Csv.serialize(contentPath) + ",")
    builder.append(Csv.serialize(buildContentPreview(table)) + ",")
    builder.append(Csv.serialize(table.name) + ",")
    builder.append(Csv.serialize(table.pdfTaggingRule) + ",")
    builder.append(Csv.serialize(table.pdfAlternateText) + ",")
    builder.append(Csv.serialize(table.action))
    return builder.toString()
}
