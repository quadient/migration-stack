//! ---
//! displayName: Import Paragraph Styles
//! category: Mapping
//! description: Imports paragraph style definitions from CSV files into the migration project, applying any updates made to the columns during editing.
//! target: gradle
//! ---
package com.quadient.migration.example.common.mapping

import com.quadient.migration.api.Migration
import com.quadient.migration.api.dto.migrationmodel.MappingItem
import com.quadient.migration.api.dto.migrationmodel.ParagraphStyle
import com.quadient.migration.api.dto.migrationmodel.builder.ParagraphStyleBuilder
import com.quadient.migration.api.dto.migrationmodel.builder.ParagraphStyleDefinitionBuilder
import com.quadient.migration.example.common.util.Csv
import com.quadient.migration.example.common.util.Mapping
import com.quadient.migration.shared.Alignment
import com.quadient.migration.shared.LineSpacing
import com.quadient.migration.shared.ParagraphPdfTaggingRule
import com.quadient.migration.shared.Size
import groovy.json.JsonOutput

import java.nio.file.Path

import static com.quadient.migration.example.common.util.InitMigration.initMigration

def migration = initMigration(this.binding)

def exportFilePath = Mapping.csvPath(binding, migration.projectConfig.name, "paragraph-styles")

run(migration, exportFilePath)

static void run(Migration migration, Path path) {
    def file = path.toFile().readLines()
    def columnNames = Csv.parseColumnNames(file.removeFirst()).collect { Mapping.normalizeHeader(it) }

    for (line in file) {
        def values = Csv.getCells(line, columnNames)
        def id = values.get("id")
        def styleRefId = normalizeTargetId(values.get("targetId"))
        def existingStyle = migration.paragraphStyleRepository.find(id)
        def shouldUpsertStyle = false
        if (existingStyle == null) {
            existingStyle = new ParagraphStyleBuilder(id)
                .definition(new ParagraphStyleDefinitionBuilder().build())
                .build()
            shouldUpsertStyle = true
        }

        if (styleRefId != null) {
            backupDefinitionFromCsv(existingStyle, values)
            shouldUpsertStyle = true
        }

        if (shouldUpsertStyle) {
            migration.paragraphStyleRepository.upsert(existingStyle)
        }

        def mapping = toMapping(values, styleRefId)
        migration.mappingRepository.upsert(id, mapping)
        migration.mappingRepository.applyParagraphStyleMapping(id)
    }
}

private static void backupDefinitionFromCsv(ParagraphStyle style, Map<String, String> values) {
    style.customFields["originalDefinition"] = JsonOutput.toJson([
        leftIndent: Csv.serialize(Csv.deserialize(values.get("leftIndent"), Size.class)),
        rightIndent: Csv.serialize(Csv.deserialize(values.get("rightIndent"), Size.class)),
        defaultTabSize: Csv.serialize(Csv.deserialize(values.get("defaultTabSize"), Size.class)),
        spaceBefore: Csv.serialize(Csv.deserialize(values.get("spaceBefore"), Size.class)),
        spaceAfter: Csv.serialize(Csv.deserialize(values.get("spaceAfter"), Size.class)),
        alignment: Csv.serialize(Csv.deserialize(values.get("alignment"), Alignment.class)),
        firstLineIndent: Csv.serialize(Csv.deserialize(values.get("firstLineIndent"), Size.class)),
        keepWithNextParagraph: Csv.serialize(Csv.deserialize(values.get("keepWithNextParagraph"), Boolean.class)),
        lineSpacingType: normalizeLineSpacingType(values.get("lineSpacingType")),
        lineSpacingValue: normalizeLineSpacingValue(values.get("lineSpacingValue")),
        pdfTaggingRule: Csv.serialize(Csv.deserialize(values.get("pdfTaggingRule"), ParagraphPdfTaggingRule.class)),
    ])
}

private static MappingItem.ParagraphStyle toMapping(Map<String, String> values, String styleRefId) {
    def name = Csv.deserialize(values.get("name"), String.class)
    if (styleRefId != null) {
        return new MappingItem.ParagraphStyle(name, styleRefId, null)
    }

    return new MappingItem.ParagraphStyle(name, null, new MappingItem.ParagraphStyle.Def(
        Csv.deserialize(values.get("leftIndent"), Size.class),
        Csv.deserialize(values.get("rightIndent"), Size.class),
        Csv.deserialize(values.get("defaultTabSize"), Size.class),
        Csv.deserialize(values.get("spaceBefore"), Size.class),
        Csv.deserialize(values.get("spaceAfter"), Size.class),
        Csv.deserialize(values.get("alignment"), Alignment.class),
        Csv.deserialize(values.get("firstLineIndent"), Size.class),
        createLineSpacingInstance(values.get("lineSpacingType"), values.get("lineSpacingValue")),
        Csv.deserialize(values.get("keepWithNextParagraph"), Boolean.class),
        null,
        Csv.deserialize(values.get("pdfTaggingRule"), ParagraphPdfTaggingRule.class),
    ))
}

static LineSpacing createLineSpacingInstance(String type, String value) {
    switch (type) {
        case "Additional" -> new LineSpacing.Additional(value ? Size.fromString(value) : null)
        case "Exact" -> new LineSpacing.Exact(value ? Size.fromString(value) : null)
        case "AtLeast" -> new LineSpacing.AtLeast(value ? Size.fromString(value) : null)
        case "MultipleOf" -> new LineSpacing.MultipleOf(value ? Double.parseDouble(value) : null)
        case "ExactFromPreviousWithAdjustLegacy" -> new LineSpacing.ExactFromPreviousWithAdjustLegacy(value ? Size.fromString(value) : null)
        case "ExactFromPreviousWithAdjust" -> new LineSpacing.ExactFromPreviousWithAdjust(value ? Size.fromString(value) : null)
        case "ExactFromPrevious" -> new LineSpacing.ExactFromPrevious(value ? Size.fromString(value) : null)
        default -> throw new IllegalArgumentException("Unknown line spacing type: $type")
    }
}

private static String normalizeLineSpacingType(String value) {
    if (value == null) return ""
    return value.trim()
}

private static String normalizeLineSpacingValue(String value) {
    if (value == null) return ""
    return value.trim()
}

private static String normalizeTargetId(String targetId) {
    if (targetId == null) return null
    def normalized = targetId.trim()
    return normalized.isEmpty() ? null : normalized
}
