package com.quadient.migration.example.common.mapping

import com.quadient.migration.api.Migration
import com.quadient.migration.api.dto.migrationmodel.Area
import com.quadient.migration.api.dto.migrationmodel.DocumentContent
import com.quadient.migration.api.dto.migrationmodel.DocumentObject
import com.quadient.migration.api.dto.migrationmodel.DocumentObjectRef
import com.quadient.migration.api.dto.migrationmodel.ImageRef
import com.quadient.migration.api.dto.migrationmodel.Ref
import com.quadient.migration.api.dto.migrationmodel.builder.DocumentObjectFilterBuilder
import com.quadient.migration.api.repository.DocumentObjectRepository
import com.quadient.migration.example.common.util.Csv
import com.quadient.migration.shared.DocumentObjectType
import groovy.transform.Field

import java.nio.file.Paths

import static com.quadient.migration.example.common.util.InitMigration.initMigration

@Field Migration migration = initMigration(this.binding.variables["args"])

def areasFile = Paths.get("mapping", "${migration.projectConfig.name}-areas.csv").toFile()
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
            areas.each { area -> writer.writeLine(buildArea(area, page, template)) }
        }
    }

    if (usedPageIds.size() != pageIds.size()) {
        def unusedPageIds = pageIds.findAll { !usedPageIds.contains(it) }

        unusedPageIds.each { unusedPageId ->
            def page = pages.find { it.id == unusedPageId }

            def areas = page.content.findAll { it instanceof Area } as List<Area>
            areas.each { area -> writer.writeLine(buildArea(area, page, null)) }
        }
    }
}

String buildArea(Area area, DocumentObject page, DocumentObject template) {
    def builder = new StringBuilder()
    builder.append(Csv.serialize(template?.id) + ",")
    builder.append(Csv.serialize(template?.name) + ",")
    builder.append(Csv.serialize(page.id) + ",")
    builder.append(Csv.serialize(page.name) + ",")
    builder.append(Csv.serialize(area.interactiveFlowName) + ",")
    builder.append(Csv.serialize(area.position.x) + ",")
    builder.append(Csv.serialize(area.position.y) + ",")
    builder.append(Csv.serialize(area.position.width) + ",")
    builder.append(Csv.serialize(area.position.height) + ",")

    def documentObjectIdsToNames = getDocumentObjectIdsToNames(area.content)
    def imageIdsToNames = getImageIdsToNames(area.content)

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

HashMap<String, String> getDocumentObjectIdsToNames(List<DocumentContent> content) {
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

HashMap<String, String> getImageIdsToNames(List<DocumentContent> content) {
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
