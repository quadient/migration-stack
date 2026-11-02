//! ---
//! displayName: Import Variable Structures
//! category: Mapping
//! description: Imports variable structure details (name and target folder of the deployed data definition) from CSV files into the migration project, applying any updates made to the columns during editing.
//! target: gradle
//! ---
package com.quadient.migration.example.common.mapping

import com.quadient.migration.api.Migration
import com.quadient.migration.api.dto.migrationmodel.MappingItem
import com.quadient.migration.example.common.util.Csv
import com.quadient.migration.example.common.util.Mapping
import com.quadient.migration.service.deploy.utility.ResourceType

import groovy.transform.Field
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import static com.quadient.migration.example.common.util.InitMigration.initMigration

@Field static Logger log = LoggerFactory.getLogger(this.class.name)

def migration = initMigration(this.binding)

def variableStructuresPath = Mapping.csvPath(binding, migration.projectConfig.name, migration.projectConfig.subProjectId, "variable-structures")

run(migration, variableStructuresPath.toFile())

static void run(Migration migration, File file) {
    def deploymentId = UUID.randomUUID().toString()
    def now = new Date().getTime()
    def output = migration.projectConfig.inspireOutput

    def lines = file.readLines()
    def columnNames = Csv.parseColumnNames(lines.removeFirst()).collect { Mapping.normalizeHeader(it) }
    def total = lines.size()

    def mappings = new HashMap<String, MappingItem>()
    for (line in lines) {
        def values = Csv.getCells(line, columnNames)
        def id = values.get("id")

        def existingStructure = migration.variableStructureRepository.find(id)
        if (existingStructure == null) {
            throw new Exception("VariableStructure with id ${id} not found")
        }
        def existingMapping = migration.mappingRepository.getVariableStructureMapping(id)

        // The structure content is owned by the variables mapping, keep it as it is in the object
        if (!existingMapping.mappings) existingMapping.mappings = new HashMap(existingStructure.structure)
        if (existingMapping.languageVariable == null) existingMapping.languageVariable = existingStructure.languageVariable

        def status = migration.statusTrackingRepository.findLastEventRelevantToOutput(existingStructure.id,
            ResourceType.VariableStructure,
            migration.projectConfig.inspireOutput)

        def newName = Csv.deserialize(values.get("name"), String.class)
        existingMapping.name = newName

        def newTargetFolder = Csv.deserialize(values.get("targetFolder"), String.class)
        existingMapping.targetFolder = newTargetFolder

        def csvStatus = values.get("status")
        if ((csvStatus == null || csvStatus == "") && status == null) {
            migration.statusTrackingRepository.active(existingStructure.id, ResourceType.VariableStructure, [reason: "Manual"])
        } else if (csvStatus == "Active" && status?.class?.simpleName != "Active") {
            migration.statusTrackingRepository.active(existingStructure.id, ResourceType.VariableStructure, [reason: "Manual"])
        } else if (csvStatus == "Deployed" && status?.class?.simpleName != "Deployed") {
            migration.statusTrackingRepository.deployed(existingStructure.id, deploymentId, now, ResourceType.VariableStructure, null, output, [reason: "Manual"])
        }

        mappings[id] = existingMapping
        if (total > 1000 && mappings.size() % 1000 == 0) {
            log.info "Processed ${mappings.size()}/${total} mappings"
        }
    }

    Mapping.upsertBatched(migration.mappingRepository, mappings, "variable structure mappings", log)
    migration.mappingRepository.applyAllVariableStructureMappings()
}
