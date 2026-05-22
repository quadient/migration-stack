//! ---
//! displayName: Import Areas
//! category: Mapping
//! description: Import areas with modified interactive flow names to their respective pages and templates
//! target: gradle
//! ---
package com.quadient.migration.example.common.mapping

import com.quadient.migration.api.Migration
import com.quadient.migration.api.dto.migrationmodel.DocumentObject
import com.quadient.migration.api.dto.migrationmodel.MappingItem
import com.quadient.migration.example.common.util.Csv
import com.quadient.migration.example.common.util.Mapping
import groovy.transform.Field
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.nio.file.Path

import static com.quadient.migration.example.common.util.InitMigration.initMigration

@Field static Logger log = LoggerFactory.getLogger(this.class.name)

def migration = initMigration(this.binding)
def areasFile = Mapping.csvPath(binding, migration.projectConfig.name, "areas")

run(migration, areasFile)

static void run(Migration migration, Path path) {
    def fileLines = path.toFile().readLines()
    def columnNames = Csv.parseColumnNames(fileLines.removeFirst()).collect { Mapping.normalizeHeader(it) }

    def mappings = new HashMap<String, MappingItem>()
    DocumentObject currentDocumentObject = null
    MappingItem.Area mapping = null
    int areaIndex = 0
    for (line in fileLines) {
        def values = Csv.getCells(line, columnNames)

        def pageId = Csv.deserialize(values.get("pageId"), String.class)
        def templateId = Csv.deserialize(values.get("templateId"), String.class)
        def documentObjectId = pageId ?: templateId

        if (currentDocumentObject?.id != documentObjectId) {
            if (currentDocumentObject != null) {
                mappings[currentDocumentObject.id] = mapping
            }

            def documentObjectModel = migration.documentObjectRepository.find(documentObjectId)
            if (!documentObjectModel) {
                throw new IllegalStateException("Document object '${documentObjectId}' not found.")
            }

            mapping = migration.mappingRepository.getAreaMapping(documentObjectId)
            currentDocumentObject = documentObjectModel
            areaIndex = 0
        }

        def interactiveFlowName = Csv.deserialize(values.get("interactiveFlowName"), String.class)
        mapping.areas[areaIndex] = interactiveFlowName

        def flowToNextPage = Csv.deserialize(values.get("flowToNextPage"), Boolean.class)
        mapping.flowToNextPage[areaIndex] = flowToNextPage ?: false

        areaIndex++
    }

    if (currentDocumentObject != null) {
        mappings[currentDocumentObject.id] = mapping
    }

    def batches = mappings.entrySet().collate(1000)
    for (int i = 0; i < batches.size(); i++) {
        log.info("Upserting mappings batch ${i + 1}/${batches.size()} (${batches[i].size()} items)")
        migration.mappingRepository.upsertBatch(batches[i].collectEntries())
    }
    migration.mappingRepository.applyAllAreaMappings()
}

