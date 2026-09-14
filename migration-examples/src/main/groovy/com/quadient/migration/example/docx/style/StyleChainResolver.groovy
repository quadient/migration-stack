package com.quadient.migration.example.docx.style

import com.quadient.migration.shared.Color
import org.apache.poi.xwpf.usermodel.XWPFParagraph
import org.apache.poi.xwpf.usermodel.XWPFRun
import org.apache.poi.xwpf.usermodel.XWPFStyles
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPPrBase
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTRPr
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTStyle

static List<CTRPr> resolveRunPropertyChain(XWPFRun run, String fallbackStyleId) {
    CTRPr directRPr = run.CTR.isSetRPr() ? run.CTR.RPr : null
    XWPFStyles docStyles = run.document.styles
    String styleId = directRPr?.RStyleList ? directRPr.RStyleList[0].val : (run.style ?: fallbackStyleId)
    return buildPropertyChain(directRPr, docStyles, styleId, docStyles.defaultRunStyle?.RPr) { it.RPr }
}

static List<CTPPrBase> resolveParagraphPropertyChain(XWPFParagraph paragraph) {
    CTPPrBase directPPr = paragraph.CTP.isSetPPr() ? paragraph.CTP.PPr : null
    XWPFStyles docStyles = paragraph.document.styles
    return buildPropertyChain(directPPr, docStyles, paragraph.styleID, docStyles.defaultParagraphStyle?.PPr) { it.PPr }
}

static <T> T resolveFirst(List chain, Closure<T> extractor) {
    return chain.findResult { extractor(it) }
}

private static <T> List<T> buildPropertyChain(T directProperties, XWPFStyles docStyles, String styleId, T docDefaults,
                                              Closure<T> propertyExtractor) {
    List<T> chain = []
    if (directProperties != null) {
        chain << directProperties
    }
    Set<String> visitedStyleIds = []
    String currentStyleId = styleId
    while (currentStyleId && visitedStyleIds.add(currentStyleId)) {
        CTStyle ctStyle = docStyles.getStyle(currentStyleId)?.CTStyle
        T properties = ctStyle != null ? propertyExtractor(ctStyle) : null
        if (properties != null) {
            chain << properties
        }
        currentStyleId = ctStyle?.isSetBasedOn() ? ctStyle.basedOn.val : null
    }
    if (docDefaults != null) {
        chain << docDefaults
    }
    return chain
}

static String resolveFontName(CTRPr rPr) {
    if (rPr?.getRFontsList()) {
        def font = rPr.getRFontsList().get(0)
        return font.getAscii() ?: font.getHAnsi() ?: font.getEastAsia()
    }
    return null
}

static Double resolveFontSize(CTRPr rPr) {
    if (rPr?.getSzList()) {
        def val = rPr.getSzList().get(0).getVal()
        // XML 'sz' is in half-points
        return val != null ? val.longValue() / 2.0 : null
    }
    return null
}

static Boolean resolveBold(CTRPr rPr) {
    return resolveOnOffFlag(rPr?.getBList())
}

static Boolean resolveItalic(CTRPr rPr) {
    return resolveOnOffFlag(rPr?.getIList())
}

static String resolveColor(CTRPr rPr) {
    if (rPr?.getColorList()) {
        return colorValueToHex(rPr.getColorList().get(0).getVal())
    }
    return null
}

static String colorValueToHex(Object colorValue) {
    if (colorValue instanceof byte[]) {
        return colorValue.collect { String.format("%02X", it & 0xFF) }.join()
    }
    return colorValue?.toString()
}

static Color toColor(Object colorValue) {
    String hex = colorValueToHex(colorValue)
    if (!hex || !hex.matches("(?i)[0-9A-F]{6}")) {
        return null
    }
    return Color.fromHex('#' + hex)
}

static String resolveAlignment(CTPPrBase pPr) {
    return pPr?.getJc()?.getVal()?.toString()
}

static Integer resolveIndentationLeft(CTPPrBase pPr) {
    return (pPr?.getInd()?.isSetLeft()) ? pPr.getInd().getLeft() as int : null
}

static Integer resolveIndentationRight(CTPPrBase pPr) {
    return (pPr?.getInd()?.isSetRight()) ? pPr.getInd().getRight() as int : null
}

static Integer resolveIndentationFirstLine(CTPPrBase pPr) {
    return (pPr?.getInd()?.isSetFirstLine()) ? pPr.getInd().getFirstLine() as int : null
}

static Integer resolveSpacingBefore(CTPPrBase pPr) {
    return (pPr?.getSpacing()?.isSetBefore()) ? pPr.getSpacing().getBefore() as int : null
}

static Integer resolveSpacingAfter(CTPPrBase pPr) {
    return (pPr?.getSpacing()?.isSetAfter()) ? pPr.getSpacing().getAfter() as int : null
}

static Map resolveLineSpacing(CTPPrBase pPr) {
    if (!pPr?.getSpacing()?.isSetLine()) {
        return null
    }
    String lineRule = pPr.getSpacing().isSetLineRule() ? pPr.getSpacing().getLineRule().toString() : "auto"
    return [line: pPr.getSpacing().getLine() as long, lineRule: lineRule]
}

private static Boolean resolveOnOffFlag(List onOffList) {
    if (!onOffList) return null
    def val = onOffList.get(0).getVal()
    return val == null || !(val.toString() in ["false", "0", "off"])
}
