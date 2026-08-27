//! ---
//! displayName: Import Base Templates
//! category: Mapping
//! description: Imports base template details from CSV files into the migration project, applying any updates made to the columns during editing.
//! target: gradle
//! ---
package com.quadient.migration.example.common.mapping

import com.quadient.migration.api.Migration
import com.quadient.migration.api.dto.migrationmodel.MappingItem
import com.quadient.migration.example.common.util.Csv
import com.quadient.migration.example.common.util.Mapping
import com.quadient.migration.service.deploy.utility.ResourceType

import groovy.transform.Field
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import static com.quadient.migration.example.common.util.InitMigration.initMigration

@Field static Logger log = LoggerFactory.getLogger(this.class.name)

def migration = initMigration(this.binding)

def baseTemplatePath = Mapping.csvPath(binding, migration.projectConfig.name, migration.projectConfig.subProjectId, "base-templates")

run(migration, baseTemplatePath.toFile())

static void run(Migration migration, File file) {
    def deploymentId = UUID.randomUUID().toString()
    def now = new Date().getTime()
    def output = migration.projectConfig.inspireOutput

    def lines = file.readLines()
    def columnNames = Csv.parseColumnNames(lines.removeFirst()).collect { Mapping.normalizeHeader(it) }
    def total = lines.size()

    def mappings = new HashMap<String, MappingItem>()
    for (line in lines) {
        def values = Csv.getCells(line, columnNames)
        def id = values.get("id")

        def existingBaseTemplate = migration.baseTemplateRepository.find(id)
        if (existingBaseTemplate == null) {
            throw new Exception("BaseTemplate with id ${id} not found")
        }
        def existingMapping = migration.mappingRepository.getBaseTemplateMapping(id)

        def status = migration.statusTrackingRepository.findLastEventRelevantToOutput(existingBaseTemplate.id,
            ResourceType.BaseTemplate,
            migration.projectConfig.inspireOutput)

        def newName = Csv.deserialize(values.get("name"), String.class)
        existingMapping.name = newName

        def newTargetFolder = Csv.deserialize(values.get("targetFolder"), String.class)
        existingMapping.targetFolder = newTargetFolder

        def newVariableStructureId = Csv.deserialize(values.get("variableStructureRef"), String.class)
        existingMapping.variableStructureRef = newVariableStructureId

        def csvStatus = values.get("status")
        if ((csvStatus == null || csvStatus == "") && status == null) {
            migration.statusTrackingRepository.active(existingBaseTemplate.id, ResourceType.BaseTemplate, [reason: "Manual"])
        } else if (csvStatus == "Active" && status?.class?.simpleName != "Active") {
            migration.statusTrackingRepository.active(existingBaseTemplate.id, ResourceType.BaseTemplate, [reason: "Manual"])
        } else if (csvStatus == "Deployed" && status?.class?.simpleName != "Deployed") {
            migration.statusTrackingRepository.deployed(existingBaseTemplate.id, deploymentId, now, ResourceType.BaseTemplate, null, output, [reason: "Manual"])
        }

        mappings[id] = existingMapping
        if (total > 1000 && mappings.size() % 1000 == 0) {
            log.info "Processed ${mappings.size()}/${total} mappings"
        }
    }

    Mapping.upsertBatched(migration.mappingRepository, mappings, "base template mappings", log)
    migration.mappingRepository.applyAllBaseTemplateMappings()
}
