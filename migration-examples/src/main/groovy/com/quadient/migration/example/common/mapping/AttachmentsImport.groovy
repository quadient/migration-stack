//! ---
//! displayName: Import Attachments
//! category: Mapping
//! description: Imports attachment details from CSV files into the migration project, applying any updates made to the columns during editing.
//! target: gradle
//! ---
package com.quadient.migration.example.common.mapping

import com.quadient.migration.api.Migration
import com.quadient.migration.api.dto.migrationmodel.MappingItem
import com.quadient.migration.api.dto.migrationmodel.builder.AttachmentBuilder
import com.quadient.migration.example.common.util.Csv
import com.quadient.migration.example.common.util.Mapping
import com.quadient.migration.service.deploy.utility.ResourceType
import com.quadient.migration.shared.AttachmentType
import com.quadient.migration.shared.SkipOptions

import java.nio.file.Path

import groovy.transform.Field
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import static com.quadient.migration.example.common.util.InitMigration.initMigration

@Field static Logger log = LoggerFactory.getLogger(this.class.name)

def migration = initMigration(this.binding)

def path = Mapping.csvPath(binding, migration.projectConfig.name, "attachments")

run(migration, path)

static void run(Migration migration, Path attachmentsFilePath) {
    def deploymentId = UUID.randomUUID().toString()
    def now = new Date().getTime()
    def output = migration.projectConfig.inspireOutput
    def attachmentLines = attachmentsFilePath.toFile().readLines()
    def attachmentColumnNames = Csv.parseColumnNames(attachmentLines.removeFirst()).collect { Mapping.normalizeHeader(it) }
    def total = attachmentLines.size()

    def mappings = new HashMap<String, MappingItem>()
    for (line in attachmentLines) {
        def values = Csv.getCells(line, attachmentColumnNames)
        def id = values.get("id")
        def existingAttachment = migration.attachmentRepository.find(id)
        if (existingAttachment == null) {
            migration.attachmentRepository.upsert(new AttachmentBuilder(id).build())
            existingAttachment = migration.attachmentRepository.find(id)
        }
        def existingMapping = migration.mappingRepository.getAttachmentMapping(id)

        def status = migration.statusTrackingRepository.findLastEventRelevantToOutput(existingAttachment.id,
                ResourceType.Attachment,
                migration.projectConfig.inspireOutput)

        def newName = Csv.deserialize(values.get("name"), String.class)
        existingMapping.name = newName

        def newSourcePath = Csv.deserialize(values.get("sourcePath"), String.class)
        existingMapping.sourcePath = newSourcePath

        def newAttachmentType = Csv.deserialize(values.get("attachmentType"), AttachmentType.class)
        existingMapping.attachmentType = newAttachmentType

        def newTargetFolder = Csv.deserialize(values.get("targetFolder"), String.class)
        existingMapping.targetFolder = newTargetFolder

        def newTargetImageId = Csv.deserialize(values.get("targetImageId"), String.class)
        existingMapping.targetImageId = newTargetImageId

        def csvStatus = values.get("status")
        if ((csvStatus == null || csvStatus == "") && status == null) {
            migration.statusTrackingRepository.active(existingAttachment.id, ResourceType.Attachment, [reason: "Manual"])
        } else if (csvStatus == "Active" && status?.class?.simpleName != "Active") {
            migration.statusTrackingRepository.active(existingAttachment.id, ResourceType.Attachment, [reason: "Manual"])
        } else if (csvStatus == "Deployed" && status?.class?.simpleName != "Deployed") {
            migration.statusTrackingRepository.deployed(existingAttachment.id, deploymentId, now, ResourceType.Attachment, null, output, [reason: "Manual"])
        }

        boolean newSkip = Csv.deserialize(values.get("skip"), boolean)
        def newSkipReason = Csv.deserialize(values.get("skipReason"), String.class)
        def newSkipPlaceholder = Csv.deserialize(values.get("skipPlaceholder"), String.class)
        existingMapping.skip = new SkipOptions(newSkip, newSkipPlaceholder, newSkipReason)

        mappings[id] = existingMapping
        if (total > 1000 && mappings.size() % 1000 == 0) {
            log.info("Processed ${mappings.size()}/${total} mappings")
        }
    }

    def batches = mappings.entrySet().collate(1000)
    for (int i = 0; i < batches.size(); i++) {
        log.info("Upserting mappings batch ${i + 1}/${batches.size()} (${batches[i].size()} items)")
        migration.mappingRepository.upsertBatch(batches[i].collectEntries())
    }
    migration.mappingRepository.applyAllAttachmentMappings()
}
