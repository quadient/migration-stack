//! ---
//! category: Mapping
//! description: Export areas assigned to the appropriate pages and templates. Allows user to modify interactive flow name
//! target: gradle
//! ---
package com.quadient.migration.example.common.mapping

import com.quadient.migration.api.Migration
import com.quadient.migration.api.dto.migrationmodel.*
import com.quadient.migration.api.dto.migrationmodel.builder.DocumentObjectFilterBuilder
import com.quadient.migration.api.repository.DocumentObjectRepository
import com.quadient.migration.example.common.util.Csv
import com.quadient.migration.shared.DocumentObjectType
import groovy.transform.Field

import java.nio.file.Path
import java.nio.file.Paths

import static com.quadient.migration.example.common.util.InitMigration.initMigration

@Field Migration migration = initMigration(this.binding.variables["args"])

def areasFile = Paths.get("mapping", "${migration.projectConfig.name}-areas.csv")

run(migration, areasFile)

static void run(Migration migration, Path path) {
    def areasFile = path.toFile()

    areasFile.createParentDirectories()

    def templatesAndPages = (migration.documentObjectRepository as DocumentObjectRepository).list(new DocumentObjectFilterBuilder().types([DocumentObjectType.Page, DocumentObjectType.Template]).build())
    def templates = templatesAndPages.findAll { it.type == DocumentObjectType.Template }
    def pages = templatesAndPages.findAll { it.type == DocumentObjectType.Page }
    def pageIds = pages.collect { it.id }
    def usedPageIds = new ArrayList<String>()

    areasFile.withWriter { writer ->
        writer.writeLine("templateId,templateName,pageId,pageName,interactiveFlowName,x,y,width,height,contentPreview")
        templates.each { template ->
            def templatePageRefs = template.content.findAll { it instanceof DocumentObjectRef && pageIds.contains(it.id) }
            templatePageRefs.each { templatePageRef ->
                def pageId = (templatePageRef as DocumentObjectRef).id
                def page = pages.find { it.id == pageId }

                usedPageIds.add(pageId)
                def areas = page.content.findAll { it instanceof Area } as List<Area>
                areas.eachWithIndex { area, idx ->
                    writer.writeLine(buildArea(migration, idx, area, page, template))
                }
            }
        }

        if (usedPageIds.size() != pageIds.size()) {
            def unusedPageIds = pageIds.findAll { !usedPageIds.contains(it) }

            unusedPageIds.each { unusedPageId ->
                def page = pages.find { it.id == unusedPageId }

                def areas = page.content.findAll { it instanceof Area } as List<Area>
                areas.eachWithIndex { area, idx -> writer.writeLine(buildArea(migration, idx, area, page, null)) }
            }
        }
    }
}

static String buildArea(Migration migration, Number idx, Area area, DocumentObject page, DocumentObject template) {
    def mapping = migration.mappingRepository.getAreaMapping(page.id)

    def builder = new StringBuilder()
    builder.append(Csv.serialize(template?.id) + ",")
    builder.append(Csv.serialize(template?.name) + ",")
    builder.append(Csv.serialize(page.id) + ",")
    builder.append(Csv.serialize(page.name) + ",")
    builder.append(Csv.serialize(mapping?.areas?.get(idx) ?: area.interactiveFlowName) + ",")
    builder.append(Csv.serialize(area.position.x) + ",")
    builder.append(Csv.serialize(area.position.y) + ",")
    builder.append(Csv.serialize(area.position.width) + ",")
    builder.append(Csv.serialize(area.position.height) + ",")

    def documentObjectIdsToNames = getDocumentObjectIdsToNames(migration, area.content)
    def imageIdsToNames = getImageIdsToNames(migration, area.content)

    builder.append(Csv.serialize(area.content.collect {
        switch (it) {
            case DocumentObjectRef:
                def id = it.id
                def name = documentObjectIdsToNames.get(id)
                if (name == null) {
                    return "DocumentObjectRef(id=$id)"
                } else {
                    return "DocumentObjectRef(id=$id name=$name)"
                }
            case ImageRef:
                def id = it.id
                def name = imageIdsToNames.get(id)
                if (name == null) {
                    return "ImageRef(id=$id)"
                } else {
                    return "ImageRef(id=$id name=$name)"
                }
            default:
                return it.toString()
        }
    }.join(";").replace(",", " ")))
    return builder.toString()
}

static HashMap<String, String> getDocumentObjectIdsToNames(Migration migration, List<DocumentContent> content) {
    def idsToNames = new HashMap<String, String>()

    def refIds = content.findAll { it instanceof DocumentObjectRef }.collect { (it as Ref).id }
    refIds.each {
        def documentObject = migration.documentObjectRepository.find(it)
        if (documentObject == null) {
            throw new IllegalStateException("Document object '$it' not found.")
        }

        if (documentObject.name != null) {
            idsToNames[it] = documentObject.name
        }
    }

    return idsToNames
}

static HashMap<String, String> getImageIdsToNames(Migration migration, List<DocumentContent> content) {
    def idsToNames = new HashMap<String, String>()

    def refIds = content.findAll { it instanceof ImageRef }.collect { (it as Ref).id }
    refIds.each {
        def image = migration.imageRepository.find(it)
        if (image == null) {
            throw new IllegalStateException("Image '$it' not found.")
        }

        if (image.name != null) {
            idsToNames[it] = image.name
        }
    }

    return idsToNames
}
