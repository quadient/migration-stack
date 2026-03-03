//! ---
//! displayName: Import Text Styles
//! category: Mapping
//! description: Imports text style definitions from CSV files into the migration project, applying any updates made to the columns during editing.
//! target: gradle
//! ---
package com.quadient.migration.example.common.mapping

import com.quadient.migration.api.Migration
import com.quadient.migration.api.dto.migrationmodel.MappingItem
import com.quadient.migration.api.dto.migrationmodel.builder.TextStyleBuilder
import com.quadient.migration.api.dto.migrationmodel.builder.TextStyleDefinitionBuilder
import com.quadient.migration.example.common.util.Csv
import com.quadient.migration.example.common.util.Mapping
import com.quadient.migration.shared.Size
import com.quadient.migration.shared.SuperOrSubscript

import java.nio.file.Path

import static com.quadient.migration.example.common.util.InitMigration.initMigration

def migration = initMigration(this.binding)

def exportFilePath = Mapping.csvPath(binding, migration.projectConfig.name, "text-styles")

run(migration, exportFilePath)

static void run(Migration migration, Path path) {
    def file = path.toFile().readLines()
    def columnNames = Csv.parseColumnNames(file.removeFirst()).collect { Mapping.normalizeHeader(it) }

    for (line in file) {
        def values = Csv.getCells(line, columnNames)
        def id = values.get("id")

        def existingStyle = migration.textStyleRepository.find(id)
        if (existingStyle == null) {
            migration.textStyleRepository.upsert(
                new TextStyleBuilder(id)
                    .definition(new TextStyleDefinitionBuilder().build())
                    .build()
            )
        }

        def mapping = toMapping(values)
        migration.mappingRepository.upsert(id, mapping)
        migration.mappingRepository.applyTextStyleMapping(id)
    }
}

private static MappingItem.TextStyle toMapping(Map<String, String> values) {
    def name = Csv.deserialize(values.get("name"), String.class)
    def targetId = Csv.deserialize(values.get("targetId"), String.class)

    return new MappingItem.TextStyle(name, targetId, new MappingItem.TextStyle.Def(
        Csv.deserialize(values.get("fontFamily"), String.class),
        Csv.deserialize(values.get("foregroundColor"), com.quadient.migration.shared.Color.class),
        Csv.deserialize(values.get("size"), Size.class),
        Csv.deserialize(values.get("bold"), Boolean.class),
        Csv.deserialize(values.get("italic"), Boolean.class),
        Csv.deserialize(values.get("underline"), Boolean.class),
        Csv.deserialize(values.get("strikethrough"), Boolean.class),
        Csv.deserialize(values.get("superOrSubscript"), SuperOrSubscript.class),
        Csv.deserialize(values.get("interspacing"), Size.class),
    ))
}

