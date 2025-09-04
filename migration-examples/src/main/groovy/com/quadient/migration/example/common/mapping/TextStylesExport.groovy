//! ---
//! category: Mapping
//! description: Export text styles
//! target: gradle
//! ---
package com.quadient.migration.example.common.mapping

import com.quadient.migration.api.Migration
import com.quadient.migration.api.dto.migrationmodel.MappingItem
import com.quadient.migration.api.dto.migrationmodel.TextStyleDefinition
import com.quadient.migration.example.common.util.Csv
import com.quadient.migration.example.common.util.Mapping
import com.quadient.migration.shared.Size
import groovy.transform.Field

import java.nio.file.Path

import static com.quadient.migration.example.common.util.InitMigration.initMigration

def migration = initMigration(this.binding)

@Field
static List<String> definitionOrder = ["fontFamily",
                                       "foregroundColor",
                                       "size",
                                       "bold",
                                       "italic",
                                       "underline",
                                       "strikethrough",
                                       "superOrSubscript",
                                       "interspacing"]

def exportFilePath = Mapping.csvPath(binding, migration.projectConfig.name, "text-styles")
run(migration, exportFilePath)

static void run(Migration migration, Path dstPath) {
    def styles = migration.textStyleRepository.listAll()

    def file = dstPath.toFile()
    file.createParentDirectories()
    file.withWriter { writer ->
        writer.writeLine("id,name,targetId,origin_locations,${definitionOrder.join(",")}")

        styles.each { style ->
            def definition = style.definition
            def mapping = migration.mappingRepository.getTextStyleMapping(style.id)
            def mappingDefinition = mapping?.definition

            def builder = new StringBuilder()
            builder << "${Csv.serialize(style.id)},"
            builder << "${Csv.serialize(mapping?.name ?: style.name)},"

            if (definition instanceof TextStyleDefinition) {
                def definitionValues
                if (mappingDefinition == null) {
                    builder << "," // targetId
                    definitionValues = definitionOrder.collect { Csv.serialize(definition?."$it", getUnit(it)) }
                } else if (mappingDefinition instanceof MappingItem.TextStyle.Def) {
                    builder << "," // targetId
                    definitionValues = definitionOrder.collect { Csv.serialize(mappingDefinition."$it" ?: definition?."$it", getUnit(it)) }
                } else if (mappingDefinition instanceof MappingItem.TextStyle.Ref) {
                    builder << "${Csv.serialize(mappingDefinition.targetId)}," // targetId
                    definitionValues = definitionOrder.collect { "" }
                }
                builder << "${Csv.serialize(style.originLocations)},"
                builder << definitionValues.join(",")
            } else {
                def definitionValues
                if (mappingDefinition == null) {
                    builder << "${Csv.serialize(definition.id)}," // targetId
                    definitionValues = definitionOrder.collect { "" }
                } else if (mappingDefinition instanceof MappingItem.TextStyle.Def) {
                    builder << "," // targetId
                    definitionValues = definitionOrder.collect { Csv.serialize(mappingDefinition."$it", getUnit(it)) }
                } else if (mappingDefinition instanceof MappingItem.TextStyle.Ref) {
                    builder << "${Csv.serialize(mappingDefinition.targetId)}," // targetId
                    definitionValues = definitionOrder.collect { "" }
                }
                builder << "${Csv.serialize(style.originLocations)},"
                builder << definitionValues.join(",")
            }

            writer.writeLine(builder.toString())
        }
    }
}

static Size.Unit getUnit(String name) {
    if (name == "size") {
        return Size.Unit.Points
    } else {
        return Size.Unit.Millimeters
    }
}

