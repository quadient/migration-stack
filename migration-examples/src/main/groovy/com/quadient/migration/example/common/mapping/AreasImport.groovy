//! ---
//! displayName: Import Areas
//! category: Mapping
//! description: Import areas with modified interactive flow names to their respective pages and templates
//! target: gradle
//! ---
package com.quadient.migration.example.common.mapping

import com.quadient.migration.api.Migration
import com.quadient.migration.api.dto.migrationmodel.BaseTemplate
import com.quadient.migration.api.dto.migrationmodel.BaseTemplateLocation
import com.quadient.migration.api.dto.migrationmodel.CustomFieldMap
import com.quadient.migration.api.dto.migrationmodel.DocumentObject
import com.quadient.migration.api.dto.migrationmodel.MappingItem
import com.quadient.migration.example.common.util.Csv
import com.quadient.migration.example.common.util.Mapping
import com.quadient.migration.shared.BaseTemplateArea
import com.quadient.migration.shared.BaseTemplatePage
import com.quadient.migration.shared.Position
import com.quadient.migration.shared.Size
import groovy.transform.Field
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.nio.file.Path

import static com.quadient.migration.example.common.util.InitMigration.initMigration

@Field static Logger log = LoggerFactory.getLogger(this.class.name)

def migration = initMigration(this.binding)
def areasFile = Mapping.csvPath(binding, migration.projectConfig.name, "areas")

run(migration, areasFile)

static void run(Migration migration, Path path) {
    def fileLines = path.toFile().readLines()
    def columnNames = Csv.parseColumnNames(fileLines.removeFirst()).collect { Mapping.normalizeHeader(it) }

    def areaMappings = new HashMap<String, MappingItem>()
    def docObjectsToTargetIds = new LinkedHashMap<String, String>()
    def baseTemplateDrafts = new LinkedHashMap<String, BaseTemplateDraft>()

    DocumentObject currentDocumentObject = null
    MappingItem.Area areaMapping = null
    int areaIndex = 0
    for (line in fileLines) {
        def values = Csv.getCells(line, columnNames)

        def type = Csv.deserialize(values.get("type"), String.class) ?: "Standard"
        if (type.equalsIgnoreCase("Base")) {
            assignAreaToBaseTemplateDraft(baseTemplateDrafts, values)
            continue
        }

        def pageId = Csv.deserialize(values.get("pageId"), String.class)
        def templateId = Csv.deserialize(values.get("templateId"), String.class)
        def documentObjectId = pageId ?: templateId

        if (currentDocumentObject?.id != documentObjectId) {
            if (currentDocumentObject != null) {
                areaMappings[currentDocumentObject.id] = areaMapping
            }

            def documentObjectModel = migration.documentObjectRepository.find(documentObjectId)
            if (!documentObjectModel) {
                throw new IllegalStateException("Document object '${documentObjectId}' not found.")
            }

            areaMapping = migration.mappingRepository.getAreaMapping(documentObjectId)
            currentDocumentObject = documentObjectModel
            areaIndex = 0
        }

        def interactiveFlowName = Csv.deserialize(values.get("interactiveFlowName"), String.class)
        areaMapping.areas[areaIndex] = interactiveFlowName

        def flowToNextPage = Csv.deserialize(values.get("flowToNextPage"), Boolean.class)
        areaMapping.flowToNextPage[areaIndex] = flowToNextPage ?: false

        def targetId = Csv.deserialize(values.get("targetId"), String.class)
        if (targetId) {
            if (pageId && !docObjectsToTargetIds.containsKey(pageId)) {
                docObjectsToTargetIds[pageId] = targetId
            }
            if (templateId && !docObjectsToTargetIds.containsKey(templateId)) {
                docObjectsToTargetIds[templateId] = targetId
            }
        }

        areaIndex++
    }

    if (currentDocumentObject != null) {
        areaMappings[currentDocumentObject.id] = areaMapping
    }

    Mapping.upsertBatched(migration.mappingRepository, areaMappings, "area mappings", log)
    migration.mappingRepository.applyAllAreaMappings()

    applyDocumentObjectTargetIdMappings(migration, docObjectsToTargetIds)
    applyBaseTemplateDraftMappings(migration, baseTemplateDrafts)
}

private static void assignAreaToBaseTemplateDraft(Map<String, BaseTemplateDraft> baseTemplateDrafts, Map<String, String> values) {
    def baseTemplateId = Csv.deserialize(values.get("templateId"), String.class)
    if (!baseTemplateId) {
        throw new IllegalStateException("Rows of type 'Base' must specify a templateId identifying the base template.")
    }
    def pageGroupId = Csv.deserialize(values.get("pageId"), String.class) ?: baseTemplateId

    def baseTemplateDraft = baseTemplateDrafts.computeIfAbsent(baseTemplateId) { new BaseTemplateDraft() }
    if (baseTemplateDraft.name == null) {
        baseTemplateDraft.name = Csv.deserialize(values.get("templateName"), String.class)
    }

    def page = baseTemplateDraft.pages.computeIfAbsent(pageGroupId) {
        new BaseTemplatePage(Csv.deserialize(values.get("pageName"), String.class),
                Csv.deserialize(values.get("pageWidth"), Size.class),
                Csv.deserialize(values.get("pageHeight"), Size.class),
                new ArrayList<BaseTemplateArea>())
    }

    def interactiveFlowName = Csv.deserialize(values.get("interactiveFlowName"), String.class)
    if (!interactiveFlowName) {
        throw new IllegalStateException("Rows of type 'Base' must specify an interactiveFlowName for the consolidated area.")
    }
    def x = Csv.deserialize(values.get("x"), Size.class)
    def y = Csv.deserialize(values.get("y"), Size.class)
    def width = Csv.deserialize(values.get("width"), Size.class)
    def height = Csv.deserialize(values.get("height"), Size.class)
    def position = (x != null && y != null && width != null && height != null) ? new Position(x, y, width, height) : null
    def flowToNextPage = Csv.deserialize(values.get("flowToNextPage"), Boolean.class) ?: false

    page.areas.add(new BaseTemplateArea(interactiveFlowName, position, flowToNextPage))
}

private static void applyDocumentObjectTargetIdMappings(Migration migration, Map<String, String> docObjectsToTargetIds) {
    if (docObjectsToTargetIds.isEmpty()) return

    def mappings = new HashMap<String, MappingItem>()
    docObjectsToTargetIds.each { documentObjectId, targetId ->
        def mapping = migration.mappingRepository.getDocumentObjectMapping(documentObjectId)
        def existingObject = migration.documentObjectRepository.find(documentObjectId)

        if (mapping.name == null) mapping.name = existingObject?.name
        if (mapping.internal == null) mapping.internal = existingObject?.internal
        if (mapping.targetFolder == null) mapping.targetFolder = existingObject?.targetFolder
        if (mapping.variableStructureRef == null) mapping.variableStructureRef = existingObject?.variableStructureRef?.id
        if (mapping.skip == null) mapping.skip = existingObject?.skip

        mapping.baseTemplate = Csv.deserialize(targetId, BaseTemplateLocation.class)
        mappings[documentObjectId] = mapping
    }

    Mapping.upsertBatched(migration.mappingRepository, mappings, "document object base template ref mappings", log)
    migration.mappingRepository.applyAllDocumentObjectMappings()
}

private static void applyBaseTemplateDraftMappings(Migration migration, Map<String, BaseTemplateDraft> baseTemplateDrafts) {
    if (baseTemplateDrafts.isEmpty()) return

    def mappings = new HashMap<String, MappingItem>()
    baseTemplateDrafts.each { baseTemplateId, draft ->
        def existing = migration.baseTemplateRepository.find(baseTemplateId)
        if (existing == null) {
            migration.baseTemplateRepository.upsert(new BaseTemplate(baseTemplateId, null, [], new CustomFieldMap(new HashMap<String, String>()), null, [], null, null))
        }

        def mapping = migration.mappingRepository.getBaseTemplateMapping(baseTemplateId)
        mapping.name = draft.name ?: existing?.name
        mapping.targetFolder = existing?.targetFolder
        mapping.pages = new ArrayList<BaseTemplatePage>(draft.pages.values())

        mappings[baseTemplateId] = mapping
    }

    Mapping.upsertBatched(migration.mappingRepository, mappings, "base template mappings", log)
    migration.mappingRepository.applyAllBaseTemplateMappings()
}

class BaseTemplateDraft {
    String name
    Map<String, BaseTemplatePage> pages = new LinkedHashMap<>()
}
