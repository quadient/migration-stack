//! ---
//! category: migration mapping
//! description: Export document objects and images
//! target: gradle
//! ---
package com.quadient.migration.example.common.mapping

import com.quadient.migration.api.Migration
import com.quadient.migration.example.common.util.Csv
import com.quadient.migration.service.deploy.ResourceType

import java.nio.file.Path
import java.nio.file.Paths

import static com.quadient.migration.example.common.util.InitMigration.initMigration

def migration = initMigration(this.binding.variables["args"])

def docObjPath = Paths.get("mapping", "${migration.projectConfig.name}-document-objects.csv")
def imagesPath = Paths.get("mapping", "${migration.projectConfig.name}-images.csv")

run(migration, docObjPath, imagesPath)

static void run(Migration migration, Path documentObjectsDstPath, Path imagesDstPath) {
    def objects = migration.documentObjectRepository.listAll().findAll { !it.internal }
    def images = migration.imageRepository.listAll()

    documentObjectsDstPath.toFile().createParentDirectories()
    imagesDstPath.toFile().createParentDirectories()

    documentObjectsDstPath.toFile().withWriter { writer ->
        writer.writeLine("id,name,type,internal,originLocation,baseTemplate,targetFolder,variableStructureId,status")
        objects.each { obj ->
            def mapping = migration.mappingRepository.getDocumentObjectMapping(obj.id)
            def status = migration.statusTrackingRepository.findLastEventRelevantToOutput(
                    obj.id,
                    ResourceType.DocumentObject,
                    migration.projectConfig.inspireOutput
            )

            def builder = new StringBuilder()
            builder.append(Csv.serialize(obj.id))
            builder.append("," + Csv.serialize(mapping.name ?: obj.name))
            builder.append("," + Csv.serialize(mapping.type ?: obj.type))
            builder.append("," + Csv.serialize(mapping.internal ?: obj.internal))
            builder.append("," + Csv.serialize(obj.originLocations))
            builder.append("," + Csv.serialize(mapping.baseTemplate ?: obj.baseTemplate))
            builder.append("," + Csv.serialize(mapping.targetFolder ?: obj.targetFolder))
            builder.append("," + Csv.serialize(mapping.variableStructureRef ?: obj.variableStructureRef?.id))
            builder.append("," + Csv.serialize(status.class.simpleName))

            writer.writeLine(builder.toString())
        }
    }

    imagesDstPath.toFile().withWriter { writer ->
        writer.writeLine("id,name,sourcePath,originLocation,targetFolder,status")
        images.each { obj ->
            def mapping = migration.mappingRepository.getImageMapping(obj.id)
            def status = migration.statusTrackingRepository.findLastEventRelevantToOutput(
                    obj.id,
                    ResourceType.Image,
                    migration.projectConfig.inspireOutput
            )

            def builder = new StringBuilder()
            builder.append(Csv.serialize(obj.id))
            builder.append("," + Csv.serialize(mapping?.name ?: obj.name))
            builder.append("," + Csv.serialize(mapping?.sourcePath ?: obj.sourcePath))
            builder.append("," + Csv.serialize(obj.originLocations))
            builder.append("," + Csv.serialize(mapping?.targetFolder ?: obj.targetFolder))
            builder.append("," + Csv.serialize(status.class.simpleName))

            writer.writeLine(builder.toString())
        }
    }
}

