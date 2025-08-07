package com.quadient.migration.example.common.mapping

import com.quadient.migration.api.dto.migrationmodel.VariableStructureRef
import com.quadient.migration.example.common.util.Csv
import com.quadient.migration.service.deploy.ResourceType
import com.quadient.migration.shared.DocumentObjectType

import java.nio.file.Paths

import static com.quadient.migration.example.common.util.InitMigration.initMigration

def migration = initMigration(this.binding.variables["args"])

def documentObjFile = Paths.get("mapping", "${migration.projectConfig.name}-document-objects.csv").toFile()
def imagesFile = Paths.get("mapping", "${migration.projectConfig.name}-images.csv").toFile()

def docObjectLines = documentObjFile.readLines()
def imageLines = imagesFile.readLines()

// id, name, internal, baseTemplate, icmFolder
def docObjectColumnNames = docObjectLines.removeFirst().split(",")
// id, name, sourcePath, icmFolderr
def imageColumnNames = imageLines.removeFirst().split(",")
def deploymentId = UUID.randomUUID().toString()
def now = new Date().getTime()
def output = migration.projectConfig.inspireOutput

for (line in docObjectLines) {
    def values = Csv.getCells(line, docObjectColumnNames)
    def existingDocObject = migration.documentObjectRepository.find(values.get("id"))
    if (existingDocObject == null) {
        throw new Exception("DocumentObject with id ${values.get("id")} not found")
    }
    def status = migration.statusTrackingRepository.findLastEventRelevantToOutput(
        existingDocObject.id,
        ResourceType.DocumentObject,
        migration.projectConfig.inspireOutput
    )

    existingDocObject.name = Csv.deserialize(values.get("name"), String.class)
    existingDocObject.internal = Csv.deserialize(values.get("internal"), boolean)
    existingDocObject.baseTemplate = Csv.deserialize(values.get("baseTemplate"), String.class)
    existingDocObject.targetFolder = Csv.deserialize(values.get("targetFolder"), String.class)
    def variableStructureId = Csv.deserialize(values.get("variableStructureId"), String.class)
    if (variableStructureId != null) {
        existingDocObject.variableStructureRef = new VariableStructureRef(variableStructureId)
    }

    def type = Csv.deserialize(values.get("type"), DocumentObjectType.class)
    if (type != existingDocObject.type) {
        existingDocObject.options = null
        existingDocObject.type = type
    }

    def csvStatus = values.get("status")
    if (status != null && csvStatus == "Active" && status.class.simpleName != "Active") {
        migration.statusTrackingRepository.active(existingDocObject.id, ResourceType.DocumentObject, [reason: "Manual"])
    }
    if (status != null && csvStatus == "Deployed" && status.class.simpleName != "Deployed") {
        migration.statusTrackingRepository.deployed(existingDocObject.id, deploymentId, now, ResourceType.DocumentObject, null, output, [reason: "Manual"])
    }

    migration.documentObjectRepository.upsert(existingDocObject)
}
for (line in imageLines) {
    def values = Csv.getCells(line, imageColumnNames)
    def existingImage = migration.imageRepository.find(values.get("id"))
    if (existingImage  == null) {
        throw new Exception("Image with id ${values.get("id")} not found")
    }
    def status = migration.statusTrackingRepository.findLastEventRelevantToOutput(
        existingImage.id,
        ResourceType.Image,
        migration.projectConfig.inspireOutput
    )

    existingImage.name = Csv.deserialize(values.get("name"), String.class)
    existingImage.sourcePath = Csv.deserialize(values.get("sourcePath"), String.class)
    existingImage.targetFolder = Csv.deserialize(values.get("targetFolder"), String.class)

    def csvStatus = values.get("status")
    if (status != null && csvStatus == "Active" && status.class.simpleName != "Active") {
        migration.statusTrackingRepository.active(existingImage.id, ResourceType.Image, [reason: "Manual"])
    }
    if (status != null && csvStatus == "Deployed" && status.class.simpleName != "Deployed") {
        migration.statusTrackingRepository.deployed(existingImage.id, deploymentId, now, ResourceType.Image, null, output, [reason: "Manual"])
    }
    migration.imageRepository.upsert(existingImage)
}