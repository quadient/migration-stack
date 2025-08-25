//! ---
//! category: Mapping
//! description: Import paragraph styles
//! target: gradle
//! ---
package com.quadient.migration.example.common.mapping

import com.quadient.migration.api.Migration
import com.quadient.migration.api.dto.migrationmodel.MappingItem
import com.quadient.migration.api.dto.migrationmodel.ParagraphStyle
import com.quadient.migration.api.dto.migrationmodel.ParagraphStyleDefinition
import com.quadient.migration.api.dto.migrationmodel.ParagraphStyleRef
import com.quadient.migration.api.dto.migrationmodel.builder.ParagraphStyleBuilder
import com.quadient.migration.api.dto.migrationmodel.builder.ParagraphStyleDefinitionBuilder
import com.quadient.migration.example.common.util.Csv
import com.quadient.migration.example.common.util.Mapping
import com.quadient.migration.shared.LineSpacing
import com.quadient.migration.shared.Size

import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.nio.file.Path
import java.nio.file.Paths

import static com.quadient.migration.example.common.util.InitMigration.initMigration

def migration = initMigration(this.binding)

def file = Paths.get("mapping", "${migration.projectConfig.name}-paragraph-styles.csv")

run(migration, file)

static void run(Migration migration, Path path) {
    def file = path.toFile().readLines()
    def columnNames = file.removeFirst().split(",")

    def definitionFields = ParagraphStyleDefinition.declaredFields.findAll { !it.synthetic && !Modifier.isStatic(it.getModifiers()) }
    for (line in file) {
        def values = Csv.getCells(line, columnNames)

        def existingStyle = migration.paragraphStyleRepository.find(values.get("id"))
        def styleRefId = values.get("targetId")

        if (existingStyle == null) {
            createNewStyle(migration, styleRefId, values, definitionFields)
        } else {
            mapStyle(migration, existingStyle, styleRefId, values, definitionFields)
        }
    }
}

static void mapStyle(Migration migration, ParagraphStyle existingStyle, String styleRefId, Map<String, String> values, List<Field> definitionFields) {
    def mapping = migration.mappingRepository.getParagraphStyleMapping(existingStyle.id)
    def name = Csv.deserialize(values.get("name"), String.class)
    Mapping.mapProp(mapping, existingStyle, "name", name)
    def existingDefinition = existingStyle.definition

    if (styleRefId.empty) {
        MappingItem.ParagraphStyle.Def mappingDefinition
        if (!(mapping.definition instanceof MappingItem.ParagraphStyle.Def)) {
            mappingDefinition = new MappingItem.ParagraphStyle.Def(null, null, null, null, null, null, null, null, null, null)
        } else {
            mappingDefinition = mapping.definition as MappingItem.ParagraphStyle.Def
        }

        if (existingDefinition instanceof ParagraphStyleDefinition) {
            for (field in definitionFields) {
                mapDefinitionField(existingDefinition, mappingDefinition, field, values)
            }
        } else {
            for (field in definitionFields) {
                updateDefinitionField(mappingDefinition, field, values)
            }
        }

        if (mappingDefinition != mapping.definition) {
            mapping.definition = mappingDefinition
        }
    } else {
        if (existingDefinition instanceof ParagraphStyleDefinition || (existingDefinition instanceof ParagraphStyleRef && existingDefinition.id != styleRefId)) {
            mapping.definition = new MappingItem.ParagraphStyle.Ref(styleRefId)
        }
    }

    migration.mappingRepository.upsert(existingStyle.id, mapping)
    migration.mappingRepository.applyParagraphStyleMapping(existingStyle.id)
}

static void createNewStyle(Migration migration, String styleRefId, Map<String, String> values, List<Field> definitionFields) {
    def style
    if (styleRefId.empty) {
        def definition = new ParagraphStyleDefinitionBuilder().build()
        for (field in definitionFields) {
            updateDefinitionField(definition, field, values)
        }
        style = new ParagraphStyleBuilder(Csv.deserialize(values.get("id"), String.class))
            .name(Csv.deserialize(values.get("name"), String.class))
            .definition(definition)
            .build()
    } else {
        style = new ParagraphStyleBuilder(Csv.deserialize(values.get("id"), String.class))
            .name(Csv.deserialize(values.get("name"), String.class))
            .styleRef(styleRefId)
            .build()
    }
    migration.paragraphStyleRepository.upsert(style)
}

private static void updateDefinitionField(Object paraStyleDefinition, Field definitionField, Map<String, String> values) {
    if (definitionField.name == "lineSpacing") {
        def lineSpacingType = values.get("lineSpacingType")
        def lineSpacingValue = values.get("lineSpacingValue")
        def lineSpacing = createLineSpacingInstance(lineSpacingType, lineSpacingValue)

        paraStyleDefinition.lineSpacing = lineSpacing
    } else {
        paraStyleDefinition."${definitionField.name}" = Csv.deserialize(values.get(definitionField.name), definitionField.type)
    }
}

private static void mapDefinitionField(ParagraphStyleDefinition paraStyleDefinition, MappingItem.ParagraphStyle.Def mapping, Field definitionField, Map<String, String> values) {
    if (definitionField.name == "lineSpacing") {
        def lineSpacingType = values.get("lineSpacingType")
        def lineSpacingValue = values.get("lineSpacingValue")
        def lineSpacing = createLineSpacingInstance(lineSpacingType, lineSpacingValue)

        Mapping.mapProp(mapping, paraStyleDefinition, "lineSpacing", lineSpacing)
    } else {
        Mapping.mapProp(mapping, paraStyleDefinition, definitionField.name, Csv.deserialize(values.get(definitionField.name), definitionField.type))
    }
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