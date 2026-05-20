//! ---
//! displayName: Area Template Analysis
//! category: Layout
//! description: Analyse page areas, detect containment and proximity groups, score cross-page similarity and suggest base-template candidates. Output JSON for layout/index.html.
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

def dstFile = PathUtil.dataDirPath(binding, "layout", "${migration.projectConfig.name}-area-analysis.json").toFile()
dstFile.parentFile.mkdirs()

// ── Load pages & templates from the migration model ──────────────────────────
def templatesAndPages = (migration.documentObjectRepository as DocumentObjectRepository)
        .list(new DocumentObjectFilterBuilder()
                .types([DocumentObjectType.Page, DocumentObjectType.Template])
                .build())

def templates = templatesAndPages.findAll { it.type == DocumentObjectType.Template }
def pages     = templatesAndPages.findAll { it.type == DocumentObjectType.Page }
def pageIds   = pages.collect { it.id } as Set<String>

// Build page → template reverse index (a page can appear in at most one template)
def pageToTemplate = [:]
templates.each { tmpl ->
    tmpl.content.findAll { it instanceof DocumentObjectRef && pageIds.contains(it.id) }.each { ref ->
        pageToTemplate[(ref as DocumentObjectRef).id] = tmpl
    }
}

// ── Build per-page data ───────────────────────────────────────────────────────
def pageDataList = pages.collect { page ->
    def template = pageToTemplate[page.id]

    // Collect valid areas (skip nulls, off-page and zero-dimension artifacts)
    def areaDataList = (page.content.findAll { it instanceof Area } as List<Area>)
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
                 flowToNextPage    : area.flowToNextPage,
                 interactiveFlowName: area.interactiveFlowName ?: "",
                 contentPreview    : buildContentPreview(migration, area)]
            }

    // Containment: find the smallest enclosing parent for each area
    def containment = findContainment(areaDataList)

    // Proximity grouping: vertical sweep with 5.3 mm gap (~15 pt)
    def groups = groupByProximity(areaDataList)
    groups.eachWithIndex { grpIndices, gi ->
        grpIndices.each { idx -> areaDataList[idx].proximityGroup = gi }
    }

    def proximityGroups = groups.collect { grpIndices ->
        def subset = grpIndices.collect { areaDataList[it] }
        double bx  = subset.min { it.x  as double }.x  as double
        double by  = subset.min { it.y  as double }.y  as double
        double bx2 = subset.max { it.x2 as double }.x2 as double
        double by2 = subset.max { it.y2 as double }.y2 as double
        [areaIndices: grpIndices, bbox: [x: r2(bx), y: r2(by), w: r2(bx2 - bx), h: r2(by2 - by)]]
    }

    def areas = areaDataList.withIndex().collect { a, i ->
        [x: r2(a.x), y: r2(a.y), w: r2(a.w), h: r2(a.h),
         flowToNextPage    : a.flowToNextPage,
         interactiveFlowName: a.interactiveFlowName,
         contentPreview    : a.contentPreview,
         containedIn       : containment[i],          // list index of parent, or null
         proximityGroup    : a.proximityGroup ?: 0]
    }

    [pageId         : page.id,
     pageName       : page.name,
     templateId     : template?.id,
     templateName   : template?.name,
     areas          : areas,
     proximityGroups: proximityGroups]
}

// ── Similarity matrix & template-family clustering ───────────────────────────
int n = pageDataList.size()

def matrix = (0..<n).collect { i ->
    (0..<n).collect { j ->
        if (i == j) return 1.0d
        if (j < i)  return 0.0d   // filled symmetrically below
        rs(pageSimilarity(pageDataList[i].areas as List<Map>, pageDataList[j].areas as List<Map>))
    }
}
(0..<n).each { i -> (0..<i).each { j -> matrix[i][j] = matrix[j][i] } }

def families    = clusterPages((0..<n).toList(), matrix)
def familyData  = families.collect { idxList ->
    [pageIds  : idxList.collect { pageDataList[it].pageId },
     pageNames: idxList.collect { pageDataList[it].pageName }]
}

// ── Write JSON ────────────────────────────────────────────────────────────────
new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(dstFile, [
        projectName: migration.projectConfig.name,
        pages      : pageDataList,
        similarity : [matrix: matrix, families: familyData]
])
println "✓ Written to: ${dstFile.absolutePath}"

// ── Static helper functions ───────────────────────────────────────────────────

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
 * Returns a map of list-index → list-index (parent), or empty if top-level.
 * Tolerance: 0.7 mm (~2 pt) to absorb rounding differences.
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
 * height with a log-ratio so a 3× difference scores ~1.0 (i.e. large
 * height differences are tolerated but still weakly penalised).
 * Unmatched areas (different counts) reduce the score proportionally.
 */
static double pageSimilarity(List<Map> areasA, List<Map> areasB,
                             double pageRefMm = 210.0, double matchThreshold = 0.5) {
    def va = areasA.findAll { (it.w as double) > 0 && (it.h as double) > 0 && (it.x as double) < pageRefMm * 1.5 }
    def vb = areasB.findAll { (it.w as double) > 0 && (it.h as double) > 0 && (it.x as double) < pageRefMm * 1.5 }
    if (!va || !vb) return 0.0

    def dists = []
    va.eachWithIndex { a, i ->
        vb.eachWithIndex { b, j ->
            double xd = Math.abs((a.x as double) - (b.x as double)) / pageRefMm
            double wd = Math.abs((a.w as double) - (b.w as double)) / pageRefMm
            double hA = Math.max(a.h as double, 0.1)
            double hB = Math.max(b.h as double, 0.1)
            double hr = Math.abs(Math.log(hA / hB)) / Math.log(3.0)
            dists << [d: Math.sqrt(xd * xd + wd * wd + hr * hr), i: i, j: j]
        }
    }
    dists.sort { it.d }

    def matchedA = [] as Set
    def matchedB = [] as Set
    double total = 0.0
    dists.each { e ->
        if (!(e.i in matchedA) && !(e.j in matchedB)) {
            matchedA << e.i; matchedB << e.j
            total += Math.max(0.0, 1.0 - (e.d as double) / matchThreshold)
        }
    }
    int slots = Math.max(va.size(), vb.size())
    return slots > 0 ? total / slots : 0.0
}

/**
 * Greedy single-linkage clustering: merge two families when any pair of
 * their pages has similarity >= threshold.
 */
static List<List<Integer>> clusterPages(List<Integer> indices,
                                        List<List<Double>> matrix,
                                        double threshold = 0.60) {
    def families = indices.collect { [it] }
    boolean changed = true
    while (changed) {
        changed = false
        for (int i = 0; i < families.size() && !changed; i++) {
            for (int j = i + 1; j < families.size() && !changed; j++) {
                boolean merge = families[i].any { a -> families[j].any { b -> (matrix[a][b] as double) >= threshold } }
                if (merge) {
                    families[i].addAll(families[j])
                    families.remove(j)
                    changed = true
                }
            }
        }
    }
    return families
}

static double r2(double v)  { Math.round(v * 100) / 100.0 }
static double rs(double v)  { Math.round(v * 1000) / 1000.0 }
