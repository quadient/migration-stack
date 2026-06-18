//! ---
//! displayName: Import Paragraph Styles
//! category: Mapping
//! description: Imports paragraph style definitions from CSV files into the migration project, applying any updates made to the columns during editing.
//! target: gradle
//! ---
package com.quadient.migration.example.common.mapping

import com.quadient.migration.api.Migration
import com.quadient.migration.api.dto.migrationmodel.MappingItem
import com.quadient.migration.api.dto.migrationmodel.builder.ParagraphStyleBuilder
import com.quadient.migration.api.dto.migrationmodel.builder.ParagraphStyleDefinitionBuilder
import com.quadient.migration.example.common.util.Csv
import com.quadient.migration.example.common.util.Mapping
import com.quadient.migration.shared.Alignment
import com.quadient.migration.shared.LineSpacing
import com.quadient.migration.shared.ParagraphPdfTaggingRule
import com.quadient.migration.shared.Size

import java.nio.file.Path

import groovy.transform.Field
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import static com.quadient.migration.example.common.util.InitMigration.initMigration

@Field static Logger log = LoggerFactory.getLogger(this.class.name)

def migration = initMigration(this.binding)

def exportFilePath = Mapping.csvPath(binding, migration.projectConfig.name, "paragraph-styles")

run(migration, exportFilePath)

static void run(Migration migration, Path path) {
    def file = path.toFile().readLines()
    def columnNames = Csv.parseColumnNames(file.removeFirst()).collect { Mapping.normalizeHeader(it) }
    def total = file.size()

    def mappings = new HashMap<String, MappingItem>()
    for (line in file) {
        def values = Csv.getCells(line, columnNames)
        def id = values.get("id")

        def existingStyle = migration.paragraphStyleRepository.find(id)
        if (existingStyle == null) {
            migration.paragraphStyleRepository.upsert(
                new ParagraphStyleBuilder(id)
                    .definition(new ParagraphStyleDefinitionBuilder().build())
                    .build()
            )
        }

        mappings[id] = toMapping(values)
        if (total > 1000 && mappings.size() % 1000 == 0) {
            log.info "Processed ${mappings.size()}/${total} mappings"
        }
    }

    def batches = mappings.entrySet().collate(1000)
    for (int i = 0; i < batches.size(); i++) {
        log.info "Upserting mappings batch ${i + 1}/${batches.size()} (${batches[i].size()} items)"
        migration.mappingRepository.upsertBatch(batches[i].collectEntries())
    }
    migration.mappingRepository.applyAllParagraphStyleMappings()
}

private static MappingItem.ParagraphStyle toMapping(Map<String, String> values) {
    def name = Csv.deserialize(values.get("name"), String.class)
    def targetId = Csv.deserialize(values.get("targetId"), String.class)

    return new MappingItem.ParagraphStyle(name, targetId, new MappingItem.ParagraphStyle.Def(
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

