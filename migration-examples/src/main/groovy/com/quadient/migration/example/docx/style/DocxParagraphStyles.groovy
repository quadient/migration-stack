package com.quadient.migration.example.docx.style

import com.quadient.migration.api.Migration
import com.quadient.migration.api.dto.migrationmodel.builder.ParagraphStyleBuilder
import com.quadient.migration.api.dto.migrationmodel.builder.ParagraphStyleDefinitionBuilder
import com.quadient.migration.shared.Alignment
import groovy.transform.Field
import org.apache.poi.xwpf.usermodel.XWPFParagraph

import static com.quadient.migration.example.docx.style.StyleChainResolver.resolveFirst
import static com.quadient.migration.example.docx.style.StyleChainResolver.resolveParagraphPropertyChain
import static com.quadient.migration.example.docx.util.DocxUtils.calculateSHA
import static com.quadient.migration.example.docx.util.DocxUtils.twipsToSize

static String captureParagraphStyle(Migration migration, XWPFParagraph paragraph, String fileName, String styleId, String context) {
    List pPrChain = resolveParagraphPropertyChain(paragraph)
    Map lineSpacing = resolveFirst(pPrChain, StyleChainResolver.&resolveLineSpacing)

    // Attribute order and fallbacks feed the style id hash; changing them invalidates persisted mappings.
    LinkedHashMap styleAttributes = [
            name                : styleId,
            alignment           : resolveFirst(pPrChain, StyleChainResolver.&resolveAlignment) ?: "LEFT",
            indentationLeft     : resolveFirst(pPrChain, StyleChainResolver.&resolveIndentationLeft) ?: -1,
            indentationRight    : resolveFirst(pPrChain, StyleChainResolver.&resolveIndentationRight) ?: -1,
            indentationFirstLine: resolveFirst(pPrChain, StyleChainResolver.&resolveIndentationFirstLine) ?: -1,
            spacingBefore       : resolveFirst(pPrChain, StyleChainResolver.&resolveSpacingBefore) ?: -1,
            spacingAfter        : resolveFirst(pPrChain, StyleChainResolver.&resolveSpacingAfter) ?: -1,
            lineSpacingLine     : lineSpacing?.line ?: -1,
            lineSpacingRule     : lineSpacing?.lineRule ?: "none",
    ]

    if (paragraph.numID != null) {
        String numFmt = paragraph.numFmt
                ?: paragraph.document.numbering?.getNum(paragraph.numID)?.CTNum?.abstractNumId?.val
        styleAttributes.listId = paragraph.numID.toString()
        styleAttributes.listType = numFmt
        styleAttributes.listLevel = paragraph.numIlvl.toString()
        styleAttributes.name = "list${numFmt ? numFmt.capitalize() : ''}_${styleAttributes.name}"
    }

    String styleName = styleAttributes.name.toString()
    if (context) {
        styleAttributes.context = context
        styleName = context + '_' + styleName
    }

    String sha = calculateSHA(styleAttributes)
    if (migration.paragraphStyleRepository.find(sha) == null) {
        ParagraphStyleDefinitionBuilder definition = new ParagraphStyleDefinitionBuilder()
                .alignment(getAlignment(styleAttributes.alignment.toString()))
                .leftIndent(twipsToSize(styleAttributes.indentationLeft as long))
                .rightIndent(twipsToSize(styleAttributes.indentationRight as long))
                .firstLineIndent(twipsToSize(styleAttributes.indentationFirstLine as long))
                .spaceBefore(twipsToSize(styleAttributes.spacingBefore as long))
                .spaceAfter(twipsToSize(styleAttributes.spacingAfter as long))
        applyLineSpacing(definition, styleAttributes.lineSpacingRule.toString(), styleAttributes.lineSpacingLine as long)

        ParagraphStyleBuilder paragraphStyleBuilder = new ParagraphStyleBuilder(sha)
                .name(styleName + '_' + sha.substring(0, 3) + sha.takeRight(3))
                .originLocations([fileName])
                .definition(definition.build())
        styleAttributes.each { paragraphStyleBuilder.addCustomField(it.key.toString(), it.value.toString()) }
        migration.paragraphStyleRepository.upsert(paragraphStyleBuilder.build())
    }
    return sha
}

static void applyLineSpacing(ParagraphStyleDefinitionBuilder builder, String lineRule, long line) {
    switch (lineRule) {
        case "auto":
            // Word stores "auto" line spacing in 240ths of a line.
            builder.multipleOfLineSpacing(line / 240.0)
            break
        case "atLeast":
            builder.atLeastLineSpacing(twipsToSize(line))
            break
        case "exact":
            builder.exactLineSpacing(twipsToSize(line))
            break
        default:
            builder.additionalLineSpacing(twipsToSize(-1))
    }
}

@Field
static final Map<String, Alignment> ALIGNMENTS = [
        left   : Alignment.Left,
        right  : Alignment.Right,
        center : Alignment.Center,
        justify: Alignment.JustifyLeft,
        both   : Alignment.JustifyLeft,
]

static Alignment getAlignment(String alignmentAttribute) {
    return ALIGNMENTS.getOrDefault(alignmentAttribute.toLowerCase(), Alignment.Left)
}