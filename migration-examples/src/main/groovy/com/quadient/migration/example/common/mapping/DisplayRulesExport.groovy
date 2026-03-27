//! ---
//! displayName: Export Display Rules
//! category: Mapping
//! description: Creates CSV files with display rule details from the migration project. The generated CSV columns can be updated and later imported back into the database using a dedicated import task.
//! target: gradle
//! ---
package com.quadient.migration.example.common.mapping

import com.quadient.migration.api.Migration
import com.quadient.migration.api.dto.migrationmodel.builder.VariableStructureBuilder
import com.quadient.migration.example.common.util.Csv
import com.quadient.migration.example.common.util.Mapping
import com.quadient.migration.service.deploy.ResourceType

import static com.quadient.migration.example.common.util.InitMigration.initMigration

def migration = initMigration(this.binding)

def displayRulePath = Mapping.csvPath(binding, migration.projectConfig.name, "display-rules")

run(migration, displayRulePath.toFile())

static void run(Migration migration, File displayRuleDstPath) {
    def displayRules = migration.displayRuleRepository.listAll()
    def variableStructure
    if (migration.projectConfig.defaultVariableStructure != null) {
        variableStructure = migration
            .variableStructureRepository
            .findOrFail(migration.projectConfig.defaultVariableStructure)
    } else {
        variableStructure = new VariableStructureBuilder("dummy").structure([:]).build()
    }

    displayRuleDstPath.createParentDirectories()

    displayRuleDstPath.withWriter { wr ->
        def headers = ["id", "name", "internal", "baseTemplate", "targetFolder", "targetId", "variableStructureRef", "status", Mapping.displayHeader("originalName", true), Mapping.displayHeader("originLocations", true), Mapping.displayHeader("definition", true)]

        wr << headers.join(",") << "\n"

        for (rule in displayRules) {
            def status = migration.statusTrackingRepository.findLastEventRelevantToOutput(rule.id,
                ResourceType.DisplayRule,
                migration.projectConfig.inspireOutput)

            def ruleVarStructure = rule.variableStructureRef
                ? migration.variableStructureRepository.findOrFail(rule.variableStructureRef.id)
                : variableStructure

            def sb = new StringBuilder()
            sb << "${Csv.serialize(rule.id)}"
            sb << ",${Csv.serialize(rule.name)}"
            sb << ",${Csv.serialize(rule.internal)}"
            sb << ",${Csv.serialize(rule.baseTemplate)}"
            sb << ",${Csv.serialize(rule.targetFolder)}"
            sb << ",${Csv.serialize(rule.targetId?.id)}"
            sb << ",${Csv.serialize(rule.variableStructureRef?.id)}"
            sb << ",${Csv.serialize(status?.class?.simpleName)}"
            sb << ",${Csv.serialize(rule.customFields["originalName"])}"
            sb << ",${Csv.serialize(rule.originLocations)}"
            sb << ",${Csv.serialize(rule.definition?.toCode(ruleVarStructure, migration.variableRepository::findOrFail))}"

            wr << sb.toString() << "\n"
        }
    }
}

