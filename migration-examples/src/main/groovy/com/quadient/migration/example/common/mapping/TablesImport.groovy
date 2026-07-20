//! ---
//! displayName: Import Tables
//! category: Mapping
//! description: Import table mapping from CSV. Computes and stores fingerprints, then applies pdfTaggingRule and pdfAlternateText changes. Fingerprint mismatches are logged as errors and skipped.
//! target: gradle
//! ---
package com.quadient.migration.example.common.mapping

import com.quadient.migration.api.Migration
import com.quadient.migration.api.dto.migrationmodel.MappingItem
import com.quadient.migration.example.common.util.Csv
import com.quadient.migration.example.common.util.Mapping
import com.quadient.migration.shared.TableAction
import com.quadient.migration.shared.TablePdfTaggingRule
import groovy.transform.Field
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.nio.file.Path

import static com.quadient.migration.example.common.util.InitMigration.initMigration
import static com.quadient.migration.service.TableUtilKt.collectDocumentTables

@Field static Logger log = LoggerFactory.getLogger(this.class.name)

def migration = initMigration(this.binding)
def tablesFile = Mapping.csvPath(binding, migration.projectConfig.name, "tables")

run(migration, tablesFile)

static void run(Migration migration, Path path) {
    def fileLines = path.toFile().readLines()
    def columnNames = Csv.parseColumnNames(fileLines.removeFirst()).collect { Mapping.normalizeHeader(it) }

    // Group table entries by documentObjectId so we upsert one MappingItem.Table per document object.
    def tablesByDocObjId = new LinkedHashMap<String, List<MappingItem.Table.TableEntry>>()
    boolean hasErrors = false

    for (line in fileLines) {
        def values = Csv.getCells(line, columnNames)

        def docObjId = Csv.deserialize(values.get("documentObjectId"), String.class)
        if (!docObjId) continue

        def contentPath = Csv.deserialize(values.get("tableId"), String.class)
        if (!contentPath) continue

        def actionStr = Csv.deserialize(values.get("action"), String.class)
        def action = actionStr ? Csv.deserialize(actionStr, TableAction.class) : TableAction.Keep

        def pdfTaggingRuleStr = Csv.deserialize(values.get("pdfTaggingRule"), String.class)
        def pdfTaggingRule = pdfTaggingRuleStr ? Csv.deserialize(pdfTaggingRuleStr, TablePdfTaggingRule.class) : null

        def pdfAlternateText = Csv.deserialize(values.get("pdfAlternateText"), String.class)
        def tableName = Csv.deserialize(values.get("tableName"), String.class)

        def docObj = migration.documentObjectRepository.find(docObjId)
        if (!docObj) {
            log.error("Table mapping: document object '${docObjId}' not found. Skipping row.")
            hasErrors = true
            continue
        }

        def documentTables = collectDocumentTables(docObj.content)
        def docTable = documentTables.find { it.contentPath == contentPath }
        if (!docTable) {
            log.error("Table mapping for '${docObjId}' at [${contentPath}]: table not found. Skipping row.")
            hasErrors = true
            continue
        }

        def fingerprint = docTable.table.computeFingerprint()

        def entry = new MappingItem.Table.TableEntry(
            contentPath,
            action,
            pdfTaggingRule,
            pdfAlternateText,
            fingerprint,
            tableName
        )

        tablesByDocObjId.computeIfAbsent(docObjId) { [] }.add(entry)
    }

    Map<String, MappingItem> mappings = tablesByDocObjId.collectEntries { docObjId, tables ->
        [docObjId, new MappingItem.Table(null, tables)]
    }

    Mapping.upsertBatched(migration.mappingRepository, mappings, "table mappings", log)
    migration.mappingRepository.applyAllTableMappings { hasErrors = true }

    if (hasErrors) {
        System.exit(1)
    }
}
