//! ---
//! displayName: Export Attachments
//! category: Mapping
//! description: Creates CSV files with attachment details from the migration project. The generated CSV columns can be updated and later imported back into the database using a dedicated import task.
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

def attachmentsPath = Mapping.csvPath(binding, migration.projectConfig.name, "attachments")

run(migration, attachmentsPath)

static void run(Migration migration, Path attachmentsDstPath) {
    def attachments = migration.attachmentRepository.listAll()

    attachmentsDstPath.toFile().createParentDirectories()

    attachmentsDstPath.toFile().withWriter { writer ->
        def headers = ["id", "name", "sourcePath", "attachmentType", "targetFolder", "targetImageId", "status", "skip", "skipPlaceholder", "skipReason", Mapping.displayHeader("originalName", true), Mapping.displayHeader("originLocations", true)]
        writer.writeLine(headers.join(","))
        attachments.each { obj ->
            def status = migration.statusTrackingRepository.findLastEventRelevantToOutput(obj.id,
                    ResourceType.Attachment,
                    migration.projectConfig.inspireOutput)

            def builder = new StringBuilder()
            builder.append(Csv.serialize(obj.id))
            builder.append("," + Csv.serialize(obj.name))
            builder.append("," + Csv.serialize(obj.sourcePath))
            builder.append("," + Csv.serialize(obj.attachmentType))
            builder.append("," + Csv.serialize(obj.targetFolder))
            builder.append("," + Csv.serialize(obj.targetImageId))
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
