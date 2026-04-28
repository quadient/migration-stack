//! ---
//! displayName: Import Display Rules
//! category: Mapping
//! description: Imports display rule details from CSV files into the migration project, applying any updates made to the columns during editing.
//! target: gradle
//! ---
package com.quadient.migration.example.common.mapping

import com.quadient.migration.api.Migration
import com.quadient.migration.example.common.util.Csv
import com.quadient.migration.example.common.util.Mapping
import com.quadient.migration.service.deploy.utility.ResourceType

import static com.quadient.migration.example.common.util.InitMigration.initMigration

def migration = initMigration(this.binding)

def displayRulePath = Mapping.csvPath(binding, migration.projectConfig.name, "display-rules")

run(migration, displayRulePath.toFile())

static void run(Migration migration, File file) {
    def deploymentId = UUID.randomUUID().toString()
    def now = new Date().getTime()
    def output = migration.projectConfig.inspireOutput

    def lines = file.readLines()
    def columnNames = Csv.parseColumnNames(lines.removeFirst()).collect { Mapping.normalizeHeader(it) }

    for (line in lines) {
        def values = Csv.getCells(line, columnNames)
        def id = values.get("id")

        def existingRule = migration.displayRuleRepository.find(id)
        if (existingRule == null) {
            throw new Exception("DisplayRule with id ${id} not found")
        }
        def existingMapping = migration.mappingRepository.getDisplayRuleMapping(id)

        def status = migration.statusTrackingRepository.findLastEventRelevantToOutput(existingRule.id,
            ResourceType.DisplayRule,
            migration.projectConfig.inspireOutput)

        def newName = Csv.deserialize(values.get("name"), String.class)
        existingMapping.name = newName

        def newInternal = Csv.deserialize(values.get("internal"), boolean)
        existingMapping.internal = newInternal

        def newBaseTemplate = Csv.deserialize(values.get("baseTemplate"), String.class)
        existingMapping.baseTemplate = newBaseTemplate

        def newTargetFolder = Csv.deserialize(values.get("targetFolder"), String.class)
        existingMapping.targetFolder = newTargetFolder

        def newTargetId = Csv.deserialize(values.get("targetId"), String.class)
        existingMapping.targetId = newTargetId

        def newVariableStructureId = Csv.deserialize(values.get("variableStructureRef"), String.class)
        existingMapping.variableStructureRef = newVariableStructureId

        def csvStatus = values.get("status")
        if ((csvStatus == null || csvStatus == "") && status == null) {
            migration.statusTrackingRepository.active(existingRule.id, ResourceType.DisplayRule, [reason: "Manual"])
        } else if (csvStatus == "Active" && status?.class?.simpleName != "Active") {
            migration.statusTrackingRepository.active(existingRule.id, ResourceType.DisplayRule, [reason: "Manual"])
        } else if (csvStatus == "Deployed" && status?.class?.simpleName != "Deployed") {
            migration.statusTrackingRepository.deployed(existingRule.id, deploymentId, now, ResourceType.DisplayRule, null, output, [reason: "Manual"])
        }

        migration.mappingRepository.upsert(id, existingMapping)
        migration.mappingRepository.applyDisplayRuleMapping(id)
    }
}
