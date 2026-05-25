//! ---
//! displayName: Layout Export
//! category: Layout
//! description: Export layout data — areas, containment, proximity groups, cross-page similarity and base-template candidates — as JSON for layout/index.html.
//! ---
package com.quadient.migration.example.common.layout

import com.fasterxml.jackson.databind.ObjectMapper
import com.quadient.migration.api.Migration
import com.quadient.migration.api.dto.migrationmodel.Area
import com.quadient.migration.api.dto.migrationmodel.DocumentObject
import com.quadient.migration.api.dto.migrationmodel.DocumentObjectRef
import com.quadient.migration.api.dto.migrationmodel.ImageRef
import com.quadient.migration.api.dto.migrationmodel.builder.DocumentObjectFilterBuilder
import com.quadient.migration.example.common.util.PathUtil
import com.quadient.migration.shared.DocumentObjectType
import groovy.transform.Field

import static com.quadient.migration.example.common.util.InitMigration.initMigration

@Field Migration migration = initMigration(LayoutExport.binding)

def dstFile = PathUtil.dataDirPath(binding, "layout", "${migration.projectConfig.name}-layout.json").toFile()
dstFile.parentFile.mkdirs()

List<DocumentObject> templatesAndPages = migration.documentObjectRepository
        .list(new DocumentObjectFilterBuilder().types([DocumentObjectType.Page, DocumentObjectType.Template]).build())

List<DocumentObject> templates = templatesAndPages.findAll { it.type == DocumentObjectType.Template } as List<DocumentObject>
List<DocumentObject> pages = templatesAndPages.findAll { it.type == DocumentObjectType.Page } as List<DocumentObject>
Map<String, DocumentObject> pageById = pages.collectEntries { [(it.id): it] } as Map<String, DocumentObject>
List<PageEntry> pageEntries = []
Set<String> assignedPageIds = []

templates.each { DocumentObject tmpl ->
    tmpl.content
            .findAll { it instanceof DocumentObjectRef && pageById.containsKey(it.id) }
            .eachWithIndex { ref, int idx ->
                DocumentObject page = pageById[(ref as DocumentObjectRef).id]
                assignedPageIds << page.id
                ProcessedAreasAndGroups areasAndGroups = processAreasAndGroups(migration, page.content.findAll { it instanceof Area } as List<Area>)
                pageEntries << new PageEntry(pageId: page.id,
                        pageName: page.name,
                        templateId: tmpl.id,
                        templateName: tmpl.name,
                        templatePageIndex: idx,
                        areas: areasAndGroups.areas,
                        proximityGroups: areasAndGroups.proximityGroups)
            }

    List<Area> directAreas = tmpl.content.findAll { it instanceof Area } as List<Area>
    if (directAreas) {
        ProcessedAreasAndGroups processed = processAreasAndGroups(migration, directAreas)
        pageEntries << new PageEntry(pageId: tmpl.id,
                pageName: tmpl.name,
                templateId: tmpl.id,
                templateName: tmpl.name,
                templatePageIndex: null,
                areas: processed.areas,
                proximityGroups: processed.proximityGroups)
    }
}

pages.findAll { !assignedPageIds.contains(it.id) }.each { DocumentObject page ->
    ProcessedAreasAndGroups processed = processAreasAndGroups(migration, page.content.findAll { it instanceof Area } as List<Area>)
    pageEntries << new PageEntry(pageId: page.id,
            pageName: page.name,
            templateId: null,
            templateName: null,
            templatePageIndex: null,
            areas: processed.areas,
            proximityGroups: processed.proximityGroups)
}

int pageCount = pageEntries.size()

List<List<Double>> pageMatrix = (0..<pageCount).collect { int i ->
    (0..<pageCount).collect { int j ->
        if (i == j) return 1.0d
        if (j < i) return 0.0d
        round3dp(pageSimilarity(pageEntries[i].areas, pageEntries[j].areas))
    }
} as List<List<Double>>
(0..<pageCount).each { i -> (0..<i).each { j -> pageMatrix[i][j] = pageMatrix[j][i] } }

// Group page-list indices by their parent template, preserving intra-template page order
Map<String, List<TemplateGroupEntry>> templateGroupMap = [:]
pageEntries.eachWithIndex { PageEntry pg, int i ->
    String key = pg.templateId ?: "orphan:${pg.pageId}"
    if (!templateGroupMap.containsKey(key)) templateGroupMap[key] = []
    templateGroupMap[key] << new TemplateGroupEntry(listIdx: i, order: pg.templatePageIndex ?: 0)
}

List<TemplateEntry> templateList = templateGroupMap.collect { String key, List<TemplateGroupEntry> entries ->
    List<Integer> pageIndices = entries.sort { it.order }.collect { it.listIdx } as List<Integer>
    PageEntry firstPage = pageEntries[pageIndices[0]]
    new TemplateEntry(templateId: firstPage.templateId ?: key,
            templateName: firstPage.templateName ?: firstPage.templateId ?: key,
            pageIndices: pageIndices)
} as List<TemplateEntry>

int templateCount = templateList.size()
List<List<Double>> templateMatrix = (0..<templateCount).collect { int i ->
    (0..<templateCount).collect { int j ->
        if (i == j) return 1.0d
        if (j < i) return 0.0d
        round3dp(templateSimilarity(templateList[i].pageIndices,
                templateList[j].pageIndices,
                pageMatrix))
    }
} as List<List<Double>>
(0..<templateCount).each { i -> (0..<i).each { j -> templateMatrix[i][j] = templateMatrix[j][i] } }

new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(dstFile,
        [projectName: migration.projectConfig.name,
         pages      : pageEntries,
         similarity : [pageLevel    : [matrix: pageMatrix],
                       templateLevel: [templates: templateList, matrix: templateMatrix]]])
println "Written to: ${dstFile.absolutePath}"

/** Processes raw areas into the structured area + proximity-group format used by the JSON output. */
static ProcessedAreasAndGroups processAreasAndGroups(Migration migration, List<Area> modelAreas) {
    List<WorkingArea> workingAreas = modelAreas
            .findAll { area ->
                area.position != null &&
                area.position.width.toMillimeters() > 0 &&
                area.position.height.toMillimeters() > 0 &&
                area.position.x.toMillimeters() < 315.0   // 1.5 × A4 width guard
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
                        contentPreview: buildContentPreview(migration, area))
            } as List<WorkingArea>

    Map<Integer, Integer> containment = findContainment(workingAreas)

    List<List<Integer>> groups = groupByProximity(workingAreas)
    groups.eachWithIndex { List<Integer> groupIndices, int gi -> groupIndices.each { int idx -> workingAreas[idx].proximityGroup = gi } }

    List<ProximityGroupEntry> proximityGroups = groups.collect { List<Integer> groupIndices ->
        List<WorkingArea> subset = groupIndices.collect { workingAreas[it] } as List<WorkingArea>
        double bx = subset.min { it.x }.x
        double by = subset.min { it.y }.y
        double bx2 = subset.max { it.x2 }.x2
        double by2 = subset.max { it.y2 }.y2
        new ProximityGroupEntry(areaIndices: groupIndices,
                position: new Position(x: round2dp(bx), y: round2dp(by), w: round2dp(bx2 - bx), h: round2dp(by2 - by)))
    } as List<ProximityGroupEntry>

    List<AreaEntry> areas = workingAreas.withIndex().collect { WorkingArea a, int i ->
        new AreaEntry(x: round2dp(a.x), y: round2dp(a.y), w: round2dp(a.w), h: round2dp(a.h),
                flowToNextPage: a.flowToNextPage,
                interactiveFlowName: a.interactiveFlowName,
                contentPreview: a.contentPreview,
                containedIn: containment[i],   // list index of parent area, or null if top-level
                proximityGroup: a.proximityGroup ?: 0)
    } as List<AreaEntry>

    new ProcessedAreasAndGroups(areas: areas, proximityGroups: proximityGroups)
}

static String buildContentPreview(Migration migration, Area area) {
    area.content.collect { c ->
        switch (c) {
            case DocumentObjectRef:
                def obj = migration.documentObjectRepository.find(c.id)
                return obj?.name ? "DocObjRef(${obj.name})" : "DocObjRef(${c.id})"
            case ImageRef:
                def img = migration.imageRepository.find(c.id)
                return img?.name ? "ImageRef(${img.name})" : "ImageRef(${c.id})"
            default:
                return c.class.simpleName
        }
    }.join("; ")
}

/**
 * For each area find its smallest enclosing parent within the same page.
 * Returns a map of list-index → list-index (parent).
 * Tolerance of 0.7 mm (~2 pt) absorbs rounding differences.*/
static Map<Integer, Integer> findContainment(List<WorkingArea> areas) {
    Map<Integer, Integer> containment = [:]
    areas.eachWithIndex { WorkingArea inner, int i ->
        areas.eachWithIndex { WorkingArea outer, int j ->
            if (i == j || outer.areaSize <= inner.areaSize) return
            double tol = 0.7
            if (outer.x - tol <= inner.x && outer.y - tol <= inner.y && inner.x2 <= outer.x2 + tol && inner.y2 <= outer.y2 + tol) {
                Integer cur = containment[i]
                if (cur == null || areas[cur].areaSize > outer.areaSize) {
                    containment[i] = j
                }
            }
        }
    }
    return containment
}

/**
 * Vertical sweep grouping: areas whose Y ranges overlap or are within
 * groupingGap of each other are placed in the same group.
 * Returns a list of groups, each group being a list of list-indices.*/
static List<List<Integer>> groupByProximity(List<WorkingArea> workingAreas, double groupingGap = 5.3) {
    if (!workingAreas) return []
    List<Integer> sortedIndices = (0..<workingAreas.size()).sort { int i, int j ->
        workingAreas[i].y <=> workingAreas[j].y ?: workingAreas[i].x <=> workingAreas[j].x
    }
    List<List<Integer>> groups = []
    List<Integer> current = [sortedIndices[0]]
    double curY2 = workingAreas[sortedIndices[0]].y2
    sortedIndices.drop(1).each { int idx ->
        WorkingArea area = workingAreas[idx]
        if (area.y <= curY2 + groupingGap) {
            current << idx
            curY2 = Math.max(curY2, area.y2)
        } else {
            groups << current
            current = [idx]
            curY2 = area.y2
        }
    }
    groups << current
    return groups
}

/**
 * Compare two pages by greedy bipartite matching on area geometry.
 * Each area is described by (x, width) normalised to pageRefMm and
 * height with a log-ratio so a 3× difference scores ~1.0 (large
 * height differences are tolerated but still weakly penalised).
 * Unmatched areas (different counts) reduce the score proportionally.*/
static double pageSimilarity(List<AreaEntry> areasA, List<AreaEntry> areasB,
                             double pageRefMm = 210.0, double matchThreshold = 0.5) {
    List<AreaEntry> validA = areasA.findAll { it.w > 0 && it.h > 0 && it.x < pageRefMm * 1.5 } as List<AreaEntry>
    List<AreaEntry> validB = areasB.findAll { it.w > 0 && it.h > 0 && it.x < pageRefMm * 1.5 } as List<AreaEntry>
    if (!validA || !validB) return 0.0

    List<AreaMatch> areaMatches = []
    validA.eachWithIndex { AreaEntry a, int i ->
        validB.eachWithIndex { AreaEntry b, int j ->
            double xDist = Math.abs(a.x - b.x) / pageRefMm
            double wDist = Math.abs(a.w - b.w) / pageRefMm
            double hRatio = Math.abs(Math.log(Math.max(a.h, 0.1) / Math.max(b.h, 0.1))) / Math.log(3.0)
            areaMatches << new AreaMatch(distance: Math.sqrt(xDist * xDist + wDist * wDist + hRatio * hRatio), indexA: i, indexB: j)
        }
    }
    areaMatches.sort { it.distance }

    Set<Integer> matchedA = []
    Set<Integer> matchedB = []
    double total = 0.0
    areaMatches.each { AreaMatch match ->
        if (!(match.indexA in matchedA) && !(match.indexB in matchedB)) {
            matchedA << match.indexA; matchedB << match.indexB
            total += Math.max(0.0, 1.0 - match.distance / matchThreshold)
        }
    }
    int slots = Math.max(validA.size(), validB.size())
    return slots > 0 ? total / slots : 0.0
}

static double round2dp(double v) { Math.round(v * 100) / 100.0 }
static double round3dp(double v) { Math.round(v * 1000) / 1000.0 }

/**
 * Compare two templates by positional page matching (page 1↔1, page 2↔2, …).
 * Unmatched pages (different template lengths) contribute 0, penalising the score
 * proportionally so longer templates aren't artificially favoured.*/
static double templateSimilarity(List<Integer> pagesA, List<Integer> pagesB,
                                 List<List<Double>> pageMatrix) {
    int maxLen = Math.max(pagesA.size(), pagesB.size())
    if (maxLen == 0) return 0.0
    double total = 0.0
    (0..<Math.min(pagesA.size(), pagesB.size())).each { k -> total += pageMatrix[pagesA[k]][pagesB[k]] }
    return total / maxLen
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
    int proximityGroup = 0
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
    int proximityGroup
}

class Position {
    double x
    double y
    double w
    double h
}

class ProximityGroupEntry {
    List<Integer> areaIndices
    Position position
}

class ProcessedAreasAndGroups {
    List<AreaEntry> areas
    List<ProximityGroupEntry> proximityGroups
}

class PageEntry {
    String pageId
    String pageName
    String templateId
    String templateName
    Integer templatePageIndex
    List<AreaEntry> areas
    List<ProximityGroupEntry> proximityGroups
}

class TemplateGroupEntry {
    int listIdx
    int order
}

class TemplateEntry {
    String templateId
    String templateName
    List<Integer> pageIndices
}

class AreaMatch {
    double distance
    int indexA
    int indexB
}
