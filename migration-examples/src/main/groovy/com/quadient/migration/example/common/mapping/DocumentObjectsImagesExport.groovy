package com.quadient.migration.example.common.mapping

import com.quadient.migration.example.common.util.Csv
import com.quadient.migration.service.deploy.ResourceType

import java.nio.file.Paths

import static com.quadient.migration.example.common.util.InitMigration.initMigration

def migration = initMigration(this.binding.variables["args"])

def objects = migration.documentObjectRepository.listAll().findAll { !it.internal }
def images = migration.imageRepository.listAll()

def documentObjFile = Paths.get("mapping", "${migration.projectConfig.name}-document-objects.csv").toFile()
def imagesFile = Paths.get("mapping", "${migration.projectConfig.name}-images.csv").toFile()

documentObjFile.createParentDirectories()
imagesFile .createParentDirectories()

documentObjFile.withWriter { writer ->
    writer.writeLine("id,name,type,internal,originLocation,baseTemplate,targetFolder,variableStructureId,status")
    objects.each { obj ->
        def status = migration.statusTrackingRepository.findLastEventRelevantToOutput(
            obj.id,
            ResourceType.DocumentObject,
            migration.projectConfig.inspireOutput
        )

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

        writer.writeLine(builder.toString())
    }
}

imagesFile.withWriter { writer ->
    writer.writeLine("id,name,sourcePath,originLocation,targetFolder,status")
    images.each { obj ->
        def status = migration.statusTrackingRepository.findLastEventRelevantToOutput(
            obj.id,
            ResourceType.Image,
            migration.projectConfig.inspireOutput
        )

        def builder = new StringBuilder()
        builder.append(Csv.serialize(obj.id))
        builder.append("," + Csv.serialize(obj.name))
        builder.append("," + Csv.serialize(obj.sourcePath))
        builder.append("," + Csv.serialize(obj.originLocations))
        builder.append("," + Csv.serialize(obj.targetFolder))
        builder.append("," + Csv.serialize(status.class.simpleName))

        writer.writeLine(builder.toString())
    }
}
