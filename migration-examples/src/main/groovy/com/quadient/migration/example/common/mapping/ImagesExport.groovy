//! ---
//! displayName: Export Images
//! category: Mapping
//! description: Creates CSV files with image details from the migration project. The generated CSV columns can be updated and later imported back into the database using a dedicated import task.
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

def imagesPath = Mapping.csvPath(binding, migration.projectConfig.name, "images")

run(migration, imagesPath)

static void run(Migration migration, Path imagesDstPath) {
    def images = migration.imageRepository.listAll()

    imagesDstPath.toFile().createParentDirectories()

    imagesDstPath.toFile().withWriter { writer ->
        def headers = ["id", "name", "sourcePath", "imageType", "targetFolder", "status", "skip", "skipPlaceholder", "skipReason", Mapping.displayHeader("originalName", true), Mapping.displayHeader("originLocations", true)]
        writer.writeLine(headers.join(","))
        images.each { obj ->
            def status = migration.statusTrackingRepository.findLastEventRelevantToOutput(obj.id,
                    ResourceType.Image,
                    migration.projectConfig.inspireOutput)

            def builder = new StringBuilder()
            builder.append(Csv.serialize(obj.id))
            builder.append("," + Csv.serialize(obj.name))
            builder.append("," + Csv.serialize(obj.sourcePath))
            builder.append("," + Csv.serialize(obj.imageType))
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