//! ---
//! displayName: Import Text Styles
//! category: Mapping
//! description: Imports text style definitions from CSV files into the migration project, applying any updates made to the columns during editing.
//! target: gradle
//! ---
package com.quadient.migration.example.common.mapping

import com.quadient.migration.api.Migration
import com.quadient.migration.api.dto.migrationmodel.MappingItem
import com.quadient.migration.api.dto.migrationmodel.TextStyle
import com.quadient.migration.api.dto.migrationmodel.TextStyleDefinition
import com.quadient.migration.api.dto.migrationmodel.TextStyleRef
import com.quadient.migration.api.dto.migrationmodel.builder.TextStyleBuilder
import com.quadient.migration.example.common.util.Csv
import com.quadient.migration.example.common.util.Mapping

import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.nio.file.Path

import static com.quadient.migration.example.common.util.InitMigration.initMigration

def migration = initMigration(this.binding)

def exportFilePath = Mapping.csvPath(binding, migration.projectConfig.name, "text-styles")

run(migration, exportFilePath )

static void run(Migration migration, Path path) {
    def file = path.toFile().readLines()
    def columnNames = Csv.parseColumnNames(file.removeFirst())
    def definitionFields = TextStyleDefinition.declaredFields.findAll { !it.synthetic && !Modifier.isStatic(it.getModifiers()) }

    for (line in file) {
        def values = Csv.getCells(line, columnNames)

        def existingStyle = migration.textStyleRepository.find(values.get("id"))
        def styleRefId = values.get("targetId")

        if (existingStyle == null) {
            createNewStyle(migration, styleRefId, values, definitionFields)
        } else {
            mapStyle(migration, existingStyle, styleRefId, values, definitionFields)
        }
    }
}

static void mapStyle(Migration migration, TextStyle existingStyle, String styleRefId, Map<String, String> values, List<Field> definitionFields) {
    def mapping = migration.mappingRepository.getTextStyleMapping(existingStyle.id)
    def name = Csv.deserialize(values.get("name"), String.class)
    Mapping.mapProp(mapping, existingStyle, "name", name)
    def existingDefinition = existingStyle.definition

    if (styleRefId.empty) {
        def mappingDefinition
        if (!(mapping.definition instanceof MappingItem.TextStyle.Def)) {
            mappingDefinition = new MappingItem.TextStyle.Def(null, null, null, null, null, null, null, null, null)
        } else {
            mappingDefinition = mapping.definition
        }

        if (existingDefinition instanceof TextStyleDefinition) {
            for (field in definitionFields) {
                Mapping.mapProp(mappingDefinition, existingDefinition, field.name, Csv.deserialize(values.get(field.name), field.type))
            }
        } else {
            for (field in definitionFields) {
                def value = Csv.deserialize(values.get(field.name), field.type)
                if (value != null && value != "") {
                    mappingDefinition[field.name] = value
                }
            }
        }

        if (mappingDefinition != mapping.definition) {
            mapping.definition = mappingDefinition
        }
    } else {
        if (existingDefinition instanceof TextStyleDefinition
            || (existingDefinition instanceof TextStyleRef && existingDefinition.id != styleRefId)) {
            mapping.definition = new MappingItem.TextStyle.Ref(styleRefId)
        }
    }

    migration.mappingRepository.upsert(existingStyle.id, mapping)
    migration.mappingRepository.applyTextStyleMapping(existingStyle.id)
}

static void createNewStyle(Migration migration, String styleRefId, Map<String, String> values, List<Field> definitionFields) {
    def style
    if (styleRefId.empty) {
        style = new TextStyleBuilder(Csv.deserialize(values.get("id"), String.class))
            .definition {
                for (field in definitionFields) {
                    it."$field.name"(Csv.deserialize(values.get(field.name), field.type))
                }
                it
            }
        def name = Csv.deserialize(values.get("name"), String.class)
        if (name != null && name != "") {
            style.name(name)
        }
    } else {
        style = new TextStyleBuilder(Csv.deserialize(values.get("id"), String.class))
            .styleRef(styleRefId)
        def name = Csv.deserialize(values.get("name"), String.class)
        if (name != null && name != "") {
            style.name(name)
        }
    }
    migration.textStyleRepository.upsert(style.build())
}