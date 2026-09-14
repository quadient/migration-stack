package com.quadient.migration.example.docx.parser

import com.quadient.migration.shared.Position
import com.quadient.migration.shared.Size
import groovy.transform.Field
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPageMar
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSectPr

import static com.quadient.migration.example.docx.util.DocxUtils.twipsToPoints

@Field
static final List<Size> DEFAULT_PAGE_SIZE = [Size.ofMillimeters(210), Size.ofMillimeters(297)]
@Field
static final Map<String, Size> DEFAULT_MARGINS = [
        left: Size.ofMillimeters(20), right: Size.ofMillimeters(20),
        top: Size.ofMillimeters(20), bottom: Size.ofMillimeters(37),
]
// Preserve the existing 170 x 240 mm fallback content area on an A4 page.
@Field
static final Position DEFAULT_CONTENT_POSITION = new Position(
        DEFAULT_MARGINS.left, DEFAULT_MARGINS.top,
        Size.ofPoints(DEFAULT_PAGE_SIZE[0].toPoints() - DEFAULT_MARGINS.left.toPoints() - DEFAULT_MARGINS.right.toPoints()),
        Size.ofPoints(DEFAULT_PAGE_SIZE[1].toPoints() - DEFAULT_MARGINS.top.toPoints() - DEFAULT_MARGINS.bottom.toPoints()))

static Position resolvePageContentPosition(CTSectPr sectPr) {
    CTPageMar pageMar = sectPr?.pgMar
    def (pageWidth, pageHeight) = resolvePageSize(sectPr)*.toPoints()
    Size top = resolveDimension(pageMar?.top, DEFAULT_MARGINS.top)
    Size left = resolveDimension(pageMar?.left, DEFAULT_MARGINS.left)
    double marginTop = top.toPoints()
    double marginRight = resolveDimension(pageMar?.right, DEFAULT_MARGINS.right).toPoints()
    double marginBottom = resolveDimension(pageMar?.bottom, DEFAULT_MARGINS.bottom).toPoints()
    double marginLeft = left.toPoints()

    double contentWidth = pageWidth - marginLeft - marginRight
    double contentHeight = pageHeight - marginTop - marginBottom
    if (contentWidth <= 0 || contentHeight <= 0) {
        return DEFAULT_CONTENT_POSITION
    }
    return new Position(left, top, Size.ofPoints(contentWidth), Size.ofPoints(contentHeight))
}

static List<Size> resolvePageSize(CTSectPr sectPr) {
    return [resolveDimension(sectPr?.pgSz?.w, DEFAULT_PAGE_SIZE[0]),
            resolveDimension(sectPr?.pgSz?.h, DEFAULT_PAGE_SIZE[1])]
}

private static Size resolveDimension(Object twips, Size fallback) {
    Double points = twipsToPoints(twips)
    // Zero is a valid explicit margin, so do not use Groovy's truth-based fallback.
    return points != null ? Size.ofPoints(points) : fallback
}
