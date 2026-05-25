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
import com.quadient.migration.api.dto.migrationmodel.Image
import com.quadient.migration.api.dto.migrationmodel.ImageRef
import com.quadient.migration.api.dto.migrationmodel.builder.DocumentObjectFilterBuilder
import com.quadient.migration.example.common.util.PathUtil
import com.quadient.migration.shared.DocumentObjectType
import groovy.transform.Field

import static com.quadient.migration.example.common.util.InitMigration.initMigration

@Field static final double MAX_X_MM = 315.0           // 1.5 × A4 width — off-page area guard
@Field static final double PAGE_REF_MM = 210.0        // A4 reference width for normalisation
@Field static final double PROXIMITY_GAP_MM = 5.3     // vertical gap threshold for proximity grouping
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
        (area as Area).content.each { c ->
            if (c instanceof DocumentObjectRef) allDocObjRefIds << c.id
            else if (c instanceof ImageRef) allImageRefIds << c.id
        }
    }
}

Map<String, DocumentObject> docObjCache = allDocObjRefIds
        ? migration.documentObjectRepository.list(new DocumentObjectFilterBuilder().ids(allDocObjRefIds.toList()).build())
        .collectEntries { [(it.id): it] } as Map<String, DocumentObject>
        : [:] as Map<String, DocumentObject>
Map<String, Image> imageCache = allImageRefIds
        ? migration.imageRepository.listAll().findAll { allImageRefIds.contains(it.id) }
        .collectEntries { [(it.id): it] } as Map<String, Image>
        : [:] as Map<String, Image>

templates.each { DocumentObject tmpl ->
    tmpl.content
            .findAll { it instanceof DocumentObjectRef && pageById.containsKey(it.id) }
            .eachWithIndex { ref, int idx ->
                DocumentObject page = pageById[(ref as DocumentObjectRef).id]
                assignedPageIds << page.id
                ProcessedAreasAndGroups areasAndGroups = processAreasAndGroups(docObjCache, imageCache, page.content.findAll { it instanceof Area } as List<Area>)
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
        ProcessedAreasAndGroups processed = processAreasAndGroups(docObjCache, imageCache, directAreas)
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
    ProcessedAreasAndGroups processed = processAreasAndGroups(docObjCache, imageCache, page.content.findAll { it instanceof Area } as List<Area>)
    pageEntries << new PageEntry(pageId: page.id,
            pageName: page.name,
            templateId: null,
            templateName: null,
            templatePageIndex: null,
            areas: processed.areas,
            proximityGroups: processed.proximityGroups)
}

List<List<Double>> pageMatrix = buildSymmetricMatrix(pageEntries.size()) { int i, int j ->
    pageSimilarity(pageEntries[i].areas, pageEntries[j].areas)
}

// Group page-list indices by their parent template
Map<String, List<Integer>> templateGroupMap = [:]
pageEntries.eachWithIndex { PageEntry pg, int i ->
    String key = pg.templateId ?: "orphan:${pg.pageId}"
    templateGroupMap.computeIfAbsent(key) { [] } << i
}

List<TemplateEntry> templateList = templateGroupMap.collect { String key, List<Integer> indices ->
    List<Integer> pageIndices = indices.sort { pageEntries[it].templatePageIndex ?: 0 }
    PageEntry first = pageEntries[pageIndices[0]]
    new TemplateEntry(templateId: first.templateId ?: key,
            templateName: first.templateName ?: first.templateId ?: key,
            pageIndices: pageIndices)
} as List<TemplateEntry>

List<List<Double>> templateMatrix = buildSymmetricMatrix(templateList.size()) { int i, int j ->
    templateSimilarity(templateList[i].pageIndices, templateList[j].pageIndices, pageMatrix)
}

new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(dstFile,
        [projectName: migration.projectConfig.name,
         pages      : pageEntries,
         similarity : [pageLevel    : [matrix: pageMatrix],
                       templateLevel: [templates: templateList, matrix: templateMatrix]]])
println "Written to: ${dstFile.absolutePath}"

// --- Helper methods ---

/** Builds a symmetric similarity matrix, computing only the upper triangle. */
static List<List<Double>> buildSymmetricMatrix(int n, Closure<Double> scoreFn) {
    List<List<Double>> m = (0..<n).collect { int i ->
        (0..<n).collect { int j ->
            if (i == j) return 1.0d
            if (j < i) return 0.0d
            round3dp(scoreFn(i, j))
        }
    } as List<List<Double>>
    (0..<n).each { i -> (0..<i).each { j -> m[i][j] = m[j][i] } }
    m
}

/** Processes raw areas into the structured area + proximity-group format used by the JSON output. */
static ProcessedAreasAndGroups processAreasAndGroups(Map<String, DocumentObject> docObjCache, Map<String, Image> imageCache, List<Area> modelAreas) {
    List<WorkingArea> workingAreas = modelAreas
            .findAll { area ->
                area.position != null &&
                area.position.width.toMillimeters() > 0 &&
                area.position.height.toMillimeters() > 0 &&
                area.position.x.toMillimeters() < MAX_X_MM
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
                containedIn: containment[i],
                proximityGroup: a.proximityGroup ?: 0)
    } as List<AreaEntry>

    new ProcessedAreasAndGroups(areas: areas, proximityGroups: proximityGroups)
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
            if (outer.x - CONTAINMENT_TOL_MM <= inner.x && outer.y - CONTAINMENT_TOL_MM <= inner.y &&
                    inner.x2 <= outer.x2 + CONTAINMENT_TOL_MM && inner.y2 <= outer.y2 + CONTAINMENT_TOL_MM) {
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
 * the proximity gap of each other are placed in the same group.
 * Returns a list of groups, each group being a list of list-indices. */
static List<List<Integer>> groupByProximity(List<WorkingArea> workingAreas, double groupingGap = PROXIMITY_GAP_MM) {
    if (!workingAreas) return []
    List<Integer> sortedIndices = (0..<workingAreas.size()).toList().sort { int i, int j ->
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
 * Each area is described by (x, width) normalised to page reference width and
 * height with a log-ratio so a 3× difference scores ~1.0. */
static double pageSimilarity(List<AreaEntry> areasA, List<AreaEntry> areasB,
                             double matchThreshold = 0.5) {
    if (!areasA || !areasB) return 0.0

    List<AreaMatch> areaMatches = []
    areasA.eachWithIndex { AreaEntry a, int i ->
        areasB.eachWithIndex { AreaEntry b, int j ->
            double xDist = Math.abs(a.x - b.x) / PAGE_REF_MM
            double wDist = Math.abs(a.w - b.w) / PAGE_REF_MM
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
    int slots = Math.max(areasA.size(), areasB.size())
    return slots > 0 ? total / slots : 0.0
}

static double round2dp(double v) { Math.round(v * 100) / 100.0 }
static double round3dp(double v) { Math.round(v * 1000) / 1000.0 }

/**
 * Compare two templates by positional page matching (page 1↔1, page 2↔2, …).
 * Unmatched pages (different template lengths) contribute 0, penalising the score
 * proportionally so longer templates aren't artificially favoured. */
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
