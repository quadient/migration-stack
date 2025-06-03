package com.quadient.migration.example.common.mapping

import com.quadient.migration.api.dto.migrationmodel.TextStyle
import com.quadient.migration.api.dto.migrationmodel.TextStyleDefinition
import com.quadient.migration.api.dto.migrationmodel.TextStyleRef
import com.quadient.migration.example.common.util.Csv
import com.quadient.migration.shared.Size

import java.nio.file.Path
import java.nio.file.Paths

import static com.quadient.migration.example.common.util.InitMigration.initMigration

def migration = initMigration(this.binding.variables["args"])

List<String> definitionOrder = ["fontFamily",
                                "foregroundColor",
                                "size",
                                "bold",
                                "italic",
                                "underline",
                                "strikethrough",
                                "superOrSubscript",
                                "interspacing"]

def styles = migration.textStyleRepository.listAll()

exportTextStylesToCsv(styles, definitionOrder, Paths.get("mapping/${migration.projectConfig.name}-text-styles.csv"))

static void exportTextStylesToCsv(List<TextStyle> styles, List<String> definitionOrder, Path exportFilePath) {
    def file = exportFilePath.toFile()
    file.createParentDirectories()
    file.withWriter { writer ->
        writer.writeLine("id,name,targetId,${definitionOrder.join(",")}")

        styles.each { style ->
            def definition = style.definition

            if (definition instanceof TextStyleDefinition) {
                def definitionValues = definitionOrder.collect {
                    if (it == "size") {
                        Csv.serialize(definition."${it}", Size.Unit.Points)
                    } else {
                        Csv.serialize(definition."${it}")
                    }
                }.join(",")
                writer.writeLine("${style.id},${style.name},,${definitionValues}")
            } else if (definition instanceof TextStyleRef) {
                def definitionValues = definitionOrder.collect { "" }.join(",")
                writer.writeLine("${style.id},${style.name},${definition.id},${definitionValues}")
            }
        }
    }
}