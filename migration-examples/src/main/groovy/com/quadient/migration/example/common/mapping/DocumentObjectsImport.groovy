//! ---
//! displayName: Import Document Objects
//! category: Mapping
//! description: Imports document objects details from CSV files into the migration project, applying any updates made to the columns during editing.
//! target: gradle
//! ---
package com.quadient.migration.example.common.mapping

import com.quadient.migration.api.Migration
import com.quadient.migration.example.common.util.Csv
import com.quadient.migration.example.common.util.Mapping
import com.quadient.migration.service.deploy.ResourceType
import com.quadient.migration.shared.DocumentObjectType

import java.nio.file.Path

import static com.quadient.migration.example.common.util.InitMigration.initMigration

def migration = initMigration(this.binding)

def docObjPath  = Mapping.csvPath(binding, migration.projectConfig.name, "document-objects")

run(migration, docObjPath)

static void run(Migration migration, Path documentObjFilePath) {
    def deploymentId = UUID.randomUUID().toString()
    def now = new Date().getTime()
    def docObjectLines = documentObjFilePath.toFile().readLines()

    def docObjectColumnNames = Csv.parseColumnNames(docObjectLines.removeFirst())
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

        def varStructure = Csv.deserialize(values.get("variableStructureId"), String.class)
        if (varStructure != null && varStructure != "" && varStructure != existingDocObject.variableStructureRef?.id && varStructure != existingMapping.variableStructureRef)  {
            existingMapping.variableStructureRef = varStructure
        }

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