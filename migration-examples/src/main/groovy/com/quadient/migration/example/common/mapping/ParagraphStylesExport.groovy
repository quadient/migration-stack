//! ---
//! displayName: Export Paragraph Styles
//! category: Mapping
//! description: Creates a CSV report with paragraph style definitions from the migration project. Each row allows you to either modify the style’s properties directly or link the style to another style using the targetId column. The updated CSV can be edited and later imported to update style mappings in the project.
//! target: gradle
//! ---
package com.quadient.migration.example.common.mapping

import com.quadient.migration.api.Migration
import com.quadient.migration.api.dto.migrationmodel.ParagraphStyleDefinition
import com.quadient.migration.api.dto.migrationmodel.ParagraphStyleRef
import com.quadient.migration.example.common.util.Csv
import com.quadient.migration.example.common.util.Mapping

import java.nio.file.Path

import static com.quadient.migration.example.common.util.InitMigration.initMigration

def migration = initMigration(this.binding)

def exportFilePath = Mapping.csvPath(binding, migration.projectConfig.name, "paragraph-styles")
run(migration, exportFilePath)

static void run(Migration migration, Path exportFilePath) {
    def file = exportFilePath.toFile()
    file.createParentDirectories()

    def styles = migration.paragraphStyleRepository.listAll()
    file.withWriter { writer ->
        writer.writeLine("id,name,targetId,origin_locations,leftIndent,rightIndent,defaultTabSize,spaceBefore,spaceAfter,alignment,firstLineIndent,keepWithNextParagraph,lineSpacingType,lineSpacingValue")

        styles.each { style ->
            def definition = style.definition

            def builder = new StringBuilder()
            builder << "${Csv.serialize(style.id)},"
            builder << "${Csv.serialize(style.name)},"

            if (definition instanceof ParagraphStyleDefinition) {
                builder << "," // targetId
                builder << "${Csv.serialize(style.originLocations)},"
                builder << serializeDefinition(definition)
            } else if (definition instanceof ParagraphStyleRef){
                builder << "${Csv.serialize(definition.id)}," // targetId
                builder << "${Csv.serialize(style.originLocations)},"
                builder << serializeDefinition(null)
            }

            writer.writeLine(builder.toString())
        }
    }
}

static StringBuilder serializeDefinition(ParagraphStyleDefinition definition) {
    StringBuilder builder = new StringBuilder()

    builder << "${Csv.serialize(definition?.leftIndent)},"
    builder << "${Csv.serialize(definition?.rightIndent)},"
    builder << "${Csv.serialize(definition?.defaultTabSize)},"
    builder << "${Csv.serialize(definition?.spaceBefore)},"
    builder << "${Csv.serialize(definition?.spaceAfter)},"
    builder << "${Csv.serialize(definition?.alignment)},"
    builder << "${Csv.serialize(definition?.firstLineIndent)},"
    builder << "${Csv.serialize(definition?.keepWithNextParagraph)},"
    // Linespacing serializes as two columns
    builder << "${Csv.serialize(definition?.lineSpacing)}"

    return builder
}