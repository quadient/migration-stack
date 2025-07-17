package com.quadient.migration.example.common.mapping

import com.quadient.migration.api.dto.migrationmodel.ParagraphStyleDefinition
import com.quadient.migration.api.dto.migrationmodel.ParagraphStyleRef
import com.quadient.migration.api.dto.migrationmodel.builder.ParagraphStyleBuilder
import com.quadient.migration.api.dto.migrationmodel.builder.ParagraphStyleDefinitionBuilder
import com.quadient.migration.example.common.util.Csv
import com.quadient.migration.shared.LineSpacing
import com.quadient.migration.shared.Size

import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.nio.file.Paths

import static com.quadient.migration.example.common.util.InitMigration.initMigration

def migration = initMigration(this.binding.variables["args"])

def file = Paths.get("mapping", "${migration.projectConfig.name}-paragraph-styles.csv").toFile().readLines()
def columnNames = file.removeFirst().split(",")

def definitionFields = ParagraphStyleDefinition.declaredFields.findAll { !it.synthetic && !Modifier.isStatic(it.getModifiers()) }
for (line in file) {
    def values = Csv.getCells(line, columnNames)

    def saved = migration.paragraphStyleRepository.find(values.get("id"))
    def styleRefId = values.get("targetId")

    if (styleRefId.empty) {
        if (saved == null) {
            def definition = new ParagraphStyleDefinitionBuilder().build()
            for (field in definitionFields) {
                updateDefinitionField(definition, field, values)
            }

            def style = new ParagraphStyleBuilder(values.get("id"))
                .name(values.get("name"))
                .definition(definition)
                .build()
            migration.paragraphStyleRepository.upsert(style)
        } else {
            if (saved.definition instanceof ParagraphStyleRef) {
                saved.definition = new ParagraphStyleDefinitionBuilder().build()
            }

            for (field in definitionFields) {
                updateDefinitionField(saved.definition as ParagraphStyleDefinition, field, values)
            }
            saved.name = values.get("name")
            migration.paragraphStyleRepository.upsert(saved)
        }
    } else {
        if (saved == null) {
            def style = new ParagraphStyleBuilder(values.get("id"))
                .name(values.get("name"))
                .styleRef(styleRefId)
                .build()
            migration.paragraphStyleRepository.upsert(style)
        } else {
            if (saved.definition instanceof ParagraphStyleDefinition) {
                saved.customFields.put("originalDefinition", saved.definition.toString())
            }

            saved.name = values.get("name")
            saved.definition = new ParagraphStyleRef(styleRefId)
            migration.paragraphStyleRepository.upsert(saved)
        }
    }
}

private static void updateDefinitionField(ParagraphStyleDefinition paraStyleDefinition, Field definitionField, Map<String, String> values) {
    if (definitionField.name == "lineSpacing") {
        def lineSpacingType = values.get("lineSpacingType")
        def lineSpacingValue = values.get("lineSpacingValue")

        def arg
        if (lineSpacingType == "MultipleOf") {
            arg = lineSpacingValue.toDouble()
        } else {
            arg = Size.fromString(lineSpacingValue)
        }

        def clazz = LineSpacing.class.getDeclaredClasses().find { it.simpleName == lineSpacingType }
        def lineSpacing = clazz.constructors[0].newInstance(arg) as LineSpacing
        paraStyleDefinition.lineSpacing = lineSpacing
    } else {
        paraStyleDefinition."${definitionField.name}" = Csv.deserialize(values.get(definitionField.name), definitionField.type)
    }
}
