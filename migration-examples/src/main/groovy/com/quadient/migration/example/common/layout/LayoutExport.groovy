//! ---
//! displayName: Layout Export
//! category: Layout
//! description: Export layout data — areas, containment, proximity groups, cross-page similarity and base-template candidates — as JSON for layout/index.html.
//! ---
package com.quadient.migration.example.common.layout

import com.fasterxml.jackson.databind.ObjectMapper
import com.quadient.migration.api.Migration
import com.quadient.migration.api.dto.migrationmodel.Area
import com.quadient.migration.api.dto.migrationmodel.DocumentObjectRef
import com.quadient.migration.api.dto.migrationmodel.ImageRef
import com.quadient.migration.api.dto.migrationmodel.builder.DocumentObjectFilterBuilder
import com.quadient.migration.api.repository.DocumentObjectRepository
import com.quadient.migration.example.common.util.PathUtil
import com.quadient.migration.shared.DocumentObjectType
import groovy.transform.Field

import static com.quadient.migration.example.common.util.InitMigration.initMigration

@Field Migration migration = initMigration(this.binding)

def dstFile = PathUtil.dataDirPath(binding, "layout", "${migration.projectConfig.name}-layout.json").toFile()
dstFile.parentFile.mkdirs()

def templatesAndPages = (migration.documentObjectRepository as DocumentObjectRepository)
        .list(new DocumentObjectFilterBuilder()
                .types([DocumentObjectType.Page, DocumentObjectType.Template])
                .build())

def templates = templatesAndPages.findAll { it.type == DocumentObjectType.Template }
def pages     = templatesAndPages.findAll { it.type == DocumentObjectType.Page }
def pageIds   = pages.collect { it.id } as Set<String>

// Page → template lookup; also track each page's position within its template
def pageToTemplate       = [:]
def pagePositionInTemplate = [:]
templates.each { tmpl ->
    tmpl.content
        .findAll { it instanceof DocumentObjectRef && pageIds.contains(it.id) }
        .eachWithIndex { ref, idx ->
            pageToTemplate[(ref as DocumentObjectRef).id]         = tmpl
            pagePositionInTemplate[(ref as DocumentObjectRef).id] = idx
        }
}

def pageDataList = pages.collect { page ->
    def template = pageToTemplate[page.id]

    def areaList = (page.content.findAll { it instanceof Area } as List<Area>)
            .findAll { area ->
                area.position != null &&
                area.position.width.toMillimeters()  > 0 &&
                area.position.height.toMillimeters() > 0 &&
                area.position.x.toMillimeters() < 315.0   // 1.5 × A4 width guard
            }
            .collect { area ->
                double x = area.position.x.toMillimeters()
                double y = area.position.y.toMillimeters()
                double w = area.position.width.toMillimeters()
                double h = area.position.height.toMillimeters()
                [x: x, y: y, w: w, h: h, x2: x + w, y2: y + h, sz: w * h,
                 flowToNextPage     : area.flowToNextPage,
                 interactiveFlowName: area.interactiveFlowName ?: "",
                 contentPreview     : buildContentPreview(migration, area)]
            }

    def containment = findContainment(areaList)

    // Proximity grouping: vertical sweep with 5.3 mm gap (~15 pt)
    def groups = groupByProximity(areaList)
    groups.eachWithIndex { groupIndices, gi ->
        groupIndices.each { idx -> areaList[idx].proximityGroup = gi }
    }

    def proximityGroups = groups.collect { groupIndices ->
        def subset = groupIndices.collect { areaList[it] }
        double bx  = subset.min { it.x  as double }.x  as double
        double by  = subset.min { it.y  as double }.y  as double
        double bx2 = subset.max { it.x2 as double }.x2 as double
        double by2 = subset.max { it.y2 as double }.y2 as double
        [areaIndices: groupIndices, bbox: [x: round2(bx), y: round2(by), w: round2(bx2 - bx), h: round2(by2 - by)]]
    }

    def areas = areaList.withIndex().collect { a, i ->
        [x: round2(a.x), y: round2(a.y), w: round2(a.w), h: round2(a.h),
         flowToNextPage     : a.flowToNextPage,
         interactiveFlowName: a.interactiveFlowName,
         contentPreview     : a.contentPreview,
         containedIn        : containment[i],   // list index of parent area, or null if top-level
         proximityGroup     : a.proximityGroup ?: 0]
    }

    [pageId            : page.id,
     pageName          : page.name,
     templateId        : template?.id,
     templateName      : template?.name,
     templatePageIndex : pagePositionInTemplate[page.id],
     areas             : areas,
     proximityGroups   : proximityGroups]
}

int pageCount = pageDataList.size()

def pageMatrix = (0..<pageCount).collect { i ->
    (0..<pageCount).collect { j ->
        if (i == j) return 1.0d
        if (j < i)  return 0.0d
        round3(pageSimilarity(pageDataList[i].areas as List<Map>, pageDataList[j].areas as List<Map>))
    }
}
(0..<pageCount).each { i -> (0..<i).each { j -> pageMatrix[i][j] = pageMatrix[j][i] } }

// Group page-list indices by their parent template, preserving intra-template page order
def templateGroupMap = [:]
pageDataList.eachWithIndex { pg, i ->
    def key = pg.templateId ?: "solo::${pg.pageId}"
    if (!templateGroupMap.containsKey(key)) templateGroupMap[key] = []
    templateGroupMap[key] << [listIdx: i as int, order: (pg.templatePageIndex ?: 0) as int]
}

def templateList = templateGroupMap.collect { key, entries ->
    def pageIndices = entries.sort { it.order }.collect { it.listIdx as int }
    def firstPage   = pageDataList[pageIndices[0]]
    [templateId  : firstPage.templateId ?: key,
     templateName: firstPage.templateName ?: firstPage.pageName,
     pageIndices : pageIndices]
}

int templateCount = templateList.size()
def templateMatrix = (0..<templateCount).collect { i ->
    (0..<templateCount).collect { j ->
        if (i == j) return 1.0d
        if (j < i)  return 0.0d
        round3(templateSimilarity(
            templateList[i].pageIndices as List<Integer>,
            templateList[j].pageIndices as List<Integer>,
            pageMatrix))
    }
}
(0..<templateCount).each { i -> (0..<i).each { j -> templateMatrix[i][j] = templateMatrix[j][i] } }

new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(dstFile, [
        projectName: migration.projectConfig.name,
        pages      : pageDataList,
        similarity : [
            pageLevel    : [matrix: pageMatrix],
            templateLevel: [templates: templateList, matrix: templateMatrix]
        ]
])
println "✓ Written to: ${dstFile.absolutePath}"

/** Summarise area content as a human-readable string. */
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
 * Tolerance of 0.7 mm (~2 pt) absorbs rounding differences.
 */
static Map<Integer, Integer> findContainment(List<Map> areas) {
    def containment = [:]
    areas.eachWithIndex { inner, i ->
        areas.eachWithIndex { outer, j ->
            if (i == j || (outer.sz as double) <= (inner.sz as double)) return
            double tol = 0.7
            if ((outer.x as double) - tol <= (inner.x as double) &&
                (outer.y as double) - tol <= (inner.y as double) &&
                (inner.x2 as double) <= (outer.x2 as double) + tol &&
                (inner.y2 as double) <= (outer.y2 as double) + tol) {
                Integer cur = containment[i] as Integer
                if (cur == null || (areas[cur].sz as double) > (outer.sz as double)) {
                    containment[i] = j
                }
            }
        }
    }
    return containment
}

/**
 * Vertical sweep grouping: areas whose Y ranges overlap or are within
 * yGapMm of each other are placed in the same group.
 * Returns a list of groups, each group being a list of list-indices.
 */
static List<List<Integer>> groupByProximity(List<Map> areas, double yGapMm = 5.3) {
    if (!areas) return []
    def indexed = areas.withIndex().collect { area, i -> [area: area, idx: i] }
    indexed.sort { a, b ->
        (a.area.y as double) <=> (b.area.y as double) ?: (a.area.x as double) <=> (b.area.x as double)
    }
    def groups  = []
    def current = [indexed[0].idx as int]
    double curY2 = indexed[0].area.y2 as double
    indexed.drop(1).each { entry ->
        if ((entry.area.y as double) <= curY2 + yGapMm) {
            current << (entry.idx as int)
            curY2 = Math.max(curY2, entry.area.y2 as double)
        } else {
            groups << current
            current = [entry.idx as int]
            curY2 = entry.area.y2 as double
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
 * Unmatched areas (different counts) reduce the score proportionally.
 */
static double pageSimilarity(List<Map> areasA, List<Map> areasB,
                             double pageRefMm = 210.0, double matchThreshold = 0.5) {
    def validA = areasA.findAll { (it.w as double) > 0 && (it.h as double) > 0 && (it.x as double) < pageRefMm * 1.5 }
    def validB = areasB.findAll { (it.w as double) > 0 && (it.h as double) > 0 && (it.x as double) < pageRefMm * 1.5 }
    if (!validA || !validB) return 0.0

    def distances = []
    validA.eachWithIndex { a, i ->
        validB.eachWithIndex { b, j ->
            double xDist  = Math.abs((a.x as double) - (b.x as double)) / pageRefMm
            double wDist  = Math.abs((a.w as double) - (b.w as double)) / pageRefMm
            double hRatio = Math.abs(Math.log(Math.max(a.h as double, 0.1) / Math.max(b.h as double, 0.1))) / Math.log(3.0)
            distances << [d: Math.sqrt(xDist * xDist + wDist * wDist + hRatio * hRatio), i: i, j: j]
        }
    }
    distances.sort { it.d }

    def matchedA = [] as Set
    def matchedB = [] as Set
    double total = 0.0
    distances.each { pair ->
        if (!(pair.i in matchedA) && !(pair.j in matchedB)) {
            matchedA << pair.i; matchedB << pair.j
            total += Math.max(0.0, 1.0 - (pair.d as double) / matchThreshold)
        }
    }
    int slots = Math.max(validA.size(), validB.size())
    return slots > 0 ? total / slots : 0.0
}

static double round2(double v) { Math.round(v * 100) / 100.0 }
static double round3(double v) { Math.round(v * 1000) / 1000.0 }

/**
 * Compare two templates by positional page matching (page 1↔1, page 2↔2, …).
 * Unmatched pages (different template lengths) contribute 0, penalising the score
 * proportionally so longer templates aren't artificially favoured.
 */
static double templateSimilarity(List<Integer> pagesA, List<Integer> pagesB,
                                 List<List<Double>> pageMatrix) {
    int maxLen = Math.max(pagesA.size(), pagesB.size())
    if (maxLen == 0) return 0.0
    double total = 0.0
    (0..<Math.min(pagesA.size(), pagesB.size())).each { k -> total += pageMatrix[pagesA[k]][pagesB[k]] as double }
    return total / maxLen
}
