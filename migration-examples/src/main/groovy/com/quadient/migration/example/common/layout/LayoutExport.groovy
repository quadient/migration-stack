//! ---
//! displayName: Layout Export
//! category: Layout
//! description: Export raw layout data — pages, template membership, areas and containment — as JSON for layout/index.html.
//! ---
package com.quadient.migration.example.common.layout

import com.fasterxml.jackson.core.util.DefaultIndenter
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter
import com.fasterxml.jackson.databind.ObjectMapper
import com.quadient.migration.api.Migration
import com.quadient.migration.api.dto.migrationmodel.Area
import com.quadient.migration.api.dto.migrationmodel.DocumentObject
import com.quadient.migration.api.dto.migrationmodel.DocumentObjectRef
import com.quadient.migration.api.dto.migrationmodel.Image
import com.quadient.migration.api.dto.migrationmodel.ImageRef
import com.quadient.migration.api.dto.migrationmodel.builder.DocumentObjectFilterBuilder
import com.quadient.migration.example.common.util.PathUtil
import com.quadient.migration.shared.PageOptions
import com.quadient.migration.shared.DocumentObjectType
import groovy.transform.Field

import static com.quadient.migration.example.common.util.InitMigration.initMigration

@Field static final double MAX_X_MM = 315.0           // 1.5 × A4 width — off-page area guard
@Field static final double CONTAINMENT_TOL_MM = 0.7   // tolerance (~2 pt) for containment checks

@Field Migration migration = initMigration(this.binding)

def dstFile = PathUtil.dataDirPath(binding, "layout", "${migration.projectConfig.name}-layout.json").toFile()
dstFile.parentFile.mkdirs()

List<DocumentObject> templatesAndPages = migration.documentObjectRepository
        .list(new DocumentObjectFilterBuilder().types([DocumentObjectType.Page, DocumentObjectType.Template]).build())

List<DocumentObject> templates = templatesAndPages.findAll { it.type == DocumentObjectType.Template } as List<DocumentObject>
List<DocumentObject> pages = templatesAndPages.findAll { it.type == DocumentObjectType.Page } as List<DocumentObject>
Map<String, DocumentObject> pageById = pages.collectEntries { [(it.id): it] } as Map<String, DocumentObject>
List<PageEntry> pageEntries = []
Set<String> assignedPageIds = []

// Pre-cache content references for buildContentPreview
Set<String> allDocObjRefIds = [] as Set<String>
Set<String> allImageRefIds = [] as Set<String>
(templates + pages).each { DocumentObject obj ->
    obj.content.findAll { it instanceof Area }.each { area ->
        (area as Area).content.each {
            contentItem ->
                if (contentItem instanceof DocumentObjectRef) allDocObjRefIds << contentItem.id
                else if (contentItem instanceof ImageRef) allImageRefIds << contentItem.id
        }
    }
}

Map<String, DocumentObject> docObjCache = allDocObjRefIds
        ? migration.documentObjectRepository.list(new DocumentObjectFilterBuilder().ids(allDocObjRefIds.toList()).build()).collectEntries { [(it.id): it] } as Map<String, DocumentObject>
        : [:] as Map<String, DocumentObject>
Map<String, Image> imageCache = allImageRefIds
        ? migration.imageRepository.listAll().findAll { allImageRefIds.contains(it.id) }.collectEntries { [(it.id): it] } as Map<String, Image>
        : [:] as Map<String, Image>

templates.each { DocumentObject tmpl ->
    tmpl.content
            .findAll { it instanceof DocumentObjectRef && pageById.containsKey(it.id) }
            .eachWithIndex { ref, int idx ->
                DocumentObject page = pageById[(ref as DocumentObjectRef).id]
                assignedPageIds << page.id
                List<AreaEntry> areas = processAreas(docObjCache, imageCache, page.content.findAll { it instanceof Area } as List<Area>)
                pageEntries << new PageEntry(pageId: page.id,
                        pageName: page.name,
                        pageSize: pageSize(page),
                        templateId: tmpl.id,
                        templateName: tmpl.name,
                        templatePageIndex: idx,
                        areas: areas)
            }

    List<Area> directAreas = tmpl.content.findAll { it instanceof Area } as List<Area>
    if (directAreas) {
        List<AreaEntry> areas = processAreas(docObjCache, imageCache, directAreas)
        pageEntries << new PageEntry(pageId: null,
                pageName: null,
                pageSize: null,
                templateId: tmpl.id,
                templateName: tmpl.name,
                templatePageIndex: null,
                areas: areas)
    }
}

pages.findAll { !assignedPageIds.contains(it.id) }.each { DocumentObject page ->
    List<AreaEntry> areas = processAreas(docObjCache, imageCache, page.content.findAll { it instanceof Area } as List<Area>)
    pageEntries << new PageEntry(pageId: page.id,
            pageName: page.name,
            pageSize: pageSize(page),
            templateId: null,
            templateName: null,
            templatePageIndex: null,
            areas: areas)
}

// Group page-list indices by their parent template
Map<String, List<Integer>> templateGroupMap = [:]
pageEntries.eachWithIndex { PageEntry pageEntry, int i ->
    String key = pageEntry.templateId ?: "orphan:${pageEntry.pageId}"
    templateGroupMap.computeIfAbsent(key) { [] } << i
}

List<TemplateEntry> templateList = templateGroupMap.collect { String key, List<Integer> indices ->
    List<Integer> pageIndices = indices.sort { int pageIndex -> templatePageOrder(pageEntries[pageIndex]) }
    PageEntry first = pageEntries[pageIndices[0]]
    new TemplateEntry(templateId: first.templateId ?: key,
            templateName: first.templateName ?: first.templateId ?: key,
            pageIndices: pageIndices)
} as List<TemplateEntry>

new ObjectMapper().writer(layoutJsonPrettyPrinter()).writeValue(dstFile,
        [projectName: migration.projectConfig.name,
         pages      : pageEntries,
         templates  : templateList])
println "Written to: ${dstFile.absolutePath}"

// --- Helper methods ---

static DefaultPrettyPrinter layoutJsonPrettyPrinter() {
    DefaultPrettyPrinter printer = new DefaultPrettyPrinter()
    printer.indentArraysWith(DefaultIndenter.SYSTEM_LINEFEED_INSTANCE)
    printer
}

/** Processes raw areas into the structured area format used by the JSON output. */
static List<AreaEntry> processAreas(Map<String, DocumentObject> docObjCache, Map<String, Image> imageCache, List<Area> modelAreas) {
    List<WorkingArea> workingAreas = modelAreas
            .findAll { area -> area.position != null && area.position.width.toMillimeters() > 0 && area.position.height.toMillimeters() > 0 && area.position.x.toMillimeters() < MAX_X_MM
            }
            .collect { Area area ->
                double x = area.position.x.toMillimeters()
                double y = area.position.y.toMillimeters()
                double w = area.position.width.toMillimeters()
                double h = area.position.height.toMillimeters()
                new WorkingArea(x: x,
                        y: y,
                        w: w,
                        h: h,
                        x2: x + w,
                        y2: y + h,
                        areaSize: w * h,
                        flowToNextPage: area.flowToNextPage,
                        interactiveFlowName: area.interactiveFlowName ?: "",
                        contentPreview: buildContentPreview(docObjCache, imageCache, area))
            } as List<WorkingArea>

    Map<Integer, Integer> containment = findContainment(workingAreas)

    List<AreaEntry> areas = workingAreas.withIndex().collect { WorkingArea a, int i ->
        new AreaEntry(x: round2dp(a.x), y: round2dp(a.y), w: round2dp(a.w), h: round2dp(a.h),
                flowToNextPage: a.flowToNextPage,
                interactiveFlowName: a.interactiveFlowName,
                contentPreview: a.contentPreview,
                containedIn: containment[i])
    } as List<AreaEntry>
    areas
}

static String buildContentPreview(Map<String, DocumentObject> docObjCache, Map<String, Image> imageCache, Area area) {
    area.content.collect { c ->
        switch (c) {
            case DocumentObjectRef:
                def obj = docObjCache[c.id]
                return obj?.name ? "DocObjRef(${obj.name})" : "DocObjRef(${c.id})"
            case ImageRef:
                def img = imageCache[c.id]
                return img?.name ? "ImageRef(${img.name})" : "ImageRef(${c.id})"
            default:
                return c.class.simpleName
        }
    }.join("; ")
}

/**
 * For each area find its smallest enclosing parent within the same page.
 * Returns a map of list-index → list-index (parent). */
static Map<Integer, Integer> findContainment(List<WorkingArea> areas) {
    Map<Integer, Integer> containment = [:]
    areas.eachWithIndex { WorkingArea inner, int i ->
        areas.eachWithIndex { WorkingArea outer, int j ->
            if (i == j || outer.areaSize <= inner.areaSize) return
            if (outer.x - CONTAINMENT_TOL_MM <= inner.x && outer.y - CONTAINMENT_TOL_MM <= inner.y && inner.x2 <= outer.x2 + CONTAINMENT_TOL_MM && inner.y2 <= outer.y2 + CONTAINMENT_TOL_MM) {
                Integer cur = containment[i]
                if (cur == null || areas[cur].areaSize > outer.areaSize) {
                    containment[i] = j
                }
            }
        }
    }
    return containment
}

static double round2dp(double v) { Math.round(v * 100) / 100.0 }

static int templatePageOrder(PageEntry page) {
    page.templatePageIndex != null ? page.templatePageIndex : Integer.MAX_VALUE
}

static PageSizeEntry pageSize(DocumentObject page) {
    PageOptions options = page.options instanceof PageOptions ? page.options as PageOptions : null
    if (options?.width == null || options?.height == null) {
        return null
    }
    return new PageSizeEntry(
            w: round2dp(options.width.toMillimeters()),
            h: round2dp(options.height.toMillimeters()))
}

class WorkingArea {
    double x
    double y
    double w
    double h
    double x2
    double y2
    double areaSize
    boolean flowToNextPage
    String interactiveFlowName
    String contentPreview
}

class AreaEntry {
    double x
    double y
    double w
    double h
    boolean flowToNextPage
    String interactiveFlowName
    String contentPreview
    Integer containedIn
}

class PageEntry {
    String pageId
    String pageName
    PageSizeEntry pageSize
    String templateId
    String templateName
    Integer templatePageIndex
    List<AreaEntry> areas
}

class PageSizeEntry {
    double w
    double h
}

class TemplateEntry {
    String templateId
    String templateName
    List<Integer> pageIndices
}
