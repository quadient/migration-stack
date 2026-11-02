//! ---
//! displayName: Export Variable Structures
//! category: Mapping
//! description: Creates a CSV file with variable structure details (name and target folder of the deployed data definition) from the migration project. The generated CSV columns can be updated and later imported back into the database using a dedicated import task.
//! target: gradle
//! ---
package com.quadient.migration.example.common.mapping

import com.quadient.migration.api.Migration
import com.quadient.migration.api.dto.migrationmodel.VariableStructure
import com.quadient.migration.example.common.util.Csv
import com.quadient.migration.example.common.util.Mapping
import com.quadient.migration.service.deploy.utility.ResourceType

import static com.quadient.migration.example.common.util.InitMigration.initMigration

def migration = initMigration(this.binding)

def variableStructuresPath = Mapping.csvPath(binding, migration.projectConfig.name, migration.projectConfig.subProjectId, "variable-structures")

run(migration, variableStructuresPath.toFile())

static void run(Migration migration, File variableStructuresDstPath) {
    List<VariableStructure> variableStructures = Mapping.collectSelectedOrAll(migration, VariableStructure) { migration.variableStructureRepository.listAll() }

    variableStructuresDstPath.createParentDirectories()

    variableStructuresDstPath.withWriter { writer ->
        def headers = ["id", "name", "targetFolder", "status", Mapping.displayHeader("originLocations", true)]
        writer.writeLine(headers.join(","))
        variableStructures.each { obj ->
            def status = migration.statusTrackingRepository.findLastEventRelevantToOutput(obj.id,
                    ResourceType.VariableStructure,
                    migration.projectConfig.inspireOutput)

            def builder = new StringBuilder()
            builder.append(Csv.serialize(obj.id))
            builder.append("," + Csv.serialize(obj.name))
            builder.append("," + Csv.serialize(obj.targetFolder))
            builder.append("," + Csv.serialize(status?.class?.simpleName))
            builder.append("," + Csv.serialize(obj.originLocations))

            writer.writeLine(builder.toString())
        }
    }
}
