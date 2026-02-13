//! ---
//! displayName: Import Areas
//! category: Mapping
//! description: Import areas with modified interactive flow names to their respective pages
//! target: gradle
//! ---
package com.quadient.migration.example.common.mapping

import com.quadient.migration.api.Migration
import com.quadient.migration.api.dto.migrationmodel.Area
import com.quadient.migration.api.dto.migrationmodel.DocumentObject
import com.quadient.migration.api.dto.migrationmodel.MappingItem
import com.quadient.migration.example.common.util.Csv
import com.quadient.migration.example.common.util.Mapping

import java.nio.file.Path

import static com.quadient.migration.example.common.util.InitMigration.initMigration

def migration = initMigration(this.binding)
def areasFile = Mapping.csvPath(binding, migration.projectConfig.name, "areas")

run(migration, areasFile)

static void run(Migration migration, Path path) {
    def fileLines = path.toFile().readLines()
    def columnNames = Csv.parseColumnNames(fileLines.removeFirst()).collect { Mapping.normalizeHeader(it) }

    DocumentObject currentPage = null
    MappingItem.Area mapping = null
    int areaIndex = 0
    for (line in fileLines) {
        def values = Csv.getCells(line, columnNames)

        def pageId = Csv.deserialize(values.get("pageId"), String.class)

        if (currentPage?.id != pageId) {
            if (currentPage != null) {
                migration.mappingRepository.upsert(currentPage.id, mapping)
                migration.mappingRepository.applyAreaMapping(currentPage.id)
            }

            def pageModel = migration.documentObjectRepository.find(pageId)
            if (!pageModel) {
                throw new IllegalStateException("Page '${pageId}' not found.")
            }

            mapping = migration.mappingRepository.getAreaMapping(pageId)
            currentPage = pageModel
            areaIndex = 0
        }

        def interactiveFlowName = Csv.deserialize(values.get("interactiveFlowName"), String.class)
        mapping.areas[areaIndex] = interactiveFlowName

        def flowToNextPage = Csv.deserialize(values.get("flowToNextPage"), Boolean.class)
        mapping.flowToNextPage[areaIndex] = flowToNextPage ?: false

        areaIndex++
    }

    if (currentPage != null) {
        migration.mappingRepository.upsert(currentPage.id, mapping)
        migration.mappingRepository.applyAreaMapping(currentPage.id)
    }
}
