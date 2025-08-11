package com.quadient.migration.example.common.mapping

import com.quadient.migration.api.Migration
import com.quadient.migration.api.dto.migrationmodel.MappingItem
import com.quadient.migration.api.dto.migrationmodel.ParagraphStyleDefinition
import com.quadient.migration.example.common.util.Csv

import java.nio.file.Path
import java.nio.file.Paths

import static com.quadient.migration.example.common.util.InitMigration.initMigration

def migration = initMigration(this.binding.variables["args"])

def exportFilePath = Paths.get("mapping", "${migration.projectConfig.name}-paragraph-styles.csv")
run(migration, exportFilePath)

static void run(Migration migration, Path exportFilePath) {
    def file = exportFilePath.toFile()
    file.createParentDirectories()

    def styles = migration.paragraphStyleRepository.listAll()
    file.withWriter { writer ->
        writer.writeLine("id,name,targetId,origin_locations,leftIndent,rightIndent,defaultTabSize,spaceBefore,spaceAfter,alignment,firstLineIndent,keepWithNextParagraph,lineSpacingType,lineSpacingValue")

        styles.each { style ->
            def definition = style.definition
            def mapping = migration.mappingRepository.getParagraphStyleMapping(style.id)
            def mappingDefinition = mapping?.definition

            def builder = new StringBuilder()
            builder << "${Csv.serialize(style.id)},"
            builder << "${Csv.serialize(mapping?.name ?: style.name)},"

            if (definition instanceof ParagraphStyleDefinition) {
                def definitionValues
                if (mappingDefinition == null) {
                    builder << "," // targetId
                    builder << "${Csv.serialize(style.originLocations)},"
                    builder << serializeDefinition(definition, null)
                } else if (mappingDefinition instanceof MappingItem.ParagraphStyle.Def) {
                    builder << "," // targetId
                    builder << "${Csv.serialize(style.originLocations)},"
                    builder << serializeDefinition(definition, mappingDefinition)
                } else if (mappingDefinition instanceof MappingItem.ParagraphStyle.Ref) {
                    builder << "${Csv.serialize(mappingDefinition.targetId)}," // targetId
                    builder << "${Csv.serialize(style.originLocations)},"
                    builder << serializeDefinition(null, null)
                }
            } else {
                def definitionValues
                if (mappingDefinition == null) {
                    builder << "${Csv.serialize(definition.id)}," // targetId
                    builder << "${Csv.serialize(style.originLocations)},"
                    builder << serializeDefinition(null, null)
                } else if (mappingDefinition instanceof MappingItem.ParagraphStyle.Def) {
                    builder << "," // targetId
                    builder << "${Csv.serialize(style.originLocations)},"
                    builder << serializeDefinition(null, mappingDefinition)
                } else if (mappingDefinition instanceof MappingItem.ParagraphStyle.Ref) {
                    builder << "${Csv.serialize(mappingDefinition.targetId)}," // targetId
                    builder << "${Csv.serialize(style.originLocations)},"
                    builder << serializeDefinition(null, null)
                }
            }

            writer.writeLine(builder.toString())
        }
    }
}

static StringBuilder serializeDefinition(ParagraphStyleDefinition definition, MappingItem.ParagraphStyle.Def mapping) {
    StringBuilder builder = new StringBuilder()

    builder << "${Csv.serialize(mapping?.leftIndent ?: definition?.leftIndent)},"
    builder << "${Csv.serialize(mapping?.rightIndent ?: definition?.rightIndent)},"
    builder << "${Csv.serialize(mapping?.defaultTabSize ?: definition?.defaultTabSize)},"
    builder << "${Csv.serialize(mapping?.spaceBefore ?: definition?.spaceBefore)},"
    builder << "${Csv.serialize(mapping?.spaceAfter ?: definition?.spaceAfter)},"
    builder << "${Csv.serialize(mapping?.alignment ?: definition?.alignment)},"
    builder << "${Csv.serialize(mapping?.firstLineIndent ?: definition?.firstLineIndent)},"
    builder << "${Csv.serialize(mapping?.keepWithNextParagraph ?: definition?.keepWithNextParagraph)},"
    // Linespacing serializes as two columns
    builder << "${Csv.serialize(mapping?.lineSpacing ?: definition?.lineSpacing)}"

    return builder
}