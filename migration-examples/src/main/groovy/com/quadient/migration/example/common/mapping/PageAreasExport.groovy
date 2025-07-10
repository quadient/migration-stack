package com.quadient.migration.example.common.mapping

import com.quadient.migration.api.dto.migrationmodel.Area
import com.quadient.migration.api.dto.migrationmodel.DocumentObject
import com.quadient.migration.api.dto.migrationmodel.DocumentObjectRef
import com.quadient.migration.api.dto.migrationmodel.builder.DocumentObjectFilterBuilder
import com.quadient.migration.api.repository.DocumentObjectRepository
import com.quadient.migration.example.common.util.Csv
import com.quadient.migration.shared.DocumentObjectType

import java.nio.file.Paths

import static com.quadient.migration.example.common.util.InitMigration.initMigration

def migration = initMigration(this.binding.variables["args"])

def pageAreasFile = Paths.get("mapping", "${migration.projectConfig.name}-page-areas.csv").toFile()
pageAreasFile.createParentDirectories()

def templatesAndPages = (migration.documentObjectRepository as DocumentObjectRepository).list(new DocumentObjectFilterBuilder().types([DocumentObjectType.Page, DocumentObjectType.Template]).build())
def templates = templatesAndPages.findAll { it.type == DocumentObjectType.Template }
def pages = templatesAndPages.findAll { it.type == DocumentObjectType.Page }
def pageIds = pages.collect { it.id }
def usedPageIds = new ArrayList<String>()

pageAreasFile.withWriter { writer ->
    writer.writeLine("id,name,type,interactiveFlowName,x,y,width,height,contentPreview")
    templates.each { template ->
        def templateBuilder = new StringBuilder()
        templateBuilder.append(Csv.serialize(template.id))
        templateBuilder.append("," + Csv.serialize(template.name))
        templateBuilder.append("," + Csv.serialize(template.type))
        writer.writeLine(templateBuilder.toString())

        def templatePageRefs = template.content.findAll { it instanceof DocumentObjectRef && pageIds.contains(it.id) }
        templatePageRefs.each { templatePageRef ->
            def templatePageId = (templatePageRef as DocumentObjectRef).id
            def templatePage = pages.find { it.id == templatePageId }
            writer.writeLine(buildPage(templatePage))

            usedPageIds.add(templatePageId)
            def pageAreas = templatePage.content.findAll { it instanceof Area } as List<Area>
            pageAreas.each { area -> writer.writeLine(buildArea(area)) }
        }
    }

    if (usedPageIds.size() != pageIds.size()) {
        def unusedPageIds = pageIds.findAll { !usedPageIds.contains(it) }

        writer.writeLine("unusedPages")
        unusedPageIds.each { unusedPageId ->
            def page = pages.find { it.id == unusedPageId }
            writer.writeLine(buildPage(page))

            def pageAreas = page.content.findAll { it instanceof Area } as List<Area>
            pageAreas.each { area -> writer.writeLine(buildArea(area)) }
        }
    }
}

static String buildArea(Area area) {
    def builder = new StringBuilder()
    builder.append(",")
    builder.append("," + Csv.serialize("Area"))
    builder.append("," + Csv.serialize(area.interactiveFlowName))
    builder.append("," + Csv.serialize(area.position.x))
    builder.append("," + Csv.serialize(area.position.y))
    builder.append("," + Csv.serialize(area.position.width))
    builder.append("," + Csv.serialize(area.position.height))
    builder.append("," + Csv.serialize(area.content.collect { it.toString().replace(",", " ") }.join(";")))
    return builder.toString()
}

static String buildPage(DocumentObject page) {
    def builder = new StringBuilder()
    builder.append(Csv.serialize(page.id))
    builder.append("," + Csv.serialize(page.name))
    builder.append("," + Csv.serialize(page.type))
    return builder.toString()
}
