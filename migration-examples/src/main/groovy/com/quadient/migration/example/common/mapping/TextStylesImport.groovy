package com.quadient.migration.example.common.mapping

import com.quadient.migration.api.dto.migrationmodel.TextStyleDefinition
import com.quadient.migration.api.dto.migrationmodel.TextStyleRef
import com.quadient.migration.api.dto.migrationmodel.builder.TextStyleBuilder
import com.quadient.migration.api.dto.migrationmodel.builder.TextStyleDefinitionBuilder
import com.quadient.migration.example.common.util.Csv

import java.lang.reflect.Modifier
import java.nio.file.Paths

import static com.quadient.migration.example.common.util.InitMigration.initMigration

def migration = initMigration(this.binding.variables["args"])

def file = Paths.get("mapping", "${migration.projectConfig.name}-text-styles.csv").toFile().readLines()
def columnNames = file.removeFirst().split(",")

def definitionFields = TextStyleDefinition.declaredFields.findAll { !it.synthetic && !Modifier.isStatic(it.getModifiers()) }
for (line in file) {
    def values = Csv.getCells(line, columnNames)

    def saved = migration.textStyleRepository.find(values.get("id"))
    def styleRefId = values.get("targetId")

    if (styleRefId.empty) {
        if (saved == null) {
            def style = new TextStyleBuilder(Csv.deserialize(values.get("id"), String.class))
                    .name(Csv.deserialize(values.get("name"), String.class))
                    .definition {
                        for (field in definitionFields) {
                            it."$field.name"(Csv.deserialize(values.get(field.name), field.type))
                        }
                        it
                    }.build()
            migration.textStyleRepository.upsert(style)
        } else {
            if (saved.definition instanceof TextStyleRef) {
                saved.definition = new TextStyleDefinitionBuilder().build()
            }

            for (field in definitionFields) {
                saved.definition."$field.name" = Csv.deserialize(values.get(field.name), field.type)
            }
            saved.name = Csv.deserialize(values.get("name"), String.class)
            migration.textStyleRepository.upsert(saved)
        }
    } else {
        if (saved == null) {
            def style = new TextStyleBuilder(Csv.deserialize(values.get("id"), String.class))
                    .name(Csv.deserialize(values.get("name"), String.class))
                    .styleRef(styleRefId)
                    .build()
            migration.textStyleRepository.upsert(style)
        } else {
            if (saved.definition instanceof TextStyleDefinition) {
                saved.customFields.put("originalDefinition", saved.definition.toString())
            }

            saved.name = Csv.deserialize(values.get("name"), String.class)
            saved.definition = new TextStyleRef(styleRefId)
            migration.textStyleRepository.upsert(saved)
        }
    }
}