package com.quadient.migration.example.docx.style

import com.quadient.migration.api.Migration
import com.quadient.migration.api.dto.migrationmodel.builder.TextStyleBuilder
import com.quadient.migration.api.dto.migrationmodel.builder.TextStyleDefinitionBuilder
import com.quadient.migration.shared.Size
import org.apache.poi.xwpf.usermodel.XWPFRun

import static com.quadient.migration.example.docx.style.StyleChainResolver.resolveFirst
import static com.quadient.migration.example.docx.style.StyleChainResolver.resolveRunPropertyChain
import static com.quadient.migration.example.docx.util.DocxUtils.calculateSHA

static String captureTextStyle(Migration migration, XWPFRun run, String fileName, String fallbackStyleId, String context) {
    String styleId = run.style ?: run.CTR.RPr?.RStyleList?.find()?.val ?: fallbackStyleId
    // Effective formatting is resolved by walking the full chain: direct run properties,
    // then the character/paragraph style and all of its basedOn ancestors, then docDefaults.
    List rPrChain = resolveRunPropertyChain(run, fallbackStyleId)

    // Attribute order and fallbacks feed the style id hash; changing them invalidates persisted mappings.
    LinkedHashMap styleAttributes = [
            name    : styleId,
            fontName: resolveFirst(rPrChain, StyleChainResolver.&resolveFontName),
            fontSize: (resolveFirst(rPrChain, StyleChainResolver.&resolveFontSize) ?: -1) as double,
            bold    : resolveFirst(rPrChain, StyleChainResolver.&resolveBold) ?: false,
            italic  : resolveFirst(rPrChain, StyleChainResolver.&resolveItalic) ?: false,
            color   : resolveFirst(rPrChain, StyleChainResolver.&resolveColor),
    ]

    String styleName = styleId
    if (context) {
        styleAttributes.context = context
        styleName = context + '_' + styleName
    }

    String sha = calculateSHA(styleAttributes)
    if (migration.textStyleRepository.find(sha) == null) {
        double fontSize = styleAttributes.fontSize as double
        TextStyleDefinitionBuilder definition = new TextStyleDefinitionBuilder()
                .fontFamily(styleAttributes.fontName?.toString())
                .size(Size.ofPoints(fontSize == -1 ? 10 : fontSize))
                .bold(styleAttributes.bold as boolean)
                .italic(styleAttributes.italic as boolean)
        String color = styleAttributes.color?.toString()
        if (color && color.matches("(?i)[0-9A-F]{6}")) {
            definition.foregroundColor('#' + color)
        }

        TextStyleBuilder textStyleBuilder = new TextStyleBuilder(sha)
                .name(styleName + '_' + sha.substring(0, 3) + sha.takeRight(3))
                .originLocations([fileName])
                .definition(definition.build())
        styleAttributes.each { textStyleBuilder.addCustomField(it.key.toString(), it.value.toString()) }
        migration.textStyleRepository.upsert(textStyleBuilder.build())
    }
    return sha
}
