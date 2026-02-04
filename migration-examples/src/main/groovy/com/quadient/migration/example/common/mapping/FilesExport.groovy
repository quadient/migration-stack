//! ---
//! displayName: Export Files
//! category: Mapping
//! description: Creates CSV files with file details from the migration project. The generated CSV columns can be updated and later imported back into the database using a dedicated import task.
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

def filesPath = Mapping.csvPath(binding, migration.projectConfig.name, "files")

run(migration, filesPath)

static void run(Migration migration, Path filesDstPath) {
    def files = migration.fileRepository.listAll()

    filesDstPath.toFile().createParentDirectories()

    filesDstPath.toFile().withWriter { writer ->
        def headers = ["id", "name", "sourcePath", "fileType", "targetFolder", "status", "skip", "skipPlaceholder", "skipReason", Mapping.displayHeader("originalName", true), Mapping.displayHeader("originLocations", true)]
        writer.writeLine(headers.join(","))
        files.each { obj ->
            def status = migration.statusTrackingRepository.findLastEventRelevantToOutput(obj.id,
                    ResourceType.File,
                    migration.projectConfig.inspireOutput)

            def builder = new StringBuilder()
            builder.append(Csv.serialize(obj.id))
            builder.append("," + Csv.serialize(obj.name))
            builder.append("," + Csv.serialize(obj.sourcePath))
            builder.append("," + Csv.serialize(obj.fileType))
            builder.append("," + Csv.serialize(obj.targetFolder))
            builder.append("," + Csv.serialize(status.class.simpleName))
            builder.append("," + Csv.serialize(obj.skip.skipped))
            builder.append("," + Csv.serialize(obj.skip.placeholder))
            builder.append("," + Csv.serialize(obj.skip.reason))
            builder.append("," + Csv.serialize(obj.customFields["originalName"]))
            builder.append("," + Csv.serialize(obj.originLocations))

            writer.writeLine(builder.toString())
        }
    }
}
