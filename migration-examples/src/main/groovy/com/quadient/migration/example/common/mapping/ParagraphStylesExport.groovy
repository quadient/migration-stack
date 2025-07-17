package com.quadient.migration.example.common.mapping

import com.quadient.migration.api.dto.migrationmodel.ParagraphStyle
import com.quadient.migration.api.dto.migrationmodel.ParagraphStyleDefinition
import com.quadient.migration.api.dto.migrationmodel.ParagraphStyleRef
import com.quadient.migration.example.common.util.Csv

import java.nio.file.Path
import java.nio.file.Paths

import static com.quadient.migration.example.common.util.InitMigration.initMigration

def migration = initMigration(this.binding.variables["args"])

List<String> definitionOrder = ["leftIndent",
                                "rightIndent",
                                "defaultTabSize",
                                "spaceBefore",
                                "spaceAfter",
                                "alignment",
                                "firstLineIndent",
                                "keepWithNextParagraph"]


def styles = migration.paragraphStyleRepository.listAll()


exportTextStylesToCsv(styles, definitionOrder, Paths.get("mapping", "${migration.projectConfig.name}-paragraph-styles.csv"))

static void exportTextStylesToCsv(List<ParagraphStyle> styles, List<String> definitionOrder, Path exportFilePath) {
    def file = exportFilePath.toFile()
    file.createParentDirectories()
    file.withWriter { writer ->
        writer.writeLine("id,name,targetId,origin_locations,${definitionOrder.join(",")},lineSpacingType,lineSpacingValue")

        styles.each { style ->
            def definition = style.definition

            if (definition instanceof ParagraphStyleDefinition) {
                def definitionValues = (definitionOrder + ["lineSpacing"]).collect {
                    Csv.serialize(definition."${it}")
                }.join(",")
                writer.writeLine("${style.id},${style.name},,${Csv.serialize(style.originLocations)},${definitionValues}")
            } else if (definition instanceof ParagraphStyleRef) {
                def definitionValues = definitionOrder.collect { "" }.join(",")
                writer.writeLine("${style.id},${style.name},${definition.id},${Csv.serialize(style.originLocations)},${definitionValues}")
            }
        }
    }
}
