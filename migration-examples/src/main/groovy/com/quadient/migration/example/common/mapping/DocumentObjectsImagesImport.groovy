package com.quadient.migration.example.common.mapping

import com.quadient.migration.example.common.util.Csv
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

for (line in docObjectLines) {
    def values = Csv.getCells(line, docObjectColumnNames)
    def existingDocObject = migration.documentObjectRepository.find(values.get("id"))
    if (existingDocObject == null) {
        throw new Exception("DocumentObject with id ${values.get("id")} not found")
    }
    existingDocObject.name = Csv.deserialize(values.get("name"), String.class)
    existingDocObject.internal = Csv.deserialize(values.get("internal"), boolean)
    existingDocObject.baseTemplate = Csv.deserialize(values.get("baseTemplate"), String.class)
    existingDocObject.targetFolder = Csv.deserialize(values.get("targetFolder"), String.class)
    def type = Csv.deserialize(values.get("type"), DocumentObjectType.class)
    if (type != existingDocObject.type) {
        existingDocObject.options = null
        existingDocObject.type = type
    }

    migration.documentObjectRepository.upsert(existingDocObject)
}
for (line in imageLines) {
    def values = Csv.getCells(line, imageColumnNames)
    def existingImage = migration.imageRepository.find(values.get("id"))
    if (existingImage  == null) {
        throw new Exception("Image with id ${values.get("id")} not found")
    }

    existingImage.name = Csv.deserialize(values.get("name"), String.class)
    existingImage.sourcePath = Csv.deserialize(values.get("sourcePath"), String.class)
    existingImage.targetFolder = Csv.deserialize(values.get("targetFolder"), String.class)

    migration.imageRepository.upsert(existingImage)
}
