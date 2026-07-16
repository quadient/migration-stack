//! ---
//! displayName: Export Areas
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
import com.quadient.migration.example.common.util.Mapping
import com.quadient.migration.shared.DocumentObjectType
import groovy.transform.Field

import java.nio.file.Path

import static com.quadient.migration.example.common.util.InitMigration.initMigration

@Field Migration migration = initMigration(this.binding)

def areasFile = Mapping.csvPath(binding, migration.projectConfig.name, "areas")

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
        def headers = [
            Mapping.displayHeader("templateId", false),
            Mapping.displayHeader("templateName", true),
            Mapping.displayHeader("pageId", false),
            Mapping.displayHeader("pageName", true),
            Mapping.displayHeader("pageWidth", true),
            Mapping.displayHeader("pageHeight", true),
            Mapping.displayHeader("interactiveFlowName", false),
            Mapping.displayHeader("flowToNextPage", false),
            Mapping.displayHeader("x", true),
            Mapping.displayHeader("y", true),
            Mapping.displayHeader("width", true),
            Mapping.displayHeader("height", true),
            Mapping.displayHeader("contentPreview", true)
        ]
        writer.writeLine(headers.join(","))
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

            def directAreas = template.content.findAll { it instanceof Area } as List<Area>
            directAreas.eachWithIndex { area, idx ->
                writer.writeLine(buildArea(migration, idx, area, null, template))
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

    def builder = new StringBuilder()
    builder.append(Csv.serialize(template?.id) + ",")
    builder.append(Csv.serialize(template?.name) + ",")
    builder.append(Csv.serialize(page?.id) + ",")
    builder.append(Csv.serialize(page?.name) + ",")
    def pageOptions = page?.options instanceof PageOptions ? page.options as PageOptions : null
    builder.append(Csv.serialize(pageOptions?.width) + ",")
    builder.append(Csv.serialize(pageOptions?.height) + ",")
    builder.append(Csv.serialize(area.interactiveFlowName) + ",")
    builder.append(Csv.serialize(area.flowToNextPage) + ",")
    builder.append(Csv.serialize(area.position.x) + ",")
    builder.append(Csv.serialize(area.position.y) + ",")
    builder.append(Csv.serialize(area.position.width) + ",")
    builder.append(Csv.serialize(area.position.height) + ",")

    builder.append(Csv.serialize(migration.previewProvider.buildDocumentContentListPreview(area.content)))

    return builder.toString()
}
