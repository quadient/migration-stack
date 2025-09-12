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
    def columnNames = Csv.parseColumnNames(fileLines.removeFirst())

    DocumentObject currentPage = null
    def areas = null
    MappingItem.Area mapping = null
    int areaIndex = 0
    for (line in fileLines) {
        def values = Csv.getCells(line, columnNames)

        def pageId = Csv.deserialize(values.get("pageId"), String.class)

        if (currentPage?.id != pageId) {
            if (currentPage != null) {
                migration.mappingRepository.upsert(pageId, mapping)
            }

            def pageModel = migration.documentObjectRepository.find(pageId)
            if (!pageModel) {
                throw new IllegalStateException("Page '${pageId}' not found.")
            }

            areas = pageModel.content.findAll { it instanceof Area } as List<Area>
            mapping = migration.mappingRepository.getAreaMapping(pageId)
            currentPage = pageModel
            areaIndex = 0
        }

        def interactiveFlowName = Csv.deserialize(values.get("interactiveFlowName"), String.class)
        if (interactiveFlowName != null && !interactiveFlowName.empty && interactiveFlowName != mapping.areas.get(areaIndex) && areas[areaIndex].interactiveFlowName != interactiveFlowName) {
            mapping.areas[areaIndex] = interactiveFlowName
        }

        areaIndex++
    }

    if (currentPage != null) {
        migration.mappingRepository.upsert(currentPage.id, mapping)
        migration.mappingRepository.applyAreaMapping(currentPage.id)
    }
}

