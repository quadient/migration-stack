package com.quadient.migration.example.common.mapping

import com.quadient.migration.api.Migration
import com.quadient.migration.example.common.util.Csv
import com.quadient.migration.example.common.util.Mapping
import com.quadient.migration.shared.DataType

import java.nio.file.Path

import static com.quadient.migration.example.common.util.InitMigration.initMigration

def migration = initMigration(this.binding.variables["args"])

def selectedFilePath = new Mapping().getVariablesMappingPath(this.binding.variables["args"], migration.projectConfig.name)
run(migration, selectedFilePath)

static void run(Migration migration, Path path) {
    def lines = path.toFile().readLines()
    def columnNames = lines.removeFirst().split(",")

    for (line in lines) {
        def values = Csv.getCells(line, columnNames)
        def id = values.get("id")

        def mapping = migration.mappingRepository.getVariableMapping(id)

        def inspirePath = values.get("inspire_path")
        if (inspirePath != null && inspirePath != "") {
            mapping.inspirePath = inspirePath
        } else {
            mapping.inspirePath = null
        }

        def variable = migration.variableRepository.find(values.get("id"))

        def newName = values.get("name")
        Mapping.mapProp(mapping, variable, "name", newName)

        def dataType = Csv.deserialize(values.get("data_type"), DataType.class)
        Mapping.mapProp(mapping, variable, "dataType", dataType)

        def structureId = Mapping.variableStructureIdFromFileName(path.fileName.toString(), migration.projectConfig.name)
        migration.mappingRepository.upsert(id, mapping)
        migration.mappingRepository.applyVariableMapping(id, structureId)
    }
}