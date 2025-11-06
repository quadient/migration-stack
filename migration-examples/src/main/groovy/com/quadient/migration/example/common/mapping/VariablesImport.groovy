//! ---
//! displayName: Import Variable Structure
//! category: Mapping
//! description: Imports variable structure from specified CSV files. The import is interactive, prompting the user to select variable structure CSV to import.
//! target: gradle
//! stdin: true
//! ---
package com.quadient.migration.example.common.mapping

import com.quadient.migration.api.Migration
import com.quadient.migration.api.dto.migrationmodel.VariableRef
import com.quadient.migration.example.common.util.Csv
import com.quadient.migration.example.common.util.Mapping
import com.quadient.migration.shared.DataType
import com.quadient.migration.shared.VariablePathData

import java.nio.file.Path

import static com.quadient.migration.example.common.util.InitMigration.initMigration

def migration = initMigration(this.binding)

def selectedFilePath = new Mapping().getVariablesMappingPath(this.binding.variables["args"], migration.projectConfig.name)
run(migration, selectedFilePath)

static void run(Migration migration, Path path) {
    def lines = path.toFile().readLines()
    def columnNames = Csv.parseColumnNames(lines.removeFirst())
    def structureId = Mapping.variableStructureIdFromFileName(path.fileName.toString(), migration.projectConfig.name)
    def structureMapping = migration.mappingRepository.getVariableStructureMapping(structureId)

    def languageVariableFound = false
    for (line in lines) {
        def values = Csv.getCells(line, columnNames)
        def id = Csv.deserialize(values.get("id"), String.class)

        def variablePathData = structureMapping.mappings[id] ?: new VariablePathData("", null)

        def newName = Csv.deserialize(values.get("inspire_name"), String.class)
        if (newName != null) {
            variablePathData.name = newName
        }

        def inspirePath = Csv.deserialize(values.get("inspire_path"), String.class)
        if (inspirePath != null) {
            variablePathData.path = inspirePath
        }

        if (variablePathData.name != null || variablePathData.path != "") {
            structureMapping.mappings[id] = variablePathData
        }

        def mapping = migration.mappingRepository.getVariableMapping(id)

        def variable = migration.variableRepository.find(id)
        def dataType = Csv.deserialize(values.get("data_type"), DataType.class)
        Mapping.mapProp(mapping, variable, "dataType", dataType)
        def variableName = Csv.deserialize(values.get("name"), String.class)
        Mapping.mapProp(mapping, variable, "name", variableName)

        if (values.get("language_variable")?.trim() == "true") {
            structureMapping.languageVariable = new VariableRef(id)
            languageVariableFound = true
        }

        migration.mappingRepository.upsert(id, mapping)
        migration.mappingRepository.applyVariableMapping(id)
    }

    if (!languageVariableFound) {
        structureMapping.languageVariable = null
    }

    migration.mappingRepository.upsert(structureId, structureMapping)
    migration.mappingRepository.applyVariableStructureMapping(structureId)
}