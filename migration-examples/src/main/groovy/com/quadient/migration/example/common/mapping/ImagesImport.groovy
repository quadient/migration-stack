//! ---
//! displayName: Import Images
//! category: Mapping
//! description: Imports images details from CSV files into the migration project, applying any updates made to the columns during editing.
//! target: gradle
//! ---
package com.quadient.migration.example.common.mapping

import com.quadient.migration.api.Migration
import com.quadient.migration.api.dto.migrationmodel.builder.ImageBuilder
import com.quadient.migration.example.common.util.Csv
import com.quadient.migration.example.common.util.Mapping
import com.quadient.migration.service.deploy.ResourceType
import com.quadient.migration.shared.ImageType
import com.quadient.migration.shared.SkipOptions

import java.nio.file.Path

import static com.quadient.migration.example.common.util.InitMigration.initMigration

def migration = initMigration(this.binding)

def path  = Mapping.csvPath(binding, migration.projectConfig.name, "images")

run(migration, path)

static void run(Migration migration, Path imagesFilePath) {
    def deploymentId = UUID.randomUUID().toString()
    def now = new Date().getTime()
    def output = migration.projectConfig.inspireOutput
    def imageLines = imagesFilePath.toFile().readLines()
    def imageColumnNames  = Csv.parseColumnNames(imageLines.removeFirst()).collect { Mapping.normalizeHeader(it) }

    for (line in imageLines) {
        def values = Csv.getCells(line, imageColumnNames)
        def id = values.get("id")
        def existingImage = migration.imageRepository.find(id)
        if (existingImage == null) {
            migration.imageRepository.upsert(new ImageBuilder(id).build())
            existingImage = migration.imageRepository.find(id)
        }
        def existingMapping = migration.mappingRepository.getImageMapping(id)

        def status = migration.statusTrackingRepository.findLastEventRelevantToOutput(existingImage.id,
                ResourceType.Image,
                migration.projectConfig.inspireOutput)

        def newName = Csv.deserialize(values.get("name"), String.class)
        existingMapping.name = newName

        def newSourcePath = Csv.deserialize(values.get("sourcePath"), String.class)
        existingMapping.sourcePath = newSourcePath

        def newImageType = Csv.deserialize(values.get("imageType"), ImageType.class)
        existingMapping.imageType = newImageType

        def newTargetFolder = Csv.deserialize(values.get("targetFolder"), String.class)
        existingMapping.targetFolder = newTargetFolder

        def newAlternateText = Csv.deserialize(values.get("alternateText"), String.class)
        existingMapping.alternateText = newAlternateText

        def newTargetAttachmentId = Csv.deserialize(values.get("targetAttachmentId"), String.class)
        existingMapping.targetAttachmentId = newTargetAttachmentId

        def csvStatus = values.get("status")
        if ((csvStatus == null || csvStatus == "") && status == null) {
            migration.statusTrackingRepository.active(existingImage.id, ResourceType.Image, [reason: "Manual"])
        } else if (csvStatus == "Active" && status?.class?.simpleName != "Active") {
            migration.statusTrackingRepository.active(existingImage.id, ResourceType.Image, [reason: "Manual"])
        } else if (csvStatus == "Deployed" && status?.class?.simpleName != "Deployed") {
            migration.statusTrackingRepository.deployed(existingImage.id, deploymentId, now, ResourceType.Image, null, output, [reason: "Manual"])
        }

        boolean newSkip = Csv.deserialize(values.get("skip"), boolean)
        def newSkipReason = Csv.deserialize(values.get("skipReason"), String.class)
        def newSkipPlaceholder = Csv.deserialize(values.get("skipPlaceholder"), String.class)
        existingMapping.skip = new SkipOptions(newSkip, newSkipPlaceholder, newSkipReason)

        migration.mappingRepository.upsert(id, existingMapping)
        migration.mappingRepository.applyImageMapping(id)
    }
}