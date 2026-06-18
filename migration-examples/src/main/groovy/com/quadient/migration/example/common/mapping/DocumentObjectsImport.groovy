//! ---
//! displayName: Import Document Objects
//! category: Mapping
//! description: Imports document objects details from CSV files into the migration project, applying any updates made to the columns during editing.
//! target: gradle
//! ---
package com.quadient.migration.example.common.mapping

import com.quadient.migration.api.Migration
import com.quadient.migration.api.dto.migrationmodel.MappingItem
import com.quadient.migration.example.common.util.Csv
import com.quadient.migration.example.common.util.Mapping
import com.quadient.migration.service.deploy.utility.ResourceType
import com.quadient.migration.shared.DocumentObjectType
import com.quadient.migration.shared.SkipOptions

import java.nio.file.Path

import groovy.transform.Field
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import static com.quadient.migration.example.common.util.InitMigration.initMigration

@Field static Logger log = LoggerFactory.getLogger(this.class.name)

def migration = initMigration(this.binding)

def docObjPath  = Mapping.csvPath(binding, migration.projectConfig.name, "document-objects")

run(migration, docObjPath)

static void run(Migration migration, Path documentObjFilePath) {
    def deploymentId = UUID.randomUUID().toString()
    def now = new Date().getTime()
    def docObjectLines = documentObjFilePath.toFile().readLines()

    def docObjectColumnNames = Csv.parseColumnNames(docObjectLines.removeFirst()).collect { Mapping.normalizeHeader(it) }
    def total = docObjectLines.size()
    def output = migration.projectConfig.inspireOutput

    def mappings = new HashMap<String, MappingItem>()
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

        def varStructureRef = Csv.deserialize(values.get("variableStructureId"), String.class)
        if (varStructureRef != existingDocObject.variableStructureRef?.id && varStructureRef != existingMapping.variableStructureRef) {
            existingMapping.variableStructureRef = varStructureRef
        }

        def csvStatus = values.get("status")
        if (status != null && csvStatus == "Active" && status.class.simpleName != "Active") {
            migration.statusTrackingRepository.active(existingDocObject.id, ResourceType.DocumentObject, [reason: "Manual"])
        }
        if (status != null && csvStatus == "Deployed" && status.class.simpleName != "Deployed") {
            migration.statusTrackingRepository.deployed(existingDocObject.id, deploymentId, now, ResourceType.DocumentObject, null, output, [reason: "Manual"])
        }

        boolean newSkip = Csv.deserialize(values.get("skip"), boolean)
        def newSkipReason = Csv.deserialize(values.get("skipReason"), String.class)
        def newSkipPlaceholder = Csv.deserialize(values.get("skipPlaceholder"), String.class)
        def skipObj = new SkipOptions(newSkip, newSkipPlaceholder, newSkipReason)
        Mapping.mapProp(existingMapping, existingDocObject, "skip", skipObj)

        mappings[id] = existingMapping
        if (total > 1000 && mappings.size() % 1000 == 0) {
            log.info "Processed ${mappings.size()}/${total} mappings"
        }
    }

    def batches = mappings.entrySet().collate(1000)
    for (int i = 0; i < batches.size(); i++) {
        log.info "Upserting mappings batch ${i + 1}/${batches.size()} (${batches[i].size()} items)"
        migration.mappingRepository.upsertBatch(batches[i].collectEntries())
    }
    migration.mappingRepository.applyAllDocumentObjectMappings()
}