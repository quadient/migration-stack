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
    def type = Csv.deserialize(values.get("type"), String.class)

    if (type == DocumentObjectType.Page.toString()) {
        if (currentPage != null) {
            migration.documentObjectRepository.upsert(currentPage)
        }

        def id = Csv.deserialize(values.get("id"), String.class)

        def pageModel = migration.documentObjectRepository.find(id)
        currentPage = pageModel
        areaIndex = 0
    } else if (type == "Area") {
        def currentArea = currentPage.content.findAll { it instanceof Area }[areaIndex] as Area
        def interactiveFlowName = Csv.deserialize(values.get("interactiveFlowName"), String.class)
        currentArea.interactiveFlowName = interactiveFlowName

        areaIndex++
    }
}

if (currentPage != null) {
    migration.documentObjectRepository.upsert(currentPage)
}