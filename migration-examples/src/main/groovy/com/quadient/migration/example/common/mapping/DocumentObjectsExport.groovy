//! ---
//! displayName: Export Document Objects
//! category: Mapping
//! description: Creates CSV files with document object details from the migration project. The generated CSV columns can be updated and later imported back into the database using a dedicated import task.
//! target: gradle
//! ---
package com.quadient.migration.example.common.mapping

import com.quadient.migration.api.Migration
import com.quadient.migration.example.common.util.Csv
import com.quadient.migration.example.common.util.Mapping
import com.quadient.migration.service.deploy.ResourceType

import java.nio.file.Path

import static com.quadient.migration.example.common.util.InitMigration.initMigration

def migration = initMigration(this.binding)

def docObjPath = Mapping.csvPath(binding, migration.projectConfig.name, "document-objects")

run(migration, docObjPath)

static void run(Migration migration, Path documentObjectsDstPath) {
    def objects = migration.documentObjectRepository.listAll().findAll { !it.internal }

    documentObjectsDstPath.toFile().createParentDirectories()

    documentObjectsDstPath.toFile().withWriter { writer ->
        writer.writeLine("id,name,type,internal,originLocation,baseTemplate,targetFolder,variableStructureId,status,skip,skipPlaceholder,skipReason")
        objects.each { obj ->
            def status = migration.statusTrackingRepository.findLastEventRelevantToOutput(obj.id,
                    ResourceType.DocumentObject,
                    migration.projectConfig.inspireOutput)

            def builder = new StringBuilder()
            builder.append(Csv.serialize(obj.id))
            builder.append("," + Csv.serialize(obj.name))
            builder.append("," + Csv.serialize(obj.type))
            builder.append("," + Csv.serialize(obj.internal))
            builder.append("," + Csv.serialize(obj.originLocations))
            builder.append("," + Csv.serialize(obj.baseTemplate))
            builder.append("," + Csv.serialize(obj.targetFolder))
            builder.append("," + Csv.serialize(obj.variableStructureRef?.id))
            builder.append("," + Csv.serialize(status.class.simpleName))
            builder.append("," + Csv.serialize(obj.skip.skipped))
            builder.append("," + Csv.serialize(obj.skip.placeholder))
            builder.append("," + Csv.serialize(obj.skip.reason))

            writer.writeLine(builder.toString())
        }
    }
}