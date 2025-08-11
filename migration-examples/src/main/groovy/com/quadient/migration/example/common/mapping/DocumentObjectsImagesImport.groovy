package com.quadient.migration.example.common.mapping

import com.quadient.migration.api.Migration
import com.quadient.migration.example.common.util.Csv
import com.quadient.migration.example.common.util.Mapping
import com.quadient.migration.service.deploy.ResourceType
import com.quadient.migration.shared.DocumentObjectType

import java.nio.file.Path
import java.nio.file.Paths

import static com.quadient.migration.example.common.util.InitMigration.initMigration

def migration = initMigration(this.binding.variables["args"])

def documentObjFilePath = Paths.get("mapping", "${migration.projectConfig.name}-document-objects.csv")
def imagesFilePath = Paths.get("mapping", "${migration.projectConfig.name}-images.csv")

runDocumentObjects(migration, documentObjFilePath)
runImages(migration, imagesFilePath)

static void runDocumentObjects(Migration migration, Path documentObjFilePath) {
    def deploymentId = UUID.randomUUID().toString()
    def now = new Date().getTime()
    def docObjectLines = documentObjFilePath.toFile().readLines()

    // id, name, internal, baseTemplate, icmFolder
    def docObjectColumnNames = docObjectLines.removeFirst().split(",")
    def output = migration.projectConfig.inspireOutput

    for (line in docObjectLines) {
        def values = Csv.getCells(line, docObjectColumnNames)
        def id = values.get("id")
        def existingDocObject = migration.documentObjectRepository.find(id)
        def existingMapping = migration.mappingRepository.getDocumentObjectMapping(id)

        if (existingDocObject == null) {
            throw new Exception("DocumentObject with id ${id} not found")
        }
        def status = migration.statusTrackingRepository.findLastEventRelevantToOutput(existingDocObject.id,
            ResourceType.DocumentObject,
            migration.projectConfig.inspireOutput)

        def newName = Csv.deserialize(values.get("name"), String.class)
        Mapping.mapProp(existingMapping, existingDocObject, "name", newName)

        def newInternal = Csv.deserialize(values.get("internal"), boolean)
        Mapping.mapProp(existingMapping, existingDocObject, "internal", newInternal)

        def newBaseTemplate = Csv.deserialize(values.get("baseTemplate"), String.class)
        Mapping.mapProp(existingMapping, existingDocObject, "baseTemplate", newBaseTemplate)

        def newTargetFolder = Csv.deserialize(values.get("targetFolder"), String.class)
        Mapping.mapProp(existingMapping, existingDocObject, "targetFolder", newTargetFolder)

        def newType = Csv.deserialize(values.get("type"), DocumentObjectType.class)
        Mapping.mapProp(existingMapping, existingDocObject, "type", newType)

        def csvStatus = values.get("status")
        if (status != null && csvStatus == "Active" && status.class.simpleName != "Active") {
            migration.statusTrackingRepository.active(existingDocObject.id, ResourceType.DocumentObject, [reason: "Manual"])
        }
        if (status != null && csvStatus == "Deployed" && status.class.simpleName != "Deployed") {
            migration.statusTrackingRepository.deployed(existingDocObject.id, deploymentId, now, ResourceType.DocumentObject, null, output, [reason: "Manual"])
        }

        migration.mappingRepository.upsert(id, existingMapping)
        migration.mappingRepository.applyDocumentObjectMapping(id)
    }

}

static void runImages(Migration migration, Path imagesFilePath) {
    def deploymentId = UUID.randomUUID().toString()
    def now = new Date().getTime()
    def output = migration.projectConfig.inspireOutput
    def imageLines = imagesFilePath.toFile().readLines()
    // id, name, sourcePath, icmFolder
    def imageColumnNames = imageLines.removeFirst().split(",")

    for (line in imageLines) {
        def values = Csv.getCells(line, imageColumnNames)
        def id = values.get("id")
        def existingImage = migration.imageRepository.find(id)
        def existingMapping = migration.mappingRepository.getImageMapping(id)

        if (existingImage == null) {
            throw new Exception("Image with id ${id} not found")
        }
        def status = migration.statusTrackingRepository.findLastEventRelevantToOutput(existingImage.id,
            ResourceType.Image,
            migration.projectConfig.inspireOutput)

        def newName = Csv.deserialize(values.get("name"), String.class)
        Mapping.mapProp(existingMapping, existingImage, "name", newName)

        def newSourcePath = Csv.deserialize(values.get("sourcePath"), String.class)
        Mapping.mapProp(existingMapping, existingImage, "sourcePath", newSourcePath)

        def newTargetFolder = Csv.deserialize(values.get("targetFolder"), String.class)
        Mapping.mapProp(existingMapping, existingImage, "targetFolder", newTargetFolder)

        def csvStatus = values.get("status")
        if (status != null && csvStatus == "Active" && status.class.simpleName != "Active") {
            migration.statusTrackingRepository.active(existingImage.id, ResourceType.Image, [reason: "Manual"])
        }
        if (status != null && csvStatus == "Deployed" && status.class.simpleName != "Deployed") {
            migration.statusTrackingRepository.deployed(existingImage.id, deploymentId, now, ResourceType.Image, null, output, [reason: "Manual"])
        }

        migration.mappingRepository.upsert(id, existingMapping)
        migration.mappingRepository.applyImageMapping(id)
    }
}


