//! ---
//! displayName: Import Variable Structure
//! category: Mapping
//! description: Imports variable structure from specified CSV files. The import is interactive, prompting the user to select variable structure CSV to import.
//! target: gradle
//! stdin: true
//! ---
package com.quadient.migration.example.common.mapping

import com.quadient.migration.api.Migration
import com.quadient.migration.api.dto.migrationmodel.MappingItem
import com.quadient.migration.api.dto.migrationmodel.VariableRef
import com.quadient.migration.example.common.util.Csv
import com.quadient.migration.example.common.util.Mapping
import com.quadient.migration.shared.DataType
import com.quadient.migration.shared.LiteralPath
import com.quadient.migration.shared.VariablePathData
import com.quadient.migration.shared.VariableRefPath

import java.nio.file.Path

import groovy.transform.Field
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import static com.quadient.migration.example.common.util.InitMigration.initMigration

@Field static Logger log = LoggerFactory.getLogger(this.class.name)

def migration = initMigration(this.binding)

def selectedFilePath = new Mapping().getVariablesMappingPath(this.binding.variables["args"], migration.projectConfig.name)
run(migration, selectedFilePath)

static void run(Migration migration, Path path) {
    def lines = path.toFile().readLines()
    def columnNames = Csv.parseColumnNames(lines.removeFirst()).collect { Mapping.normalizeHeader(it) }
    def total = lines.size()
    def structureId = Mapping.variableStructureIdFromFileName(path.fileName.toString(), migration.projectConfig.name)
    def structureMapping = migration.mappingRepository.getVariableStructureMapping(structureId)

    def languageVariableFound = false
    def mappings = new HashMap<String, MappingItem>()
    for (line in lines) {
        def values = Csv.getCells(line, columnNames)
        def id = Csv.deserialize(values.get("id"), String.class)

        def variablePathData = structureMapping.mappings[id] ?: new VariablePathData("", null)

        def newName = Csv.deserialize(values.get("inspire_name"), String.class)
        variablePathData.name = newName

        def inspirePathRaw = Csv.deserialize(values.get("inspire_path"), String.class)
        variablePathData.path = parseVariablePath(inspirePathRaw)

        structureMapping.mappings[id] = variablePathData

        def mapping = migration.mappingRepository.getVariableMapping(id)

        def variable = migration.variableRepository.find(id)
        def dataType = Csv.deserialize(values.get("data_type"), DataType.class)
        Mapping.mapProp(mapping, variable, "dataType", dataType)
        def variableName = Csv.deserialize(values.get("name"), String.class)
        Mapping.mapProp(mapping, variable, "name", variableName)

        if (Csv.deserialize(values.get("language_variable")?.trim(), Boolean.class) == true) {
            structureMapping.languageVariable = new VariableRef(id)
            languageVariableFound = true
        }

        mappings[id] = mapping
        if (total > 1000 && mappings.size() % 1000 == 0) {
            log.info("Processed ${mappings.size()}/${total} mappings")
        }
    }

    def batches = mappings.entrySet().collate(1000)
    for (int i = 0; i < batches.size(); i++) {
        log.info("Upserting mappings batch ${i + 1}/${batches.size()} (${batches[i].size()} items)")
        migration.mappingRepository.upsertBatch(batches[i].collectEntries())
    }
    migration.mappingRepository.applyAllVariableMappings()

    if (!languageVariableFound) {
        structureMapping.languageVariable = null
    }

    migration.mappingRepository.upsert(structureId, structureMapping)
    migration.mappingRepository.applyVariableStructureMapping(structureId)
}

static parseVariablePath(String raw) {
    if (!raw) return new LiteralPath("")
    if (raw.startsWith("@") || raw.startsWith("\$")) return new VariableRefPath(raw.substring(1))
    return new LiteralPath(raw)
}