//! ---
//! displayName: Import Files
//! category: Mapping
//! description: Imports file details from CSV files into the migration project, applying any updates made to the columns during editing.
//! target: gradle
//! ---
package com.quadient.migration.example.common.mapping

import com.quadient.migration.api.Migration
import com.quadient.migration.example.common.util.Csv
import com.quadient.migration.example.common.util.Mapping
import com.quadient.migration.service.deploy.ResourceType
import com.quadient.migration.shared.FileType
import com.quadient.migration.shared.SkipOptions

import java.nio.file.Path

import static com.quadient.migration.example.common.util.InitMigration.initMigration

def migration = initMigration(this.binding)

def path = Mapping.csvPath(binding, migration.projectConfig.name, "files")

run(migration, path)

static void run(Migration migration, Path filesFilePath) {
    def deploymentId = UUID.randomUUID().toString()
    def now = new Date().getTime()
    def output = migration.projectConfig.inspireOutput
    def fileLines = filesFilePath.toFile().readLines()
    def fileColumnNames = Csv.parseColumnNames(fileLines.removeFirst()).collect { Mapping.normalizeHeader(it) }

    for (line in fileLines) {
        def values = Csv.getCells(line, fileColumnNames)
        def id = values.get("id")
        def existingFile = migration.fileRepository.find(id)
        def existingMapping = migration.mappingRepository.getFileMapping(id)

        if (existingFile == null) {
            throw new Exception("File with id ${id} not found")
        }
        def status = migration.statusTrackingRepository.findLastEventRelevantToOutput(existingFile.id,
                ResourceType.File,
                migration.projectConfig.inspireOutput)

        def newName = Csv.deserialize(values.get("name"), String.class)
        Mapping.mapProp(existingMapping, existingFile, "name", newName)

        def newSourcePath = Csv.deserialize(values.get("sourcePath"), String.class)
        Mapping.mapProp(existingMapping, existingFile, "sourcePath", newSourcePath)

        def newFileType = Csv.deserialize(values.get("fileType"), FileType.class)
        Mapping.mapProp(existingMapping, existingFile, "fileType", newFileType)

        def newTargetFolder = Csv.deserialize(values.get("targetFolder"), String.class)
        Mapping.mapProp(existingMapping, existingFile, "targetFolder", newTargetFolder)

        def csvStatus = values.get("status")
        if (status != null && csvStatus == "Active" && status.class.simpleName != "Active") {
            migration.statusTrackingRepository.active(existingFile.id, ResourceType.File, [reason: "Manual"])
        }
        if (status != null && csvStatus == "Deployed" && status.class.simpleName != "Deployed") {
            migration.statusTrackingRepository.deployed(existingFile.id, deploymentId, now, ResourceType.File, null, output, [reason: "Manual"])
        }

        boolean newSkip = Csv.deserialize(values.get("skip"), boolean)
        def newSkipReason = Csv.deserialize(values.get("skipReason"), String.class)
        def newSkipPlaceholder = Csv.deserialize(values.get("skipPlaceholder"), String.class)
        def skipObj = new SkipOptions(newSkip, newSkipPlaceholder, newSkipReason)
        Mapping.mapProp(existingMapping, existingFile, "skip", skipObj)

        migration.mappingRepository.upsert(id, existingMapping)
        migration.mappingRepository.applyFileMapping(id)
    }
}
