//! ---
//! displayName: Export Base Templates
//! category: Mapping
//! description: Creates CSV files with base template details from the migration project. The generated CSV columns can be updated and later imported back into the database using a dedicated import task.
//! target: gradle
//! ---
package com.quadient.migration.example.common.mapping

import com.quadient.migration.api.Migration
import com.quadient.migration.api.dto.migrationmodel.BaseTemplate
import com.quadient.migration.example.common.util.Csv
import com.quadient.migration.example.common.util.Mapping
import com.quadient.migration.service.deploy.utility.ResourceType

import static com.quadient.migration.example.common.util.InitMigration.initMigration

def migration = initMigration(this.binding)

def baseTemplatePath = Mapping.csvPath(binding, migration.projectConfig.name, migration.projectConfig.subProjectId, "base-templates")

run(migration, baseTemplatePath.toFile())

static void run(Migration migration, File baseTemplateDstPath) {
    List<BaseTemplate> baseTemplates = Mapping.collectSelectedOrAll(migration, BaseTemplate) { migration.baseTemplateRepository.listAll() }

    baseTemplateDstPath.createParentDirectories()

    baseTemplateDstPath.withWriter { writer ->
        def headers = ["id", "name", "targetFolder", "variableStructureRef", "status", Mapping.displayHeader("originLocations", true)]
        writer.writeLine(headers.join(","))
        baseTemplates.each { obj ->
            def status = migration.statusTrackingRepository.findLastEventRelevantToOutput(obj.id,
                    ResourceType.BaseTemplate,
                    migration.projectConfig.inspireOutput)

            def builder = new StringBuilder()
            builder.append(Csv.serialize(obj.id))
            builder.append("," + Csv.serialize(obj.name))
            builder.append("," + Csv.serialize(obj.targetFolder))
            builder.append("," + Csv.serialize(obj.variableStructureRef?.id))
            builder.append("," + Csv.serialize(status?.class?.simpleName))
            builder.append("," + Csv.serialize(obj.originLocations))

            writer.writeLine(builder.toString())
        }
    }
}
