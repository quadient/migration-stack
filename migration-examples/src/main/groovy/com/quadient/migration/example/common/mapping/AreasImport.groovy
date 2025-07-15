package com.quadient.migration.example.common.mapping

import com.quadient.migration.api.dto.migrationmodel.Area
import com.quadient.migration.api.dto.migrationmodel.DocumentObject
import com.quadient.migration.example.common.util.Csv
import com.quadient.migration.shared.DocumentObjectType

import java.nio.file.Paths

import static com.quadient.migration.example.common.util.InitMigration.initMigration

def migration = initMigration(this.binding.variables["args"])

def areasFile = Paths.get("mapping", "${migration.projectConfig.name}-areas.csv").toFile()

def fileLines = areasFile.readLines()
def columnNames = fileLines.removeFirst().split(",")

DocumentObject currentPage = null
int areaIndex = 0
for (line in fileLines) {
    def values = Csv.getCells(line, columnNames)

    def pageId = Csv.deserialize(values.get("pageId"), String.class)

    if (currentPage?.id != pageId) {
        if (currentPage != null) {
            migration.documentObjectRepository.upsert(currentPage)
        }

        def pageModel = migration.documentObjectRepository.find(pageId)
        if (!pageModel) {
            throw new IllegalStateException("Page '${pageId}' not found.")
        }

        currentPage = pageModel
        areaIndex = 0
    }

    def currentArea = currentPage.content.findAll { it instanceof Area }[areaIndex] as Area
    def interactiveFlowName = Csv.deserialize(values.get("interactiveFlowName"), String.class)
    currentArea.interactiveFlowName = interactiveFlowName

    areaIndex++
}

if (currentPage != null) {
    migration.documentObjectRepository.upsert(currentPage)
}
