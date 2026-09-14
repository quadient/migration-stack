package com.quadient.migration.example.docx.parser

import groovy.transform.Field
import org.apache.poi.xwpf.usermodel.XWPFParagraph
import org.apache.poi.xwpf.usermodel.XWPFStyle
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTStyle

import java.util.regex.Matcher
import java.util.regex.Pattern

// Word's outline levels are 0-based and level 9 means "body text".
@Field
static final int BODY_TEXT_OUTLINE_LEVEL = 9
@Field
static final Pattern HEADING_STYLE_NAME = ~/(?i)(heading|hdr|title)\s*(\d)?/
// Styles whose name says they are body text are never headings, even when they are (carelessly) based on a heading.
@Field
static final Pattern BODY_STYLE_NAME = ~/(?i)body|normal|text|list|table|toc|caption|footer|header/

/**
 * Returns the 1-based heading level of a paragraph, or null when it is ordinary text. The paragraph's style chain is
 * walked from the most specific style upwards: a heading-like style name decides first (e.g. "SLR HDR2" or "1. SLR
 * HDR1", which have no outline level of their own), then a style's outline level. Outline levels set directly on a
 * paragraph are ignored, as they typically stem from copied formatting rather than from document structure.
 */
static Integer headingLevel(XWPFParagraph paragraph) {
    if (paragraph.text.isBlank()) {
        return null
    }
    Set<String> visited = []
    String styleId = paragraph.styleID
    while (styleId && visited.add(styleId)) {
        XWPFStyle style = paragraph.document.styles?.getStyle(styleId)
        if (style == null) {
            return null
        }
        Integer level = levelOf(style)
        if (level != null) {
            return level > 0 ? level : null
        }
        CTStyle ctStyle = style.CTStyle
        styleId = ctStyle.isSetBasedOn() ? ctStyle.basedOn.val : null
    }
    return null
}

// Heading level of one style; 0 means "definitely body text", null means "undecided, ask the parent style".
private static Integer levelOf(XWPFStyle style) {
    String name = style.name ?: style.styleId
    if (name =~ /(?i)subtitle/) {
        return 2
    }
    Integer outlineLevel = style.CTStyle.PPr?.isSetOutlineLvl() ? style.CTStyle.PPr.outlineLvl.val.intValue() : null
    if (outlineLevel != null && outlineLevel >= BODY_TEXT_OUTLINE_LEVEL) {
        return 0
    }
    Matcher heading = HEADING_STYLE_NAME.matcher(name)
    if (heading.find()) {
        return heading.group(2) ? heading.group(2).toInteger() : (outlineLevel != null ? outlineLevel + 1 : 1)
    }
    if (BODY_STYLE_NAME.matcher(name).find()) {
        return 0
    }
    return outlineLevel != null ? outlineLevel + 1 : null
}
